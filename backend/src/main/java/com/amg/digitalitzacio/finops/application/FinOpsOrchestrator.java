package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.finops.api.dto.*;
import com.amg.digitalitzacio.finops.domain.*;
import com.amg.digitalitzacio.shared.exception.ConflictException;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FinOpsOrchestrator implements FinOpsService {

    private final FinOpsClient finOpsClient;
    private final HoldedConfigRepository holdedConfigRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;
    private final SepaMandateRepository sepaMandateRepository;
    private final MonthlyInvoiceRepository monthlyInvoiceRepository;
    private final BillingCalculator billingCalculator;
    private final SepaXmlGenerator sepaXmlGenerator;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public HoldedConfigResponse configure(HoldedConfigRequest request) {
        var existing = holdedConfigRepository.findByTenantId(request.tenantId());
        var config = existing.orElseGet(() -> HoldedConfig.builder()
                .tenantId(request.tenantId())
                .build());
        config.setApiKeyRef(request.apiKeyRef());
        config.setHoldedCompanyId(request.holdedCompanyId());
        config.setIsActive(true);
        config = holdedConfigRepository.save(config);
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public HoldedConfigResponse getConfig(UUID tenantId) {
        var config = findConfig(tenantId);
        return toConfigResponse(config);
    }

    @Override
    @Transactional
    public HoldedConfigResponse syncContact(UUID tenantId) {
        var config = findConfig(tenantId);
        var contactId = finOpsClient.createContact("tenant-" + tenantId, null, null, null);
        config.setHoldedContactId(contactId);
        config.setIsSynced(true);
        config.setLastSyncAt(Instant.now());
        config = holdedConfigRepository.save(config);
        return toConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> listInvoices(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        Page<Invoice> result;
        if (status != null && !status.isBlank()) {
            result = invoiceRepository.findByTenantIdAndStatus(tenantId, InvoiceStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            result = invoiceRepository.findByTenantId(tenantId, pageable);
        }
        return result.map(this::toInvoiceResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId, UUID currentTenantId) {
        var invoice = currentTenantId != null
                ? invoiceRepository.findByIdAndTenantId(invoiceId, currentTenantId)
                : invoiceRepository.findById(invoiceId);
        return toInvoiceResponse(invoice.orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId)));
    }

    @Override
    @Transactional(readOnly = true)
    public String getInvoicePdfUrl(UUID invoiceId, UUID currentTenantId) {
        var inv = currentTenantId != null
                ? invoiceRepository.findByIdAndTenantId(invoiceId, currentTenantId)
                : invoiceRepository.findById(invoiceId);
        inv.orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        var invoice = inv.get();
        if (invoice.getInvoicePdfUrl() != null) {
            return invoice.getInvoicePdfUrl();
        }
        return finOpsClient.getInvoicePdfUrl(invoice.getHoldedInvoiceId());
    }

    @Override
    @Transactional
    public InvoiceResponse cancelInvoice(UUID invoiceId) {
        var invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setIsActive(false);
        if (invoice.getHoldedInvoiceId() != null) {
            finOpsClient.cancelInvoice(invoice.getHoldedInvoiceId());
        }
        invoice = invoiceRepository.save(invoice);
        return toInvoiceResponse(invoice);
    }

    @Override
    @Transactional
    public InvoiceResponse createInvoiceFromBudget(UUID budgetId) {
        var budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + budgetId));

        var config = holdedConfigRepository.findByTenantId(budget.getTenantId())
                .orElseThrow(() -> new IllegalStateException("Holded not configured for tenant: " + budget.getTenantId()));

        if (config.getHoldedContactId() == null || !config.getIsSynced()) {
            syncContact(budget.getTenantId());
            config = holdedConfigRepository.findByTenantId(budget.getTenantId()).orElseThrow();
        }

        var invoice = Invoice.builder()
                .tenantId(budget.getTenantId())
                .budgetId(budgetId)
                .amount(budget.getTotal())
                .taxAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING)
                .build();

        try {
            var holdedId = finOpsClient.createInvoice(
                    config.getHoldedContactId(),
                    invoice.getAmount(),
                    invoice.getTaxAmount(),
                    "Budget " + budgetId,
                    null);
            invoice.setHoldedInvoiceId(holdedId);
            invoice.setInvoiceNumber("F-" + Instant.now().toString().substring(0, 10) + "-" + budgetId.toString().substring(0, 4));
            invoice.setStatus(InvoiceStatus.SENT);
            invoice.setVerifactuStatus(VerifactuStatus.SENT);
            if (finOpsClient.isConnected()) {
                invoice.setInvoicePdfUrl(finOpsClient.getInvoicePdfUrl(holdedId));
            }
        } catch (Exception e) {
            invoice.setStatus(InvoiceStatus.PENDING);
            invoice.setErrorMessage(e.getMessage());
            invoice.setVerifactuStatus(VerifactuStatus.FAILED);
        }

        invoice = invoiceRepository.save(invoice);
        return toInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public FinOpsDashboardResponse getDashboard(UUID tenantId) {
        long totalInvoices = invoiceRepository.count();
        long paidCount = invoiceRepository.countByTenantIdAndStatus(tenantId, InvoiceStatus.PAID);
        long pendingCount = invoiceRepository.countByTenantIdAndStatus(tenantId, InvoiceStatus.PENDING);
        long overdueCount = invoiceRepository.countByTenantIdAndStatus(tenantId, InvoiceStatus.OVERDUE);
        long sentCount = invoiceRepository.countByTenantIdAndStatus(tenantId, InvoiceStatus.SENT);

        return new FinOpsDashboardResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                (int) totalInvoices, (int) paidCount, (int) (pendingCount + sentCount), (int) overdueCount);
    }

    @Override
    @Transactional(readOnly = true)
    public FinOpsDashboardGlobalResponse getGlobalDashboard() {
        long totalInvoices = invoiceRepository.count();
        long configuredTenants = holdedConfigRepository.count();

        return new FinOpsDashboardGlobalResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                (int) totalInvoices, 0, (int) configuredTenants);
    }

    @Override
    @Transactional
    public WebhookResponse processWebhook(WebhookRequest request) {
        if (request.holdedInvoiceId() != null && request.event() != null) {
            var invoices = invoiceRepository.findByHoldedInvoiceId(request.holdedInvoiceId());
            invoices.ifPresent(inv -> {
                switch (request.event().toLowerCase()) {
                    case "invoice.paid" -> {
                        inv.setStatus(InvoiceStatus.PAID);
                        inv.setPaidAt(Instant.now());
                    }
                    case "invoice.cancelled" -> {
                        inv.setStatus(InvoiceStatus.CANCELLED);
                        inv.setIsActive(false);
                    }
                    case "invoice.overdue" -> inv.setStatus(InvoiceStatus.OVERDUE);
                }
                invoiceRepository.save(inv);
            });
        }
        return new WebhookResponse(true);
    }

    private HoldedConfig findConfig(UUID tenantId) {
        return holdedConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Holded not configured for tenant: " + tenantId));
    }

    private HoldedConfigResponse toConfigResponse(HoldedConfig config) {
        return new HoldedConfigResponse(
                config.getId(), config.getTenantId(), config.getHoldedCompanyId(),
                config.getHoldedContactId(), Boolean.TRUE.equals(config.getIsSynced()),
                config.getLastSyncAt(), Boolean.TRUE.equals(config.getIsActive()),
                config.getCreatedAt());
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv) {
        return new InvoiceResponse(
                inv.getId(), inv.getTenantId(), inv.getBudgetId(),
                inv.getHoldedInvoiceId(), inv.getInvoiceNumber(),
                inv.getStatus().name(), inv.getAmount(), inv.getTaxAmount(),
                inv.getCurrency(), inv.getVerifactuStatus().name(),
                inv.getInvoicePdfUrl(), inv.getDueDate(), inv.getPaidAt(),
                inv.getErrorMessage(), inv.getCreatedAt());
    }

    // --- SEPA ---

    @Override
    @Transactional
    public SepaMandateResponse registerSepaMandate(UUID tenantId, SepaMandateRequest request) {
        // tenant_id és unique — només es permet un mandat per tenant
        if (sepaMandateRepository.findByTenantIdAndIsActiveTrue(tenantId).isPresent()) {
            throw new ConflictException("Tenant already has an active SEPA mandate");
        }

        var existingInactive = sepaMandateRepository.findByTenantId(tenantId);
        if (existingInactive.isPresent()) {
            throw new ConflictException("SEPA mandate already exists for this tenant (revoked)");
        }

        long count = sepaMandateRepository.count();
        var mandate = SepaMandate.builder()
                .tenantId(tenantId)
                .mandateId("AMG-" + java.time.Year.now().getValue() + "-" + String.format("%04d", count + 1))
                .iban(request.iban())
                .bic(request.bic())
                .accountHolderName(request.accountHolderName())
                .signedAt(request.signedAt())
                .isActive(true)
                .build();
        mandate = sepaMandateRepository.save(mandate);
        return toMandateResponse(mandate);
    }

    @Override
    @Transactional(readOnly = true)
    public SepaMandateResponse getSepaMandate(UUID tenantId) {
        var mandate = sepaMandateRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active SEPA mandate for tenant: " + tenantId));
        return toMandateResponse(mandate);
    }

    @Override
    @Transactional
    public void revokeSepaMandate(UUID tenantId) {
        var mandate = sepaMandateRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("No active SEPA mandate for tenant: " + tenantId));
        mandate.setIsActive(false);
        mandate.setRevokedAt(Instant.now());
        sepaMandateRepository.save(mandate);
    }

    // --- Monthly invoices ---

    @Override
    @Transactional
    public List<MonthlyInvoiceResponse> generateMonthlyInvoices(String period) {
        // Obtenir tots els tenants que tenen configuració Holded
        var configs = holdedConfigRepository.findAll();
        var results = new ArrayList<MonthlyInvoiceResponse>();

        for (var config : configs) {
            var tenantId = config.getTenantId();
            if (monthlyInvoiceRepository.findByTenantIdAndPeriod(tenantId, period).isPresent()) continue;

            var tenant = tenantRepository.findById(tenantId).orElse(null);
            var billingStartDate = tenant != null ? tenant.getBillingStartDate() : null;

            var amount = billingCalculator.calculateMonthlyAmount(tenantId, period, billingStartDate);
            if (amount.compareTo(BigDecimal.ZERO) == 0) continue;

            var holdedInvoiceId = finOpsClient.createInvoice(
                    config.getHoldedContactId() != null ? config.getHoldedContactId() : "UNKNOWN",
                    amount,
                    amount.multiply(new BigDecimal("0.21")),
                    "Quota mensual serveis AMG – " + period,
                    null);

            var sepaMandate = sepaMandateRepository.findByTenantIdAndIsActiveTrue(tenantId);
            LocalDate collectionDate = sepaMandate.map(m -> LocalDate.now().withDayOfMonth(5)).orElse(null);

            var invoice = MonthlyInvoice.builder()
                    .tenantId(tenantId)
                    .period(period)
                    .holdedInvoiceId(holdedInvoiceId)
                    .invoiceNumber("F-MENS-" + period + "-" + String.format("%03d", results.size() + 1))
                    .amount(amount)
                    .status(InvoiceStatus.SENT)
                    .sepaCollectionDate(collectionDate)
                    .sepaCollected(false)
                    .tenantName(tenant != null ? tenant.getName() : null)
                    .tenantNif(tenant != null ? tenant.getNif() : null)
                    .tenantAddress(tenant != null ? tenant.getAddress() : null)
                    .tenantEmail(tenant != null ? tenant.getEmail() : null)
                    .build();
            invoice = monthlyInvoiceRepository.save(invoice);
            results.add(toMonthlyInvoiceResponse(invoice));
        }
        return results;
    }

    @Override
    @Transactional
    public MonthlyInvoiceResponse generateMonthlyInvoiceForTenant(UUID tenantId, String period) {
        // Si ja existeix la factura mensual per aquest tenant i període, retorna-la
        var existing = monthlyInvoiceRepository.findByTenantIdAndPeriod(tenantId, period);
        if (existing.isPresent()) {
            return toMonthlyInvoiceResponse(existing.get());
        }

        var config = holdedConfigRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Holded no configurat per al tenant: " + tenantId));

        var tenant = tenantRepository.findById(tenantId).orElse(null);
        var billingStartDate = tenant != null ? tenant.getBillingStartDate() : null;

        var amount = billingCalculator.calculateMonthlyAmount(tenantId, period, billingStartDate);
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException("L'import calculat per al tenant és zero — no es genera factura");
        }

        var totalExisting = monthlyInvoiceRepository.countByTenantId(tenantId);

        var holdedInvoiceId = finOpsClient.createInvoice(
                config.getHoldedContactId() != null ? config.getHoldedContactId() : "UNKNOWN",
                amount,
                amount.multiply(new BigDecimal("0.21")),
                "Quota mensual serveis AMG – " + period,
                null);

        var sepaMandate = sepaMandateRepository.findByTenantIdAndIsActiveTrue(tenantId);
        LocalDate collectionDate = sepaMandate.map(m -> LocalDate.now().withDayOfMonth(5)).orElse(null);

        var invoice = MonthlyInvoice.builder()
                .tenantId(tenantId)
                .period(period)
                .holdedInvoiceId(holdedInvoiceId)
                .invoiceNumber("F-MENS-" + period + "-" + String.format("%03d", totalExisting + 1))
                .amount(amount)
                .status(InvoiceStatus.SENT)
                .sepaCollectionDate(collectionDate)
                .sepaCollected(false)
                .tenantName(tenant != null ? tenant.getName() : null)
                .tenantNif(tenant != null ? tenant.getNif() : null)
                .tenantAddress(tenant != null ? tenant.getAddress() : null)
                .tenantEmail(tenant != null ? tenant.getEmail() : null)
                .build();
        invoice = monthlyInvoiceRepository.save(invoice);
        return toMonthlyInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public MonthlyInvoiceResponse getMonthlyInvoice(UUID id) {
        var invoice = monthlyInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monthly invoice not found: " + id));
        return toMonthlyInvoiceResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MonthlyInvoiceResponse> listMonthlyInvoices(UUID tenantId, String period, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (tenantId != null) return monthlyInvoiceRepository.findByTenantId(tenantId, pageable).map(this::toMonthlyInvoiceResponse);
        if (period != null) return monthlyInvoiceRepository.findByPeriod(period, pageable).map(this::toMonthlyInvoiceResponse);
        return monthlyInvoiceRepository.findAll(pageable).map(this::toMonthlyInvoiceResponse);
    }

    @Override
    @Transactional
    public byte[] exportSepaXml(String period) {
        var mandates = sepaMandateRepository.findAllByIsActiveTrue();
        var pendingInvoices = monthlyInvoiceRepository.findByPeriodAndSepaCollectedFalse(period);

        var mandateMap = mandates.stream().collect(Collectors.toMap(SepaMandate::getTenantId, m -> m));

        var entries = new ArrayList<SepaXmlGenerator.SepaPaymentEntry>();
        for (var inv : pendingInvoices) {
            var mandate = mandateMap.get(inv.getTenantId());
            if (mandate == null) continue;
            entries.add(new SepaXmlGenerator.SepaPaymentEntry(
                    mandate.getMandateId(), mandate.getSignedAt(),
                    mandate.getIban(), mandate.getAccountHolderName(),
                    inv.getAmount(), "Quota mensual AMG – " + period));
        }

        return sepaXmlGenerator.generate("ES0000000000000000000000", "AMG Digitalitzacio SL",
                "ES12ZZZ12345678", entries);
    }

    @Override
    @Transactional
    public void markSepaCollected(String period) {
        var invoices = monthlyInvoiceRepository.findByPeriodAndSepaCollectedFalse(period);
        var mandates = sepaMandateRepository.findAllByIsActiveTrue();
        var tenantIdsWithMandate = mandates.stream().map(SepaMandate::getTenantId).collect(Collectors.toSet());

        for (var inv : invoices) {
            if (!tenantIdsWithMandate.contains(inv.getTenantId())) continue;
            inv.setSepaCollected(true);
            inv.setStatus(InvoiceStatus.PAID);
            monthlyInvoiceRepository.save(inv);
        }
    }

    private SepaMandateResponse toMandateResponse(SepaMandate m) {
        return new SepaMandateResponse(m.getId(), m.getTenantId(), m.getMandateId(),
                m.getIban(), m.getAccountHolderName(), m.getSignedAt(), m.getIsActive(), m.getCreatedAt());
    }

    private MonthlyInvoiceResponse toMonthlyInvoiceResponse(MonthlyInvoice inv) {
        return new MonthlyInvoiceResponse(inv.getId(), inv.getTenantId(), inv.getPeriod(),
                inv.getInvoiceNumber(), inv.getAmount(), inv.getStatus().name(),
                inv.getSepaCollectionDate(), inv.getSepaCollected(), inv.getInvoicePdfUrl(), inv.getCreatedAt(),
                inv.getTenantName(), inv.getTenantNif(), inv.getTenantAddress(), inv.getTenantEmail());
    }
}
