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
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final LeadRepository leadRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final TenantRepository tenantRepository;

    public record PipelineCard(
            String id,
            String tenantId,
            String name,
            String sector,
            String contact,
            String value,
            Instant date,
            String actionUrl
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

        // Columna 3: Pressupostos enviats
        var sentBudgets = budgetRepository
                .findByStatusOrderByCreatedAtDesc(BudgetStatus.SENT, PageRequest.of(0, 50))
                .getContent().stream()
                .map(b -> toBudgetCard(b, "PROPOSAL_SENT"))
                .toList();

        // Columna 4: Setup pendent (intake no completat)
        var pendingIntakes = intakeRepository.findAll().stream()
                .filter(i -> "PENDING".equals(i.getStatus()) || "IN_PROGRESS".equals(i.getStatus()))
                .limit(50)
                .map(i -> toIntakeCard(i, "SETUP_PENDING"))
                .toList();

        // Columna 5: Implementant (intake completat, però el tenant NO té activePhases encara)
        var completedIntakes = intakeRepository.findByStatusAndCompletedAtBefore("COMPLETE",
                        Instant.now().plusSeconds(1)).stream()
                .filter(i -> {
                    var tenant = tenantRepository.findById(i.getTenantId()).orElse(null);
                    // Només mostrar si el tenant existeix però encara no té fases actives
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
                "https://amgdl.com/portal/admin/leads/" + l.getId()
        );
    }

    private PipelineCard toBudgetCard(Budget b, String stage) {
        var tenant = tenantRepository.findById(b.getTenantId()).orElse(null);
        String name = tenant != null ? (tenant.getName() != null ? tenant.getName() : tenant.getEmail()) : "—";
        String value = b.getTotal() != null ? b.getTotal().toPlainString() + " €" : null;
        return new PipelineCard(
                b.getId().toString(), b.getTenantId().toString(),
                name, b.getSector(), null, value, b.getSentAt(),
                "https://amgdl.com/portal/admin/tenants/" + b.getTenantId()
        );
    }

    private PipelineCard toIntakeCard(BudgetSetupIntake i, String stage) {
        return new PipelineCard(
                i.getId().toString(), i.getTenantId().toString(),
                i.getTenantName() != null ? i.getTenantName() : "—",
                i.getSector(), null, null,
                "SETUP_PENDING".equals(stage) ? i.getCreatedAt() : i.getCompletedAt(),
                "https://amgdl.com/portal/admin/tenants/" + i.getTenantId() + "/wizard"
        );
    }

    private PipelineCard toTenantCard(Tenant t) {
        String sector = t.getSector() != null ? t.getSector().name() : null;
        return new PipelineCard(
                t.getId().toString(), t.getId().toString(),
                t.getName() != null ? t.getName() : t.getEmail(),
                sector, t.getEmail(), null,
                t.getUpdatedAt(),
                "https://amgdl.com/portal/admin/tenants/" + t.getId()
        );
    }
}
