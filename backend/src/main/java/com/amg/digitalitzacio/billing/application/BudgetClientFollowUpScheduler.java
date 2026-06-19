package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Envia recordatoris per email al client quan un pressupost porta dies sense resposta.
 * Separat de BudgetAmgFollowUpScheduler (que notifica l'equip intern AMG via Telegram).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetClientFollowUpScheduler {

    private static final int WINDOW_HOURS = 12;

    private final BudgetRepository budgetRepository;
    private final TenantRepository tenantRepository;
    private final FollowupLogRepository followupLogRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 30 9 * * *")
    public void sendClientReminders() {
        checkDay(2);
        checkDay(5);
    }

    private void checkDay(int daysSinceSent) {
        Instant now    = Instant.now();
        Instant midDay = now.minus(daysSinceSent, ChronoUnit.DAYS);
        Instant from   = midDay.minus(WINDOW_HOURS, ChronoUnit.HOURS);
        Instant to     = midDay.plus(WINDOW_HOURS, ChronoUnit.HOURS);

        var budgets = budgetRepository.findByStatusAndSentAtBetween(BudgetStatus.SENT, from, to);
        if (budgets.isEmpty()) return;

        log.info("[ClientFollowUp] D{} — {} pressupostos pendents de resposta del client", daysSinceSent, budgets.size());

        for (var budget : budgets) {
            String logType = "BUDGET_CLIENT_D" + daysSinceSent;
            if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(budget.getTenantId(), logType, budget.getId())) {
                continue;
            }

            try {
                var tenant = tenantRepository.findById(budget.getTenantId()).orElse(null);
                if (tenant == null || tenant.getEmail() == null || tenant.getEmail().isBlank()) continue;

                String clientName = tenant.getName() != null ? tenant.getName() : "client";
                String email = tenant.getEmail();
                String budgetRef = budget.getBudgetNumber() != null ? budget.getBudgetNumber() : budget.getId().toString().substring(0, 8).toUpperCase();
                String acceptUrl = "https://amgdl.com/ca/login";

                String subject = daysSinceSent == 2
                        ? "Tens un pressupost pendent · " + budgetRef
                        : "Recordatori: pressupost pendent · " + budgetRef;

                String body = buildEmailBody(daysSinceSent, clientName, budgetRef, acceptUrl);
                emailService.sendEmail(email, subject, body);

                var log2 = new FollowupLog();
                log2.setTenantId(budget.getTenantId());
                log2.setType(logType);
                log2.setEntityId(budget.getId());
                followupLogRepository.save(log2);

                log.info("[ClientFollowUp] D{} email enviat a {} (budget {})", daysSinceSent, email, budget.getId());
            } catch (Exception e) {
                log.warn("[ClientFollowUp] Error D{} budget {}: {}", daysSinceSent, budget.getId(), e.getMessage());
            }
        }
    }

    private String buildEmailBody(int day, String name, String budgetRef, String loginUrl) {
        return switch (day) {
            case 2 -> """
                    Hola %s,

                    Et recordem que tens el pressupost %s pendent de revisió.

                    Pots accedir al teu portal per veure'l i acceptar-lo:
                    %s

                    Si tens alguna pregunta, escriu-nos a info@amgdl.com o per WhatsApp al +34 614 492 062.

                    Una salutació,
                    L'equip AMG Digitalitzacions
                    """.formatted(name, budgetRef, loginUrl);
            case 5 -> """
                    Hola %s,

                    Han passat 5 dies des que et vam enviar el pressupost %s i encara no hem rebut resposta.

                    Si tens dubtes sobre el pressupost o vols modificar alguna cosa, estem a la teva disposició.
                    Pots contactar-nos directament:
                    · Email: info@amgdl.com
                    · WhatsApp: +34 614 492 062
                    · Portal: %s

                    Si has decidit no continuar, no cal que facis res; tanquem el pressupost automàticament als 10 dies.

                    Gràcies per considerar els nostres serveis,
                    L'equip AMG Digitalitzacions
                    """.formatted(name, budgetRef, loginUrl);
            default -> "";
        };
    }
}
