package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.auth.domain.*;
import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.vault.application.InvoiceService;
import com.amg.digitalitzacio.vault.application.PaymentService;
import com.amg.digitalitzacio.vault.application.ProfileService;
import com.amg.digitalitzacio.vault.application.VaultService;
import lombok.RequiredArgsConstructor;
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
        1, "F1 · Comunicació base",
        2, "F2 · Gestió de cites",
        3, "F3 · Pressupostos",
        4, "F4 · Fidelització",
        5, "F5 · Equip"
    );

    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final DiscountRepository discountRepository;
    private final ProfileService profileService;
    private final VaultService vaultService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TenantRepository tenantRepository;
    private final SectorPhaseRepository sectorPhaseRepository;

    @Override
    @Transactional
    public BudgetResponse createBudget(UUID tenantId, CreateBudgetRequest request) {
        var subtotal = BigDecimal.ZERO;
        var lines = new ArrayList<BudgetLine>();

        if (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()) {
            // Mode NexeLocal: preus per fase des de sector_phases (independents, no ordinals)
            var sector = resolveSector(tenantId, request.sector());
            var sortedPhases = request.phaseNumbers().stream().sorted().toList();

            for (int i = 0; i < sortedPhases.size(); i++) {
                var pn = sortedPhases.get(i);
                var phaseData = sector != null
                        ? sectorPhaseRepository.findBySectorAndPhaseNumber(sector, pn).orElse(null)
                        : null;
                var setupLine = phaseData != null ? phaseData.getSetupPrice() : BigDecimal.ZERO;
                var monthly = phaseData != null ? phaseData.getMonthlyPrice() : BigDecimal.ZERO;
                var name = phaseData != null ? "F" + pn + " · " + phaseData.getName()
                        : PHASE_NAMES.getOrDefault(pn, "Fase " + pn);
                subtotal = subtotal.add(setupLine);
                lines.add(BudgetLine.builder()
                        .phaseNumber(pn).serviceName(name)
                        .unitPrice(setupLine).total(setupLine)
                        .monthlyPrice(monthly).sortOrder(i)
                        .build());
            }

            // Add-on worker tier (si no és AUTONOMO)
            var tierAddon = resolveWorkerTier(tenantId, request.businessSize());
            if (tierAddon.setup().compareTo(BigDecimal.ZERO) > 0) {
                subtotal = subtotal.add(tierAddon.setup());
                lines.add(BudgetLine.builder()
                        .serviceName("Add-on equip — " + tierAddon.name().toLowerCase())
                        .unitPrice(tierAddon.setup()).total(tierAddon.setup())
                        .monthlyPrice(tierAddon.monthly()).sortOrder(lines.size())
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
        var total = subtotal.subtract(discountTotal);
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
                .budgetNumber(generateBudgetNumber())
                .status(BudgetStatus.DRAFT)
                .subtotal(subtotal).discountTotal(discountTotal).total(total)
                .notes(request.notes()).clientNotes(request.clientNotes())
                .recommendation(request.recommendation())
                .recommendedPhaseIds(recPhaseIds)
                .validUntil(validUntil)
                .sector(resolvedSector)
                .businessSize(resolvedSize)
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
    public List<BudgetResponse> listBudgets(UUID tenantId, String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var budgetPage = (status != null && !status.isBlank())
                ? budgetRepository.findByTenantIdAndStatus(tenantId, BudgetStatus.valueOf(status), pageable)
                : budgetRepository.findByTenantId(tenantId, pageable);
        return budgetPage.stream().map(b -> toBudgetResponse(b)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetResponse> listAllBudgets(String status, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var budgetPage = (status != null && !status.isBlank())
                ? budgetRepository.findByStatusOrderByCreatedAtDesc(BudgetStatus.valueOf(status), pageable)
                : budgetRepository.findAllByOrderByCreatedAtDesc(pageable);
        return budgetPage.stream().map(b -> toBudgetResponse(b)).toList();
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

        if (request.phaseNumbers() != null && !request.phaseNumbers().isEmpty()) {
            var sector = resolveSector(budget.getTenantId(), request.sector());
            var sortedPhases = request.phaseNumbers().stream().sorted().toList();

            for (int i = 0; i < sortedPhases.size(); i++) {
                var pn = sortedPhases.get(i);
                var phaseData = sector != null
                        ? sectorPhaseRepository.findBySectorAndPhaseNumber(sector, pn).orElse(null)
                        : null;
                var setupLine = phaseData != null ? phaseData.getSetupPrice() : BigDecimal.ZERO;
                var monthly = phaseData != null ? phaseData.getMonthlyPrice() : BigDecimal.ZERO;
                var name = phaseData != null ? "F" + pn + " · " + phaseData.getName()
                        : PHASE_NAMES.getOrDefault(pn, "Fase " + pn);
                subtotal = subtotal.add(setupLine);
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .phaseNumber(pn).serviceName(name)
                        .unitPrice(setupLine).total(setupLine)
                        .monthlyPrice(monthly).sortOrder(i)
                        .build());
            }

            var tierAddon = resolveWorkerTier(budget.getTenantId(), request.businessSize());
            if (tierAddon.setup().compareTo(BigDecimal.ZERO) > 0) {
                subtotal = subtotal.add(tierAddon.setup());
                lines.add(BudgetLine.builder().budgetId(budgetId)
                        .serviceName("Add-on equip — " + tierAddon.name().toLowerCase())
                        .unitPrice(tierAddon.setup()).total(tierAddon.setup())
                        .monthlyPrice(tierAddon.monthly()).sortOrder(lines.size())
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
        budget.setSubtotal(subtotal);
        budget.setDiscountTotal(discountTotal);
        budget.setTotal(subtotal.subtract(discountTotal));
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

        var url = "https://amgdl.com/accept-budget?token=" + token;
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
    public BudgetResponse previewBudget(String token) {
        var budget = budgetRepository.findByAcceptanceToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Token invàlid o caducat"));
        return toBudgetResponse(budget);
    }

    @Override
    @Transactional
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
            for (var key : phaseKeys) {
                // NexeLocal: claus "F1".."F5"
                if (key != null && key.matches("F[1-5]")) continue;
                // Catàleg: UUID → aprova la fase al vault
                try {
                    var phaseId = UUID.fromString(key);
                    vaultService.approvePhase(budget.getTenantId(), phaseId);
                } catch (Exception ignored) {}
            }
        }
        return new AcceptRejectResponse("ACCEPTED", "Pressupost acceptat correctament");
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

    private BusinessSector resolveSector(UUID tenantId, String reqSector) {
        if (reqSector != null) {
            try { return BusinessSector.valueOf(reqSector.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }
        return tenantRepository.findById(tenantId).map(Tenant::getSector).orElse(null);
    }

    private WorkerTierAddon resolveWorkerTier(UUID tenantId, String reqSize) {
        if (reqSize != null) {
            try { return WorkerTierAddon.from(BusinessSize.valueOf(reqSize.toUpperCase())); }
            catch (IllegalArgumentException ignored) {}
        }
        return tenantRepository.findById(tenantId)
                .map(t -> WorkerTierAddon.from(t.getBusinessSize()))
                .orElse(WorkerTierAddon.AUTONOMO);
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

        var addonLines = lines.stream().filter(l -> l.getPhaseId() == null && l.getPhaseNumber() == null).toList();
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
                null,
                budget.getAcceptanceToken() != null
                    ? "https://amgdl.com/accept-budget?token=" + budget.getAcceptanceToken()
                    : null,
                budget.getValidUntil(), budget.getCreatedAt(),
                profileId, phaseIds, budget.getNotes(), budget.getClientNotes(),
                budget.getTenantId(), tenantName,
                budget.getRecommendation(), recPhaseIds,
                phaseNumbers.isEmpty() ? null : phaseNumbers,
                budget.getSector(), budget.getBusinessSize());
    }
}
