package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.finops.api.dto.*;
import com.amg.digitalitzacio.finops.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinOpsOrchestrator implements FinOpsService {

    private final FinOpsClient finOpsClient;
    private final HoldedConfigRepository holdedConfigRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

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
}
