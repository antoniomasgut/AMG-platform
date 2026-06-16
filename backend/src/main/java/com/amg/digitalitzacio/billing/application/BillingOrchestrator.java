package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.auth.application.PhaseActivationService;
import com.amg.digitalitzacio.auth.domain.*;
import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.application.InvoiceService;
import com.amg.digitalitzacio.vault.application.PaymentService;
import com.amg.digitalitzacio.vault.application.ProfileService;
import com.amg.digitalitzacio.vault.application.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BillingOrchestrator implements BillingService {

    private static final Map<Integer, String> PHASE_NAMES = Map.of(
        1, "Captació i Agent IA",
        2, "Agenda i Cites",
        3, "Pressupostos i Cobraments",
        4, "Fidelització i Seguiment",
        5, "Equip i Documentació"
    );

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final DiscountRepository discountRepository;
    private final ProfileService profileService;
    private final VaultService vaultService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TenantRepository tenantRepository;
    private final NexePricingFormula pricingFormula;
    private final LeadRepository leadRepository;
    private final PhaseActivationService phaseActivationService;
    private final PostAcceptanceService postAcceptanceService;

    @Override
    @Transactional
    public BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request) {
        var subtotal = BigDecimal.ZERO;
        var lines = new ArrayList<BudgetLine>();

        if (request.customLines() != null && !request.customLines().isEmpty()) {
            // Mode lliure: línies personalitzades (sense catàleg)
            for (int i = 0; i < request.customLines().size(); i++) {
                var cl  = request.customLines().get(i);
                var qty = cl.quantity() != null ? cl.quantity() : 1;
                var lineTotal = cl.unitPrice().multiply(BigDecimal.valueOf(qty));
                subtotal = subtotal.add(lineTotal);
                lines.add(BudgetLine.builder()
                        .serviceName(cl.description())
                        .quantity(qty)
                        .unitPrice(cl.unitPrice()).total(lineTotal)
                        .monthlyPrice(cl.monthlyPrice() != null ? cl.monthlyPrice() : BigDecimal.ZERO)
                        .sortOrder(i)
                        .build());
            }
        } else if (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()) {
            // Mode NexeLocal: fórmula Σfases × factor_sector × factor_mida
            var sector = resolveSector(tenantId, request.sector());
            var size   = resolveSize(tenantId, request.businessSize());
            var sortedPhases = request.phaseNumbers().stream().sorted().toList();

            for (int i = 0; i < sortedPhases.size(); i++) {
                var pn    = sortedPhases.get(i);
                var setup = pricingFormula.setupPerPhase(pn, sector, size);
                var monthly = pricingFormula.monthlyPerPhase(pn, sector, size);
                subtotal = subtotal.add(setup);
                lines.add(BudgetLine.builder()
                        .phaseNumber(pn).serviceName(pricingFormula.phaseLabel(pn))
                        .unitPrice(setup).total(setup)
                        .monthlyPrice(monthly).sortOrder(i)
                        .build());
            }
        } else {
            // Mode catàleg: preus des del perfil
            var budgetResponse = profileService.calculateBudget(request.profileId(),
                    request.addonIds(), true);
            int sortOrder = 0;
            for (var phase : budgetResponse.phases()) {
                for (var svc : phase.services()) {
                    var lineSetup = svc.setupPrice();
                    var lineMonthly = svc.monthlyPrice();
                    subtotal = subtotal.add(lineSetup);
                    lines.add(BudgetLine.builder()
                            .phaseId(phase.phase().id())
                            .serviceId(svc.id())
                            .serviceName(svc.name())
                            .unitPrice(lineSetup).total(lineSetup)
                            .monthlyPrice(lineMonthly)
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
                            .unitPrice(addon.salePrice()).total(addon.salePrice())
                            .monthlyPrice(BigDecimal.ZERO)
                            .sortOrder(lines.size())
                            .build());
                }
            }
        }

        var discountTotal = applyDiscounts(request.discountIds(), subtotal);
        var offerDiscount = computeOfferDiscount(subtotal, request.offerPercent());
        var total = subtotal.subtract(discountTotal).subtract(offerDiscount);
        var validUntil = request.validUntil() != null ? request.validUntil() : LocalDate.now().plus(30, ChronoUnit.DAYS);

        var recPhaseIds = request.recommendedPhaseIds() != null
                ? request.recommendedPhaseIds().stream().map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(","))
                : null;

        var resolvedSector = request.sector() != null ? request.sector()
                : (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()
                    ? tenantRepository.findById(tenantId).map(t -> t.getSector() != null ? t.getSector().name() : null).orElse(null)
                    : null);
        var resolvedSize = request.businessSize() != null ? request.businessSize()
                : (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()
                    ? tenantRepository.findById(tenantId).map(t -> t.getBusinessSize() != null ? t.getBusinessSize().name() : null).orElse(null)
                    : null);

        var budget = Budget.builder()
                .tenantId(tenantId).profileId(request.profileId())
                .budgetNumber(generateBudgetNumber(tenantId))
                .status(BudgetStatus.DRAFT)
                .subtotal(subtotal).discountTotal(discountTotal).total(total)
                .notes(request.notes()).clientNotes(request.clientNotes())
                .recommendation(request.recommendation())
                .recommendedPhaseIds(recPhaseIds)
                .validUntil(validUntil)
                .sector(resolvedSector)
                .businessSize(resolvedSize)
                .leadId(request.leadId())
                .offerPercent(request.offerPercent())
                .build();
        budget = budgetRepository.save(budget);

        for (var line : lines) {
            line.setBudgetId(budget.getId());
        }
        budgetLineRepository.saveAll(lines);

        return toBudgetResponse(budget);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> listBudgets(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var budgetPage = (status != null && !status.isBlank())
                ? budgetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, BudgetStatus.valueOf(status), pageable)
                : budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        return budgetPage.map(this::toBudgetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BudgetResponse> listAllBudgets(String status, UUID tenantId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasTenant = tenantId != null;
        Page<Budget> budgetPage;
        if (hasTenant && hasStatus) {
            budgetPage = budgetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, BudgetStatus.valueOf(status), pageable);
        } else if (hasTenant) {
            budgetPage = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        } else if (hasStatus) {
            budgetPage = budgetRepository.findByStatusOrderByCreatedAtDesc(BudgetStatus.valueOf(status), pageable);
        } else {
            budgetPage = budgetRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return budgetPage.map(this::toBudgetResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse getBudget(UUID budgetId, boolean includeInternalNotes) {
        var budget = findBudget(budgetId);
        return toBudgetResponse(budget);
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

        var subtotal = BigDecimal.ZERO;
        var lines = new ArrayList<BudgetLine>();

        if (request.customLines() != null && !request.customLines().isEmpty()) {
            // Mode lliure: línies personalitzades
            for (int i = 0; i < request.customLines().size(); i++) {
                var cl  = request.customLines().get(i);
                var qty = cl.quantity() != null ? cl.quantity() : 1;
                var lineTotal = cl.unitPrice().multiply(BigDecimal.valueOf(qty));
                subtotal = subtotal.add(lineTotal);
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .serviceName(cl.description())
                        .quantity(qty)
                        .unitPrice(cl.unitPrice()).total(lineTotal)
                        .monthlyPrice(cl.monthlyPrice() != null ? cl.monthlyPrice() : BigDecimal.ZERO)
                        .sortOrder(i)
                        .build());
            }
        } else if (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()) {
            var sector = resolveSector(budget.getTenantId(), request.sector());
            var size   = resolveSize(budget.getTenantId(), request.businessSize());
            var sortedPhases = request.phaseNumbers().stream().sorted().toList();

            for (int i = 0; i < sortedPhases.size(); i++) {
                var pn    = sortedPhases.get(i);
                var setup = pricingFormula.setupPerPhase(pn, sector, size);
                var monthly = pricingFormula.monthlyPerPhase(pn, sector, size);
                subtotal = subtotal.add(setup);
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .phaseNumber(pn).serviceName(pricingFormula.phaseLabel(pn))
                        .unitPrice(setup).total(setup)
                        .monthlyPrice(monthly).sortOrder(i)
                        .build());
            }
        } else {
            var budgetResponse = profileService.calculateBudget(request.profileId(),
                    request.addonIds(), true);
            int sortOrder = 0;
            for (var phase : budgetResponse.phases()) {
                for (var svc : phase.services()) {
                    var lineSetup = svc.setupPrice();
                    var lineMonthly = svc.monthlyPrice();
                    subtotal = subtotal.add(lineSetup);
                    lines.add(BudgetLine.builder().budgetId(budgetId)
                            .phaseId(phase.phase().id()).serviceId(svc.id())
                            .serviceName(svc.name()).unitPrice(lineSetup)
                            .total(lineSetup).monthlyPrice(lineMonthly)
                            .sortOrder(sortOrder++).build());
                }
            }
            if (budgetResponse.addons() != null) {
                for (var addon : budgetResponse.addons()) {
                    subtotal = subtotal.add(addon.salePrice());
                    lines.add(BudgetLine.builder().budgetId(budgetId)
                            .serviceId(addon.id()).serviceName(addon.name())
                            .unitPrice(addon.salePrice()).total(addon.salePrice())
                            .sortOrder(lines.size()).build());
                }
            }
        }

        var discountTotal = applyDiscounts(request.discountIds(), subtotal);
        var offerPct = request.offerPercent() != null ? request.offerPercent() : budget.getOfferPercent();
        var offerDiscount = computeOfferDiscount(subtotal, offerPct);
        budget.setSubtotal(subtotal);
        budget.setDiscountTotal(discountTotal);
        budget.setOfferPercent(offerPct);
        budget.setTotal(subtotal.subtract(discountTotal).subtract(offerDiscount));
        if (request.validUntil() != null) budget.setValidUntil(request.validUntil());
        if (request.notes() != null) budget.setNotes(request.notes());
        if (request.clientNotes() != null) budget.setClientNotes(request.clientNotes());
        if (request.recommendation() != null) budget.setRecommendation(request.recommendation());
        if (request.recommendedPhaseIds() != null) budget.setRecommendedPhaseIds(
                request.recommendedPhaseIds().stream().map(UUID::toString)
                        .collect(java.util.stream.Collectors.joining(",")));
        if (request.sector() != null) budget.setSector(request.sector());
        if (request.businessSize() != null) budget.setBusinessSize(request.businessSize());

        budget = budgetRepository.save(budget);
        budgetLineRepository.saveAll(lines);
        return toBudgetResponse(budget);
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
        advanceLeadStage(budget, PipelineStage.PROPOSAL);
        final var sentBudget = budget;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        postAcceptanceService.onBudgetSent(sentBudget.getTenantId(), null, sentBudget.getTotal(), sentBudget.getId());
                    }
                });

        var url = "https://amgdl.com/accept-budget?token=" + token;
        return new BudgetSendResponse(budget.getId(), "SENT", budget.getSentAt(), url);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
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

        // Fases catàleg (UUID): aprova al vault després del commit per evitar rollback
        var phaseIds = lines.stream()
                .map(BudgetLine::getPhaseId)
                .filter(Objects::nonNull)
                .distinct().toList();
        if (!phaseIds.isEmpty()) {
            var tenantId = budget.getTenantId();
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (var phaseId : phaseIds) {
                            try {
                                vaultService.approvePhase(tenantId, phaseId);
                            } catch (Exception ignored) {}
                        }
                    }
                }
            );
        }

        // Fases NexeLocal (F1-F5): afegeix a contractedPhases del tenant
        var budgetSector = budget.getSector() != null
                ? safeBusinessSector(budget.getSector()) : null;
        var nexePhases = lines.stream()
                .map(BudgetLine::getPhaseNumber)
                .filter(Objects::nonNull)
                .flatMap(n -> nexePhasesForSector(budgetSector, n).stream())
                .distinct().sorted().toList();
        if (!nexePhases.isEmpty()) {
            addContractedPhases(budget.getTenantId(), nexePhases);
        }

        advanceLeadStage(budget, PipelineStage.WON);
        final var acceptedBudget = budget;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        postAcceptanceService.onBudgetAccepted(acceptedBudget);
                    }
                });

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
        advanceLeadStage(budget, PipelineStage.LOST);
        final var rejectedBudget = budget;
        final var rejectedReason = reason;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        postAcceptanceService.onBudgetRejected(rejectedBudget, rejectedReason);
                    }
                });
        return new AcceptRejectResponse("REJECTED", "Pressupost rebutjat");
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetResponse previewBudget(String token) {
        var budget = budgetRepository.findByAcceptanceToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invàlid o caducat"));
        return toBudgetResponse(budget);
    }

    @Override
    @Transactional(noRollbackFor = Exception.class)
    public AcceptRejectResponse acceptBudgetPhases(String token, List<String> phaseKeys) {
        var budget = budgetRepository.findByAcceptanceToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));
        if (budget.getStatus() != BudgetStatus.SENT) {
            throw new IllegalArgumentException("Budget is not in SENT status");
        }

        budget.setStatus(BudgetStatus.ACCEPTED);
        budget.setAcceptedAt(Instant.now());
        budget.setAcceptanceToken(null);
        budgetRepository.save(budget);

        if (phaseKeys != null && !phaseKeys.isEmpty()) {
            var nexePhases = new ArrayList<String>();
            for (var key : phaseKeys) {
                if (key == null) continue;
                if (key.matches("F[1-5]")) {
                    nexePhases.add(key);
                } else {
                    try {
                        var phaseId = UUID.fromString(key);
                        vaultService.approvePhase(budget.getTenantId(), phaseId);
                    } catch (Exception ignored) {}
                }
            }
            if (!nexePhases.isEmpty()) {
                addContractedPhases(budget.getTenantId(), nexePhases);
            }
        }
        advanceLeadStage(budget, PipelineStage.WON);
        final var acceptedBudget2 = budget;
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        postAcceptanceService.onBudgetAccepted(acceptedBudget2);
                    }
                });
        return new AcceptRejectResponse("ACCEPTED", "Pressupost acceptat correctament");
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID tenantId) {
        var pending = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);

        var budgets = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(0, 1));
        var lastBudget = budgets.stream().findFirst().map(b ->
                new DashboardResponse.BudgetSummary(b.getId(), b.getBudgetNumber(), b.getTotal(),
                        b.getStatus().name(), b.getSentAt())).orElse(null);

        var totalSpent = BigDecimal.ZERO;
        // Simple calculation from accepted budgets
        var allBudgets = budgetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, BudgetStatus.ACCEPTED, PageRequest.of(0, 100));
        for (var b : allBudgets) {
            totalSpent = totalSpent.add(b.getTotal());
        }

        return new DashboardResponse((int) pending, lastBudget, totalSpent, List.of());
    }

    private BigDecimal computeOfferDiscount(BigDecimal subtotal, Integer offerPercent) {
        if (offerPercent == null || offerPercent <= 0) return BigDecimal.ZERO;
        return subtotal.multiply(BigDecimal.valueOf(offerPercent))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    @Transactional
    public BudgetResponse createLeadProposal(UUID leadId, CreateBudgetRequest request) {
        var lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new com.amg.digitalitzacio.shared.exception.ResourceNotFoundException("Lead " + leadId));
        var sector = request.sector() != null ? request.sector() : lead.getInterviewSector();
        var size = request.businessSize() != null ? request.businessSize() : lead.getInterviewBusinessSize();
        var merged = new CreateBudgetRequest(
                request.profileId(), request.phaseIds(), request.addonIds(),
                request.notes(), request.clientNotes(), request.discountIds(),
                request.validUntil(), request.recommendation(), request.recommendedPhaseIds(),
                request.phaseNumbers(), sector, size, request.customLines(),
                leadId, request.offerPercent());
        return createBudget(lead.getTenantId(), merged);
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

    private String generateBudgetNumber(UUID tenantId) {
        var year = LocalDate.now().getYear();
        var count = budgetRepository.countByTenantId(tenantId) + 1;
        return String.format("BUD-%d-%04d", year, count);
    }

    private BusinessSector resolveSector(UUID tenantId, String reqSector) {
        if (reqSector != null) {
            try { return BusinessSector.valueOf(reqSector.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return tenantRepository.findById(tenantId).map(Tenant::getSector).orElse(null);
    }

    private BusinessSize resolveSize(UUID tenantId, String reqSize) {
        if (reqSize != null) {
            try { return BusinessSize.valueOf(reqSize.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return tenantRepository.findById(tenantId).map(Tenant::getBusinessSize).orElse(null);
    }

    private void advanceLeadStage(Budget budget, PipelineStage target) {
        if (budget.getLeadId() == null) return;
        leadRepository.findById(budget.getLeadId()).ifPresent(lead -> {
            boolean shouldUpdate = target == PipelineStage.LOST
                    || lead.getStage().ordinal() < target.ordinal();
            if (shouldUpdate) {
                lead.setStage(target);
                leadRepository.save(lead);
            }
        });
    }

    private List<String> nexePhasesForSector(BusinessSector sector, int phaseNumber) {
        if (phaseNumber > 1) return List.of("F" + Math.min(phaseNumber, 5));
        // SP1: F1 always; add F2 or F3 depending on sector profile
        if (sector == null) return List.of("F1");
        return switch (sector) {
            case FISIOTERAPEUTA, PSICOLEG, NUTRICIONISTA,
                 PERRUQUERIA, ESTETICA, VETERINARI, PERRUQUERIA_CANINA,
                 RESTAURANTE, GESTORIA, ACADEMIA, INMOBILIARIA -> List.of("F1", "F2");
            case PINTOR, ELECTRICISTA, FONTANER, JARDINER,
                 NETEJA, TALLER_MECANIC -> List.of("F1", "F3");
            default -> List.of("F1");
        };
    }

    private BusinessSector safeBusinessSector(String name) {
        if (name == null) return null;
        try { return BusinessSector.valueOf(name.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private void addContractedPhases(UUID tenantId, List<String> newPhases) {
        var tenant = tenantRepository.findById(tenantId).orElse(null);
        if (tenant == null) return;
        var existing = new TreeSet<String>();
        if (tenant.getContractedPhases() != null && !tenant.getContractedPhases().isBlank()) {
            existing.addAll(Arrays.asList(tenant.getContractedPhases().split(",")));
        }
        // Prerequisit: F2/F3/F4/F5 requereix F1 (Captació i Agent IA)
        var combined = new TreeSet<>(existing);
        combined.addAll(newPhases);
        boolean hasAdvanced = combined.stream().anyMatch(p -> p.equals("F2") || p.equals("F3") || p.equals("F4") || p.equals("F5"));
        if (hasAdvanced && !combined.contains("F1")) {
            throw new IllegalArgumentException("F1 (Captació i Agent IA) és prerequisit per contractar fases avançades (F2–F5)");
        }
        existing.addAll(newPhases);
        tenant.setContractedPhases(String.join(",", existing));
        tenantRepository.save(tenant);
        for (String phase : newPhases) {
            phaseActivationService.recordActivation(tenantId, phase, "BUDGET_ACCEPTED", null, null);
        }
    }

    private Budget findBudget(UUID id) {
        return budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found: " + id));
    }

    private BudgetResponse toBudgetResponse(Budget budget) {
        var profileId = budget.getProfileId();
        var lines = budgetLineRepository.findByBudgetIdOrderBySortOrder(budget.getId());
        var phases = new ArrayList<BudgetResponse.BudgetPhase>();
        var addons = new ArrayList<BudgetResponse.BudgetAddon>();

        var nexeLocalLines = lines.stream().filter(l -> l.getPhaseNumber() != null).toList();
        if (!nexeLocalLines.isEmpty()) {
            // Mode NexeLocal: agrupa per phaseNumber
            var byPhase = new java.util.TreeMap<Integer, java.util.List<BudgetLine>>();
            for (var l : nexeLocalLines) {
                byPhase.computeIfAbsent(l.getPhaseNumber(), k -> new ArrayList<>()).add(l);
            }
            for (var entry : byPhase.entrySet()) {
                var pn = entry.getKey();
                var phLines = entry.getValue();
                var phaseTotal = phLines.stream().map(BudgetLine::getUnitPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                var phaseMonthly = phLines.stream()
                        .map(l -> l.getMonthlyPrice() != null ? l.getMonthlyPrice() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                var budgetLines = phLines.stream()
                        .map(l -> new BudgetResponse.BudgetPhase.BudgetLine(
                                l.getServiceName(), l.getUnitPrice(),
                                l.getMonthlyPrice() != null ? l.getMonthlyPrice() : BigDecimal.ZERO))
                        .toList();
                phases.add(new BudgetResponse.BudgetPhase(
                        PHASE_NAMES.getOrDefault(pn, "Fase " + pn), pn, budgetLines,
                        phaseTotal, phaseMonthly, null, "F" + pn));
            }
        } else if (profileId != null) {
            try {
                var profile = profileService.getProfile(profileId);
                for (var phaseResp : profile.phases()) {
                    var phaseLines = lines.stream()
                            .filter(l -> phaseResp.id() != null && l.getPhaseId() != null && l.getPhaseId().equals(phaseResp.id()))
                            .map(l -> new BudgetResponse.BudgetPhase.BudgetLine(
                                    l.getServiceName(), l.getUnitPrice(),
                                    l.getMonthlyPrice() != null ? l.getMonthlyPrice() : BigDecimal.ZERO))
                            .toList();
                    if (!phaseLines.isEmpty()) {
                        var phaseTotal = phaseLines.stream()
                                .map(BudgetResponse.BudgetPhase.BudgetLine::setupPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        var phaseMonthlyTotal = phaseLines.stream()
                                .map(BudgetResponse.BudgetPhase.BudgetLine::monthlyPrice)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        phases.add(new BudgetResponse.BudgetPhase(
                                phaseResp.name(), phaseResp.sortOrder(), phaseLines, phaseTotal, phaseMonthlyTotal,
                                phaseResp.id(), phaseResp.id() != null ? phaseResp.id().toString() : null));
                    }
                }
            } catch (Exception ignored) {}
        }

        // Línies lliures (serviceId == null && phaseId == null && phaseNumber == null)
        var freeLines = lines.stream()
                .filter(l -> l.getPhaseId() == null && l.getPhaseNumber() == null && l.getServiceId() == null)
                .toList();
        var customLinesList = freeLines.stream()
                .map(l -> new BudgetResponse.CustomLine(
                        l.getServiceName(),
                        l.getQuantity() != null ? l.getQuantity() : 1,
                        l.getUnitPrice(),
                        l.getMonthlyPrice() != null ? l.getMonthlyPrice() : BigDecimal.ZERO,
                        l.getTotal()))
                .toList();

        // Addons del catàleg (serviceId != null, phaseId == null, phaseNumber == null)
        var addonLines = lines.stream()
                .filter(l -> l.getPhaseId() == null && l.getPhaseNumber() == null && l.getServiceId() != null)
                .toList();
        for (var line : addonLines) {
            addons.add(new BudgetResponse.BudgetAddon(line.getServiceName(), line.getUnitPrice()));
        }

        var phaseIds = lines.stream()
                .map(l -> l.getPhaseId())
                .filter(id -> id != null)
                .distinct()
                .toList();

        var phaseNumbers = lines.stream()
                .map(BudgetLine::getPhaseNumber)
                .filter(n -> n != null)
                .distinct().sorted()
                .toList();

        var tenantName = budget.getTenantId() != null
                ? tenantRepository.findById(budget.getTenantId()).map(t -> t.getName()).orElse(null)
                : null;

        var monthlyTotal = lines.stream()
                .map(l -> l.getMonthlyPrice() != null ? l.getMonthlyPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var recPhaseIds = budget.getRecommendedPhaseIds() != null
                && !budget.getRecommendedPhaseIds().isBlank()
                ? java.util.Arrays.stream(budget.getRecommendedPhaseIds().split(","))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .map(UUID::fromString).toList()
                : List.<UUID>of();

        return new BudgetResponse(
                budget.getId(), budget.getBudgetNumber(), budget.getStatus().name(),
                phases, addons, budget.getSubtotal(), budget.getDiscountTotal(), budget.getTotal(),
                monthlyTotal,
                budget.getSentAt(), budget.getAcceptedAt(), budget.getRejectedAt(),
                budget.getAcceptanceToken() != null
                    ? "https://amgdl.com/reject-budget?token=" + budget.getAcceptanceToken()
                    : null,
                budget.getAcceptanceToken() != null
                    ? "https://amgdl.com/accept-budget?token=" + budget.getAcceptanceToken()
                    : null,
                budget.getValidUntil(), budget.getCreatedAt(),
                profileId, phaseIds, budget.getNotes(), budget.getClientNotes(),
                budget.getTenantId(), tenantName,
                budget.getRecommendation(), recPhaseIds,
                phaseNumbers.isEmpty() ? null : phaseNumbers,
                budget.getSector(), budget.getBusinessSize(),
                customLinesList.isEmpty() ? null : customLinesList,
                budget.getLeadId(), budget.getOfferPercent());
    }
}
