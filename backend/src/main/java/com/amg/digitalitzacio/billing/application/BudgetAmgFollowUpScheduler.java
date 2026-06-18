package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLog;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Recorda a l'equip AMG que hi ha pressupostos enviats sense resposta.
 * No contacta el client — envia notificació interna a Telegram.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BudgetAmgFollowUpScheduler {

    private static final int WINDOW_HOURS = 12;
    private static final int[] FOLLOW_UP_DAYS = {2, 5, 10};

    private final BudgetRepository budgetRepository;
    private final TenantRepository tenantRepository;
    private final FollowupLogRepository followupLogRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService systemConfigService;

    @Scheduled(cron = "0 0 9 * * *")
    public void checkUnrespondedBudgets() {
        for (int day : FOLLOW_UP_DAYS) {
            checkDay(day);
        }
    }

    private void checkDay(int daysSinceSent) {
        Instant now     = Instant.now();
        Instant midDay  = now.minus(daysSinceSent, ChronoUnit.DAYS);
        Instant from    = midDay.minus(WINDOW_HOURS, ChronoUnit.HOURS);
        Instant to      = midDay.plus(WINDOW_HOURS, ChronoUnit.HOURS);

        var budgets = budgetRepository.findByStatusAndSentAtBetween(BudgetStatus.SENT, from, to);
        if (budgets.isEmpty()) return;

        log.info("[AMG-FollowUp] D{} — {} pressupostos sense resposta", daysSinceSent, budgets.size());

        for (var budget : budgets) {
            String logType = "BUDGET_AMG_D" + daysSinceSent;
            if (followupLogRepository.existsByTenantIdAndTypeAndEntityId(budget.getTenantId(), logType, budget.getId())) {
                continue;
            }

            try {
                notifySalesChat(budget, daysSinceSent);
                saveLog(budget, logType);
            } catch (Exception e) {
                log.warn("[AMG-FollowUp] Error processant budget {}: {}", budget.getId(), e.getMessage());
            }
        }
    }

    private void notifySalesChat(Budget budget, int day) {
        var tenant = tenantRepository.findById(budget.getTenantId()).orElse(null);
        String clientName = tenant != null
                ? (tenant.getName() != null ? tenant.getName() : tenant.getEmail())
                : "Desconegut";
        String total = budget.getTotal() != null ? budget.getTotal().toPlainString() + " €" : "—";
        String sector = budget.getSector() != null ? budget.getSector() : "—";

        String msg = switch (day) {
            case 2 -> """
                    📋 <b>Pressupost sense resposta · D2</b>
                    👤 %s · %s
                    💰 %s
                    💡 Considerar fer seguiment manual.
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(clientName, sector, total, budget.getTenantId());
            case 5 -> """
                    ⏳ <b>Pressupost sense resposta · D5</b>
                    👤 %s · %s
                    💰 %s
                    💡 5 dies sense resposta — contacte directe recomanat.
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(clientName, sector, total, budget.getTenantId());
            default -> """
                    🔴 <b>Pressupost sense resposta · D10</b>
                    👤 %s · %s
                    💰 %s
                    ⚠️ 10 dies — considerar tancar o moure a nurturing.
                    🔗 <a href="https://amgdl.com/portal/admin/tenants/%s">Veure tenant →</a>
                    """.formatted(clientName, sector, total, budget.getTenantId());
        };

        try {
            String chatIdStr = systemConfigService.get("AMG_SALES_CHAT_ID");
            if (chatIdStr != null && !chatIdStr.isBlank()) {
                telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), msg);
            }
        } catch (Exception e) {
            log.warn("[AMG-FollowUp] Error enviant Telegram D{} budget {}: {}", day, budget.getId(), e.getMessage());
        }
    }

    private void saveLog(Budget budget, String type) {
        var log2 = new FollowupLog();
        log2.setTenantId(budget.getTenantId());
        log2.setType(type);
        log2.setEntityId(budget.getId());
        followupLogRepository.save(log2);
    }
}
