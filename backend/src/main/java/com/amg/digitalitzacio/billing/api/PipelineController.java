package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntake;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineEvent;
import com.amg.digitalitzacio.leads.domain.PipelineEventRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final LeadRepository leadRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final TenantRepository tenantRepository;
    private final PipelineEventRepository pipelineEventRepository;

    public record PipelineCard(
            String id,
            String tenantId,
            String name,
            String sector,
            String contact,
            String value,
            Instant date,
            String actionUrl,
            String stage   // només per a cards de lead (null per a la resta)
    ) {}

    public record PipelineColumn(String stage, String label, List<PipelineCard> cards) {}

    public record PipelineView(List<PipelineColumn> columns, int total) {}

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PipelineView> getPipeline() {
        // Leads del tenant propietari (AMG) — els prospects gestionats per AMG
        var ownerTenantOpt = tenantRepository.findByIsOwnerTrue();
        UUID ownerTenantId = ownerTenantOpt.map(Tenant::getId).orElse(null);

        // Columna 1: Leads nous (NEW/CONTACTED)
        var newLeads = (ownerTenantId != null
                ? leadRepository.findByTenantId(ownerTenantId)
                : leadRepository.findAll()).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive())
                        && (l.getStage() == PipelineStage.NEW || l.getStage() == PipelineStage.CONTACTED))
                .limit(50)
                .map(this::toLeadCard)
                .toList();

        // Columna 2: Leads qualificats (QUALIFIED/PROPOSAL/NEGOTIATION)
        var qualifiedLeads = (ownerTenantId != null
                ? leadRepository.findByTenantId(ownerTenantId)
                : leadRepository.findAll()).stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive())
                        && (l.getStage() == PipelineStage.QUALIFIED
                            || l.getStage() == PipelineStage.PROPOSAL
                            || l.getStage() == PipelineStage.NEGOTIATION))
                .limit(50)
                .map(this::toLeadCard)
                .toList();

        // Columna 3: Pressupostos enviats — pre-carrega tenants per evitar N+1
        var sentBudgetList = budgetRepository
                .findByStatusOrderByCreatedAtDesc(BudgetStatus.SENT, PageRequest.of(0, 50))
                .getContent();

        // Columna 4 i 5: totes les intakes en una sola query
        var allIntakes = intakeRepository.findAll();
        var pendingIntakeList = allIntakes.stream()
                .filter(i -> "PENDING".equals(i.getStatus()) || "IN_PROGRESS".equals(i.getStatus()))
                .limit(50)
                .toList();
        var completeIntakeList = allIntakes.stream()
                .filter(i -> "COMPLETE".equals(i.getStatus()))
                .toList();

        // Pre-carrega tots els tenants referenciats en una sola query
        var neededIds = new HashSet<UUID>();
        sentBudgetList.forEach(b -> neededIds.add(b.getTenantId()));
        completeIntakeList.forEach(i -> neededIds.add(i.getTenantId()));
        var tenantMap = tenantRepository.findAllById(neededIds).stream()
                .collect(Collectors.toMap(Tenant::getId, t -> t));

        var sentBudgets = sentBudgetList.stream()
                .map(b -> toBudgetCard(b, tenantMap))
                .toList();

        var pendingIntakes = pendingIntakeList.stream()
                .map(i -> toIntakeCard(i, "SETUP_PENDING"))
                .toList();

        // Columna 5: Implementant (intake completat, però el tenant NO té activePhases encara)
        var completedIntakes = completeIntakeList.stream()
                .filter(i -> {
                    var tenant = tenantMap.get(i.getTenantId());
                    return tenant != null
                            && (tenant.getActivePhases() == null || tenant.getActivePhases().isBlank());
                })
                .limit(50)
                .map(i -> toIntakeCard(i, "IMPLEMENTING"))
                .toList();

        // Columna 6: Actius (tenant amb activePhases no nul — go-live confirmat)
        var activeCards = tenantRepository.findAll().stream()
                .filter(t -> t.getActivePhases() != null && !t.getActivePhases().isBlank())
                .sorted((a, b) -> {
                    if (a.getUpdatedAt() == null) return 1;
                    if (b.getUpdatedAt() == null) return -1;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .limit(50)
                .map(this::toTenantCard)
                .toList();

        var columns = List.of(
                new PipelineColumn("NEW_LEADS",     "Nous leads",         newLeads),
                new PipelineColumn("QUALIFIED",     "Qualificats",        qualifiedLeads),
                new PipelineColumn("PROPOSAL_SENT", "Proposta enviada",   sentBudgets),
                new PipelineColumn("SETUP_PENDING", "Setup pendent",      pendingIntakes),
                new PipelineColumn("IMPLEMENTING",  "Implementació",      completedIntakes),
                new PipelineColumn("ACTIVE",        "Actius",             activeCards)
        );

        int total = columns.stream().mapToInt(c -> c.cards().size()).sum();
        return ResponseEntity.ok(new PipelineView(columns, total));
    }

    private PipelineCard toLeadCard(Lead l) {
        String contact = l.getPhone() != null ? l.getPhone()
                : (l.getEmail() != null ? l.getEmail() : "—");
        String value = l.getEstimatedValue() != null ? l.getEstimatedValue().toPlainString() + " €" : null;
        return new PipelineCard(
                l.getId().toString(), null, l.getName(),
                l.getSource() != null ? l.getSource().name() : null,
                contact, value, l.getCreatedAt(),
                "https://amgdl.com/portal/admin/leads/" + l.getId(),
                l.getStage() != null ? l.getStage().name() : null
        );
    }

    private PipelineCard toBudgetCard(Budget b, Map<UUID, Tenant> tenantMap) {
        var tenant = tenantMap.get(b.getTenantId());
        String name = tenant != null ? (tenant.getName() != null ? tenant.getName() : tenant.getEmail()) : "—";
        String value = b.getTotal() != null ? b.getTotal().toPlainString() + " €" : null;
        return new PipelineCard(
                b.getId().toString(), b.getTenantId().toString(),
                name, b.getSector(), null, value, b.getSentAt(),
                "https://amgdl.com/portal/admin/tenants/" + b.getTenantId(),
                null
        );
    }

    private PipelineCard toIntakeCard(BudgetSetupIntake i, String stage) {
        return new PipelineCard(
                i.getId().toString(), i.getTenantId().toString(),
                i.getTenantName() != null ? i.getTenantName() : "—",
                i.getSector(), null, null,
                "SETUP_PENDING".equals(stage) ? i.getCreatedAt() : i.getCompletedAt(),
                "https://amgdl.com/portal/admin/tenants/" + i.getTenantId() + "/wizard",
                null
        );
    }

    private PipelineCard toTenantCard(Tenant t) {
        String sector = t.getSector() != null ? t.getSector().name() : null;
        return new PipelineCard(
                t.getId().toString(), t.getId().toString(),
                t.getName() != null ? t.getName() : t.getEmail(),
                sector, t.getEmail(), null,
                t.getUpdatedAt(),
                "https://amgdl.com/portal/admin/tenants/" + t.getId(),
                null
        );
    }

    // ── Transició manual d'etapa ────────────────────────────────────────────────

    record StageTransitionRequest(String stage, String notes) {}
    record StageTransitionResponse(String leadId, String fromStage, String toStage) {}

    @PutMapping("/{leadId}/stage")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<StageTransitionResponse> updateStage(
            @PathVariable UUID leadId,
            @RequestBody StageTransitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        var lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        PipelineStage newStage;
        try {
            newStage = PipelineStage.valueOf(request.stage().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etapa desconeguda: " + request.stage());
        }

        String fromStage = lead.getStage() != null ? lead.getStage().name() : null;
        lead.setStage(newStage);

        // SLA deadline per etapa
        Instant slaDeadline = switch (newStage) {
            case NEW                       -> Instant.now().plus(1, ChronoUnit.DAYS);
            case CONTACTED                 -> Instant.now().plus(3, ChronoUnit.DAYS);
            case QUALIFIED                 -> Instant.now().plus(7, ChronoUnit.DAYS);
            case PROPOSAL, NEGOTIATION     -> Instant.now().plus(2, ChronoUnit.DAYS);
            case WON, LOST, NURTURING      -> null;
        };
        lead.setSlaDeadline(slaDeadline);
        lead.setSlaAlerted(false);

        leadRepository.save(lead);

        var event = new PipelineEvent();
        event.setLeadId(leadId);
        event.setFromStage(fromStage);
        event.setToStage(newStage.name());
        event.setTriggeredBy("MANUAL");
        event.setActorId(principal != null ? principal.id() : null);
        event.setNotes(request.notes());
        pipelineEventRepository.save(event);

        return ResponseEntity.ok(new StageTransitionResponse(leadId.toString(), fromStage, newStage.name()));
    }

    // ── Historial d'events d'un lead ───────────────────────────────────────────

    @GetMapping("/{leadId}/events")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<PipelineEvent>> getLeadEvents(@PathVariable UUID leadId) {
        return ResponseEntity.ok(pipelineEventRepository.findByLeadIdOrderByCreatedAtDesc(leadId));
    }

    // ── Estadístiques per etapa ────────────────────────────────────────────────

    record PipelineStats(Map<String, Long> byStage, long total) {}

    @GetMapping("/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<PipelineStats> getStats() {
        Map<String, Long> counts = Arrays.stream(PipelineStage.values())
                .collect(Collectors.toMap(
                        PipelineStage::name,
                        leadRepository::countByStage
                ));

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return ResponseEntity.ok(new PipelineStats(counts, total));
    }
}
