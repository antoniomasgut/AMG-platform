package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.vault.application.ProfileService;
import com.amg.digitalitzacio.vault.application.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillingOrchestrator implements BillingService {

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final DiscountRepository discountRepository;
    private final ProfileService profileService;
    private final VaultService vaultService;

    // ── Crear pressupost ──

    @Override
    @Transactional
    public BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request, UUID createdBy) {
        var prefix = "BUD-" + Year.now().getValue() + "-";
        var count = budgetRepository.countByBudgetNumberStartingWith(prefix);
        var budgetNumber = prefix + String.format("%04d", count + 1);

        var budget = Budget.builder()
                .tenantId(tenantId)
                .budgetNumber(budgetNumber)
                .profileId(request.getProfileId())
                .version(1)
                .status(BudgetStatus.DRAFT)
                .subtotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .validUntil(request.getValidUntilDays() != null
                        ? LocalDate.now().plusDays(request.getValidUntilDays())
                        : LocalDate.now().plusDays(30))
                .notes(request.getNotes())
                .acceptanceToken(UUID.randomUUID())
                .build();
        budget = budgetRepository.save(budget);

        // Crear línies de pressupost des del Vault
        var budgetLines = new ArrayList<BudgetLine>();
        var sortOrder = 0;

        if (request.getProfileId() != null) {
            var budgetCalc = profileService.calculateBudget(
                    request.getProfileId(), request.getAddonIds(), true);

            if (budgetCalc.getPhases() != null) {
                for (var phaseBudget : budgetCalc.getPhases()) {
                    if (phaseBudget.getServices() != null) {
                        for (var svc : phaseBudget.getServices()) {
                            budgetLines.add(BudgetLine.builder()
                                    .budgetId(budget.getId())
                                    .phaseId(phaseBudget.getPhase().getId())
                                    .serviceId(svc.getId())
                                    .serviceName(svc.getName())
                                    .quantity(1)
                                    .unitPrice(svc.getSalePrice())
                                    .total(svc.getSalePrice())
                                    .sortOrder(sortOrder++)
                                    .build());
                        }
                    }
                }
            }

            // Addons
            if (budgetCalc.getAddons() != null) {
                for (var addon : budgetCalc.getAddons()) {
                    budgetLines.add(BudgetLine.builder()
                            .budgetId(budget.getId())
                            .phaseId(null)
                            .serviceId(addon.getId())
                            .serviceName(addon.getName())
                            .quantity(1)
                            .unitPrice(addon.getSalePrice())
                            .total(addon.getSalePrice())
                            .sortOrder(sortOrder++)
                            .build());
                }
            }
        }

        budgetLines = new ArrayList<>(budgetLineRepository.saveAll(budgetLines));

        // Calcular subtotal
        var subtotal = budgetLines.stream()
                .map(BudgetLine::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Aplicar descomptes actius
        var now = LocalDate.now();
        var activeDiscounts = discountRepository.findByTenantIdAndIsActiveTrue(tenantId).stream()
                .filter(d -> d.getValidFrom() == null || !d.getValidFrom().isAfter(now))
                .filter(d -> d.getValidUntil() == null || !d.getValidUntil().isBefore(now))
                .filter(d -> d.getMaxApplications() == null || d.getAppliedCount() < d.getMaxApplications())
                .toList();

        var discountTotal = BigDecimal.ZERO;
        for (var discount : activeDiscounts) {
            var applicableAmount = BigDecimal.ZERO;

            if (discount.getAppliesTo() == AppliesTo.BUDGET && discount.getReferenceId() == null) {
                applicableAmount = subtotal;
            } else if (discount.getAppliesTo() == AppliesTo.PHASE && discount.getReferenceId() != null) {
                applicableAmount = budgetLines.stream()
                        .filter(bl -> discount.getReferenceId().equals(bl.getPhaseId()))
                        .map(BudgetLine::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            } else if (discount.getAppliesTo() == AppliesTo.SERVICE && discount.getReferenceId() != null) {
                applicableAmount = budgetLines.stream()
                        .filter(bl -> discount.getReferenceId().equals(bl.getServiceId()))
                        .map(BudgetLine::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (applicableAmount.compareTo(BigDecimal.ZERO) > 0) {
                var discountAmount = discount.getType() == com.amg.digitalitzacio.billing.domain.DiscountType.PERCENTAGE
                        ? applicableAmount.multiply(discount.getValue()).divide(BigDecimal.valueOf(100))
                        : discount.getValue().min(applicableAmount);
                discountTotal = discountTotal.add(discountAmount);

                discount.setAppliedCount(discount.getAppliedCount() + 1);
                discountRepository.save(discount);
            }
        }

        var total = subtotal.subtract(discountTotal);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        budget.setSubtotal(subtotal);
        budget.setDiscountTotal(discountTotal);
        budget.setTotal(total);
        budget = budgetRepository.save(budget);

        return toBudgetResponse(budget, budgetLines);
    }

    // ── Llistar pressupostos ──

    @Override
    public BudgetListResponse listBudgets(UUID tenantId) {
        return listBudgetsByTenant(tenantId);
    }

    @Override
    public BudgetListResponse listBudgetsByTenant(UUID tenantId) {
        var budgets = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        var summaries = budgets.stream().map(b -> BudgetResponse.BudgetSummary.builder()
                .id(b.getId())
                .budgetNumber(b.getBudgetNumber())
                .status(b.getStatus())
                .total(b.getTotal())
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .build()).toList();
        return BudgetListResponse.builder()
                .budgets(summaries)
                .totalCount(summaries.size())
                .build();
    }

    // ── Veure pressupost ──

    @Override
    public BudgetResponse getBudget(UUID tenantId, UUID id) {
        var budget = budgetRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("Pressupost no trobat"));
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        return toBudgetResponse(budget, lines);
    }

    // ── Actualitzar pressupost ──

    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID id, UpdateBudgetRequest request) {
        var budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pressupost no trobat"));
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new IllegalArgumentException("Només es poden modificar pressupostos en esborrany");
        }
        if (request.getNotes() != null) budget.setNotes(request.getNotes());
        if (request.getClientNotes() != null) budget.setClientNotes(request.getClientNotes());
        budget = budgetRepository.save(budget);
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        return toBudgetResponse(budget, lines);
    }

    // ── Cancel·lar pressupost ──

    @Override
    @Transactional
    public void cancelBudget(UUID id) {
        var budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pressupost no trobat"));
        if (budget.getStatus() == BudgetStatus.ACCEPTED || budget.getStatus() == BudgetStatus.CANCELLED) {
            throw new IllegalArgumentException("No es pot cancel·lar un pressupost " + budget.getStatus());
        }
        budget.setStatus(BudgetStatus.CANCELLED);
        budgetRepository.save(budget);
    }

    // ── Enviar pressupost ──

    @Override
    @Transactional
    public BudgetResponse sendBudget(UUID id) {
        var budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pressupost no trobat"));
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new IllegalArgumentException("Només es poden enviar pressupostos en esborrany");
        }
        budget.setStatus(BudgetStatus.SENT);
        budget.setSentAt(java.time.Instant.now());
        budget = budgetRepository.save(budget);
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        return toBudgetResponse(budget, lines);
    }

    // ── Acceptar pressupost (token públic) ──

    @Override
    @Transactional
    public BudgetResponse acceptBudget(String token) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token invàlid");
        }

        var budget = budgetRepository.findByAcceptanceToken(tokenUuid)
                .orElseThrow(() -> new IllegalArgumentException("Token invàlid o pressupost no trobat"));

        if (budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Aquest pressupost no es pot acceptar (estat: " + budget.getStatus() + ")");
        }

        budget.setStatus(BudgetStatus.ACCEPTED);
        budget.setAcceptedAt(java.time.Instant.now());
        budget = budgetRepository.save(budget);

        // Aprovar cada fase al Vault
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        var phaseIds = lines.stream()
                .map(BudgetLine::getPhaseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (var phaseId : phaseIds) {
            try {
                vaultService.approvePhase(budget.getTenantId(), phaseId);
            } catch (Exception e) {
                // Si falla una fase, continuem amb les altres
                // La facturació es gestiona al Vault via InvoiceService/PaymentService
            }
        }

        return toBudgetResponse(budget, lines);
    }

    // ── Rebutjar pressupost (token públic) ──

    @Override
    @Transactional
    public BudgetResponse rejectBudget(String token, String reason) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Token invàlid");
        }

        var budget = budgetRepository.findByAcceptanceToken(tokenUuid)
                .orElseThrow(() -> new IllegalArgumentException("Token invàlid o pressupost no trobat"));

        if (budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Aquest pressupost no es pot rebutjar (estat: " + budget.getStatus() + ")");
        }

        budget.setStatus(BudgetStatus.REJECTED);
        budget.setRejectedAt(java.time.Instant.now());
        budget.setRejectedReason(reason);
        budget = budgetRepository.save(budget);

        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        return toBudgetResponse(budget, lines);
    }

    // ── Dashboard ──

    @Override
    public DashboardResponse getDashboard(UUID tenantId) {
        var budgets = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);

        var draftCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.DRAFT).count();
        var sentCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.SENT).count();
        var acceptedCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.ACCEPTED).count();
        var rejectedCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.REJECTED).count();
        var expiredCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.EXPIRED).count();
        var cancelledCount = budgets.stream().filter(b -> b.getStatus() == BudgetStatus.CANCELLED).count();

        var totalAcceptedAmount = budgets.stream()
                .filter(b -> b.getStatus() == BudgetStatus.ACCEPTED)
                .map(Budget::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var totalPendingAmount = budgets.stream()
                .filter(b -> b.getStatus() == BudgetStatus.SENT)
                .map(Budget::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardResponse.builder()
                .totalBudgets(budgets.size())
                .draftCount(draftCount)
                .sentCount(sentCount)
                .acceptedCount(acceptedCount)
                .rejectedCount(rejectedCount)
                .expiredCount(expiredCount)
                .cancelledCount(cancelledCount)
                .totalAcceptedAmount(totalAcceptedAmount)
                .totalPendingAmount(totalPendingAmount)
                .build();
    }

    // ── Privats ──

    private BudgetResponse toBudgetResponse(Budget budget, List<BudgetLine> lines) {
        var lineResponses = lines.stream().map(l -> BudgetResponse.BudgetLineResponse.builder()
                .id(l.getId())
                .phaseId(l.getPhaseId())
                .serviceId(l.getServiceId())
                .serviceName(l.getServiceName())
                .quantity(l.getQuantity())
                .unitPrice(l.getUnitPrice())
                .total(l.getTotal())
                .sortOrder(l.getSortOrder())
                .build()).toList();

        return BudgetResponse.builder()
                .id(budget.getId())
                .tenantId(budget.getTenantId())
                .budgetNumber(budget.getBudgetNumber())
                .profileId(budget.getProfileId())
                .version(budget.getVersion())
                .status(budget.getStatus())
                .subtotal(budget.getSubtotal())
                .discountTotal(budget.getDiscountTotal())
                .total(budget.getTotal())
                .validUntil(budget.getValidUntil())
                .notes(budget.getNotes())
                .clientNotes(budget.getClientNotes())
                .sentAt(budget.getSentAt())
                .acceptedAt(budget.getAcceptedAt())
                .rejectedAt(budget.getRejectedAt())
                .rejectedReason(budget.getRejectedReason())
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .lines(lineResponses)
                .build();
    }
}
