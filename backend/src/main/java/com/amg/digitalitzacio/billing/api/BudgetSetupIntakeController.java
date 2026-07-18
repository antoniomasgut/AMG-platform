package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.application.PostAcceptanceService;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetLineRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntake;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// Controlador de fitxes de configuració de pressupost (Budget Setup Intake)
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BudgetSetupIntakeController {

    private final BudgetSetupIntakeRepository intakeRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final TenantRepository tenantRepository;
    private final PostAcceptanceService postAcceptanceService;

    // Records de resposta/petició
    record IntakeResponse(
            String id,
            String token,
            String status,
            String budgetId,
            String tenantName,
            String sector,
            List<Integer> phaseNumbers,
            String intakeData,
            String intakeUrl
    ) {}

    record SaveIntakeRequest(String intakeData, boolean complete) {}

    // POST /api/v1/billing/budgets/{budgetId}/intake — crea o retorna la fitxa existent (idempotent)
    @PostMapping("/budgets/{budgetId}/intake")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public IntakeResponse createIntake(@PathVariable UUID budgetId) {
        // Si ja existeix, retorna la fitxa sense crear-ne una de nova
        return intakeRepository.findByBudgetId(budgetId)
                .map(this::toResponse)
                .orElseGet(() -> {
                    Budget budget = budgetRepository.findById(budgetId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pressupost no trobat"));

                    // Obté tenantName des del repositori de tenants
                    String tenantName = budget.getTenantId() != null
                            ? tenantRepository.findById(budget.getTenantId())
                                    .map(t -> t.getName())
                                    .orElse(null)
                            : null;

                    // Obté els números de fase des de les línies del pressupost
                    List<Integer> phases = budgetLineRepository.findByBudgetIdOrderBySortOrder(budgetId).stream()
                            .map(l -> l.getPhaseNumber())
                            .filter(n -> n != null)
                            .distinct()
                            .sorted()
                            .toList();

                    String phaseNumbersCsv = phases.isEmpty() ? null
                            : phases.stream().map(String::valueOf).collect(Collectors.joining(","));

                    String token = UUID.randomUUID().toString().replace("-", "");

                    BudgetSetupIntake intake = BudgetSetupIntake.builder()
                            .budgetId(budgetId)
                            .tenantId(budget.getTenantId())
                            .tenantName(tenantName)
                            .sector(budget.getSector())
                            .phaseNumbers(phaseNumbersCsv)
                            .token(token)
                            .status("PENDING")
                            .build();

                    return toResponse(intakeRepository.save(intake));
                });
    }

    // GET /api/v1/billing/budgets/{budgetId}/intake — obté la fitxa per pressupost (requereix auth)
    @GetMapping("/budgets/{budgetId}/intake")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<IntakeResponse> getIntakeByBudget(
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return intakeRepository.findByBudgetId(budgetId)
                .filter(i -> "SUPER_ADMIN".equals(principal.role()) || "ADMIN".equals(principal.role())
                        || i.getTenantId().equals(principal.tenantId()))
                .map(i -> ResponseEntity.ok(toResponse(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/v1/billing/status/{token} — pública, retorna l'estat del projecte per token
    @GetMapping("/status/{token}")
    public ProjectStatusResponse getProjectStatus(@PathVariable String token) {
        BudgetSetupIntake intake = intakeRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estat no trobat"));

        Budget budget = budgetRepository.findById(intake.getBudgetId()).orElse(null);
        var tenant = tenantRepository.findById(intake.getTenantId()).orElse(null);

        boolean configReceived   = true;
        // setupPaid = Stripe/GoCardless confirmat; ACCEPTED = pagament offline confirmat per AMG
        boolean paymentConfirmed = budget != null &&
                (Boolean.TRUE.equals(budget.getSetupPaid()) || budget.getStatus() == BudgetStatus.ACCEPTED);
        boolean setupCompleted   = "COMPLETE".equals(intake.getStatus());
        boolean isActive         = tenant != null && tenant.getActivePhases() != null
                                   && !tenant.getActivePhases().isBlank();
        boolean implementing     = setupCompleted && !isActive;

        return new ProjectStatusResponse(
                intake.getTenantName(), intake.getSector(),
                configReceived, intake.getCreatedAt(),
                paymentConfirmed, paymentConfirmed && budget != null ? budget.getUpdatedAt() : null,
                setupCompleted, intake.getCompletedAt(),
                implementing,
                isActive,
                "https://amgdl.com/ca/login"
        );
    }

    record ProjectStatusResponse(
            String tenantName, String sector,
            boolean configReceived, Instant configReceivedAt,
            boolean paymentConfirmed, Instant paymentConfirmedAt,
            boolean setupCompleted, Instant setupCompletedAt,
            boolean implementing,
            boolean active,
            String portalUrl
    ) {}

    // GET /api/v1/billing/intake/{token} — pública, retorna la fitxa per token
    @GetMapping("/intake/{token}")
    public IntakeResponse getIntakePublic(@PathVariable String token) {
        return intakeRepository.findByToken(token)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fitxa no trobada"));
    }

    // PUT /api/v1/billing/intake/{token} — pública, desa les dades de la fitxa
    @PutMapping("/intake/{token}")
    public IntakeResponse saveIntake(@PathVariable String token,
                                     @RequestBody SaveIntakeRequest request) {
        BudgetSetupIntake intake = intakeRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fitxa no trobada"));

        intake.setIntakeData(request.intakeData());

        if (request.complete()) {
            intake.setStatus("COMPLETE");
            intake.setCompletedAt(Instant.now());
            BudgetSetupIntake saved = intakeRepository.save(intake);
            postAcceptanceService.onSetupCompleted(
                    saved.getTenantId(), saved.getTenantName(), saved.getSector(),
                    "https://amgdl.com/setup-intake/" + saved.getToken());
            return toResponse(saved);
        } else if ("PENDING".equals(intake.getStatus())) {
            // Primer desar → passa a IN_PROGRESS
            intake.setStatus("IN_PROGRESS");
        }

        return toResponse(intakeRepository.save(intake));
    }

    // --- Mètodes auxiliars ---

    private IntakeResponse toResponse(BudgetSetupIntake i) {
        List<Integer> phases = parsePhaseNumbers(i.getPhaseNumbers());
        String intakeUrl = "https://amgdl.com/setup-intake/" + i.getToken();
        return new IntakeResponse(
                i.getId().toString(),
                i.getToken(),
                i.getStatus(),
                i.getBudgetId().toString(),
                i.getTenantName(),
                i.getSector(),
                phases.isEmpty() ? null : phases,
                i.getIntakeData(),
                intakeUrl
        );
    }

    private List<Integer> parsePhaseNumbers(String phaseNumbers) {
        if (phaseNumbers == null || phaseNumbers.isBlank()) return List.of();
        return Arrays.stream(phaseNumbers.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }
}
