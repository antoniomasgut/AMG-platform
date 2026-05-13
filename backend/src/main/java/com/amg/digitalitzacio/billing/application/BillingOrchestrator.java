package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.application.InvoiceService;
import com.amg.digitalitzacio.vault.application.PaymentService;
import com.amg.digitalitzacio.vault.application.ProfileService;
import com.amg.digitalitzacio.vault.application.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingOrchestrator implements BillingService {

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final DiscountRepository discountRepository;
    private final ProfileService profileService;
    private final VaultService vaultService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request) {
        var budgetResponse = profileService.calculateBudget(request.profileId(),
                request.addonIds(), true);

        var subtotal = BigDecimal.ZERO;
        var lines = new ArrayList<BudgetLine>();

        int sortOrder = 0;
        for (var phase : budgetResponse.phases()) {
            for (var svc : phase.services()) {
                var lineTotal = svc.salePrice();
                subtotal = subtotal.add(lineTotal);
                lines.add(BudgetLine.builder()
                        .phaseId(phase.phase().id())
                        .serviceId(svc.id())
                        .serviceName(svc.name())
                        .unitPrice(svc.salePrice())
                        .total(lineTotal)
                        .sortOrder(sortOrder++)
                        .build());
            }
        }

        if (budgetResponse.addons() != null) {
            for (var addon : budgetResponse.addons()) {
                subtotal = subtotal.add(addon.salePrice());
                lines.add(BudgetLine.builder()
                        .serviceId(addon.id())
                        .serviceName(addon.name())
                        .unitPrice(addon.salePrice())
                        .total(addon.salePrice())
                        .sortOrder(sortOrder++)
                        .build());
            }
        }

        var discountTotal = applyDiscounts(request.discountIds(), subtotal);
        var total = subtotal.subtract(discountTotal);
        var validUntil = request.validUntil() != null ? request.validUntil() : LocalDate.now().plus(30, ChronoUnit.DAYS);

        var budget = Budget.builder()
                .tenantId(tenantId).profileId(request.profileId())
                .budgetNumber(generateBudgetNumber())
                .status(BudgetStatus.DRAFT)
                .subtotal(subtotal).discountTotal(discountTotal).total(total)
                .notes(request.notes()).clientNotes(request.clientNotes())
                .validUntil(validUntil)
                .build();
        budget = budgetRepository.save(budget);

        for (var line : lines) {
            line.setBudgetId(budget.getId());
        }
        budgetLineRepository.saveAll(lines);

        return toBudgetResponse(budget, request.profileId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgets(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var budgetPage = (status != null && !status.isBlank())
                ? budgetRepository.findByTenantIdAndStatus(tenantId, BudgetStatus.valueOf(status), pageable)
                : budgetRepository.findByTenantId(tenantId, pageable);
        return budgetPage.stream().map(b -> toBudgetResponse(b, b.getProfileId())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID budgetId, boolean includeInternalNotes) {
        var budget = findBudget(budgetId);
        return toBudgetResponse(budget, budget.getProfileId());
    }

    @Override
    @Transactional
    public BudgetResponse updateBudget(UUID budgetId, UpdateBudgetRequest request) {
        var budget = findBudget(budgetId);
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT budgets can be updated");
        }
        budget.setVersion(budget.getVersion() + 1);

        budgetLineRepository.deleteByBudgetId(budgetId);

        var budgetResponse = profileService.calculateBudget(request.profileId(),
                request.addonIds(), true);
        var subtotal = BigDecimal.ZERO;
        var lines = new ArrayList<BudgetLine>();
        int sortOrder = 0;
        for (var phase : budgetResponse.phases()) {
            for (var svc : phase.services()) {
                var lineTotal = svc.salePrice();
                subtotal = subtotal.add(lineTotal);
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .phaseId(phase.phase().id()).serviceId(svc.id())
                        .serviceName(svc.name()).unitPrice(svc.salePrice())
                        .total(lineTotal).sortOrder(sortOrder++).build());
            }
        }
        if (budgetResponse.addons() != null) {
            for (var addon : budgetResponse.addons()) {
                subtotal = subtotal.add(addon.salePrice());
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .serviceId(addon.id()).serviceName(addon.name())
                        .unitPrice(addon.salePrice()).total(addon.salePrice())
                        .sortOrder(sortOrder++).build());
            }
        }

        var discountTotal = applyDiscounts(request.discountIds(), subtotal);
        budget.setSubtotal(subtotal);
        budget.setDiscountTotal(discountTotal);
        budget.setTotal(subtotal.subtract(discountTotal));
        if (request.validUntil() != null) budget.setValidUntil(request.validUntil());
        if (request.notes() != null) budget.setNotes(request.notes());
        if (request.clientNotes() != null) budget.setClientNotes(request.clientNotes());

        budget = budgetRepository.save(budget);
        budgetLineRepository.saveAll(lines);
        return toBudgetResponse(budget, budget.getProfileId());
    }

    @Override
    @Transactional
    public void cancelBudget(UUID budgetId) {
        var budget = findBudget(budgetId);
        if (budget.getStatus() != BudgetStatus.DRAFT && budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Only DRAFT or SENT budgets can be cancelled");
        }
        budget.setStatus(BudgetStatus.CANCELLED);
        budgetRepository.save(budget);
    }

    @Override
    @Transactional
    public BudgetSendResponse sendBudget(UUID budgetId) {
        var budget = findBudget(budgetId);
        if (budget.getStatus() != BudgetStatus.DRAFT) {
            throw new IllegalArgumentException("Only DRAFT budgets can be sent");
        }
        var token = UUID.randomUUID().toString().replace("-", "");
        budget.setStatus(BudgetStatus.SENT);
        budget.setAcceptanceToken(token);
        budget.setSentAt(Instant.now());
        budgetRepository.save(budget);

        var url = "https://portal.amg.cat/accept-budget?token=" + token;
        return new BudgetSendResponse(budget.getId(), "SENT", budget.getSentAt(), url);
    }

    @Override
    @Transactional
    public AcceptRejectResponse acceptBudget(String token) {
        var budget = budgetRepository.findByAcceptanceToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Budget is not in SENT status");
        }
        budget.setStatus(BudgetStatus.ACCEPTED);
        budget.setAcceptedAt(Instant.now());
        budget.setAcceptanceToken(null);
        budgetRepository.save(budget);

        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        var phaseIds = lines.stream()
                .map(BudgetLine::getPhaseId)
                .filter(Objects::nonNull)
                .distinct().toList();

        for (var phaseId : phaseIds) {
            try {
                vaultService.approvePhase(budget.getTenantId(), phaseId);
            } catch (Exception e) {
                // Log but continue — phases may already be approved
            }
        }

        return new AcceptRejectResponse("ACCEPTED", "Pressupost acceptat correctament");
    }

    @Override
    @Transactional
    public AcceptRejectResponse rejectBudget(String token, String reason) {
        var budget = budgetRepository.findByAcceptanceToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Budget is not in SENT status");
        }
        budget.setStatus(BudgetStatus.REJECTED);
        budget.setRejectedAt(Instant.now());
        budget.setRejectedReason(reason);
        budget.setAcceptanceToken(null);
        budgetRepository.save(budget);
        return new AcceptRejectResponse("REJECTED", "Pressupost rebutjat");
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID tenantId) {
        var pending = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);

        var budgets = budgetRepository.findByTenantId(tenantId, PageRequest.of(0, 1));
        var lastBudget = budgets.stream().findFirst().map(b ->
                new DashboardResponse.BudgetSummary(b.getId(), b.getBudgetNumber(), b.getTotal(),
                        b.getStatus().name(), b.getSentAt())).orElse(null);

        var totalSpent = BigDecimal.ZERO;
        // Simple calculation from accepted budgets
        var allBudgets = budgetRepository.findByTenantIdAndStatus(tenantId, BudgetStatus.ACCEPTED, PageRequest.of(0, 100));
        for (var b : allBudgets) {
            totalSpent = totalSpent.add(b.getTotal());
        }

        return new DashboardResponse((int) pending, lastBudget, totalSpent, List.of());
    }

    private BigDecimal applyDiscounts(List<UUID> discountIds, BigDecimal subtotal) {
        if (discountIds == null || discountIds.isEmpty()) return BigDecimal.ZERO;
        var total = BigDecimal.ZERO;
        for (var id : discountIds) {
            var discount = discountRepository.findById(id).orElse(null);
            if (discount == null || !discount.getIsActive()) continue;
            switch (discount.getType()) {
                case PERCENTAGE -> total = total.add(subtotal.multiply(discount.getValue())
                        .divide(BigDecimal.valueOf(100)));
                case FIXED -> total = total.add(discount.getValue());
            }
            // appliedCount tracking is not incremented here —
            // discount limits and usage tracking are pending implementation
        }
        return total;
    }

    private String generateBudgetNumber() {
        var year = LocalDate.now().getYear();
        var count = budgetRepository.count() + 1;
        return String.format("BUD-%d-%04d", year, count);
    }

    private Budget findBudget(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private BudgetResponse toBudgetResponse(Budget budget, UUID profileId) {
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        var phaseMap = new LinkedHashMap<UUID, List<BudgetResponse.BudgetPhase.BudgetLine>>();
        var phases = new ArrayList<BudgetResponse.BudgetPhase>();
        var addons = new ArrayList<BudgetResponse.BudgetAddon>();

        if (profileId != null) {
            try {
                var profile = profileService.getProfile(profileId);
                for (var phaseResp : profile.phases()) {
                    var phaseLines = lines.stream()
                            .filter(l -> phaseResp.id() != null && l.getPhaseId() != null && l.getPhaseId().equals(phaseResp.id()))
                            .map(l -> new BudgetResponse.BudgetPhase.BudgetLine(
                                    l.getServiceName(), l.getUnitPrice(), l.getTotal()))
                            .toList();
                    if (!phaseLines.isEmpty()) {
                        var phaseTotal = phaseLines.stream()
                                .map(BudgetResponse.BudgetPhase.BudgetLine::total)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        phases.add(new BudgetResponse.BudgetPhase(
                                phaseResp.name(), phaseResp.sortOrder(), phaseLines, phaseTotal));
                    }
                }
            } catch (Exception ignored) {}
        }

        var addonLines = lines.stream().filter(l -> l.getPhaseId() == null).toList();
        for (var line : addonLines) {
            addons.add(new BudgetResponse.BudgetAddon(line.getServiceName(), line.getUnitPrice()));
        }

        return new BudgetResponse(
                budget.getId(), budget.getBudgetNumber(), budget.getStatus().name(),
                phases, addons, budget.getSubtotal(), budget.getDiscountTotal(), budget.getTotal(),
                budget.getSentAt(), budget.getAcceptedAt(), budget.getRejectedAt(),
                null, budget.getValidUntil(), budget.getCreatedAt());
    }
}
