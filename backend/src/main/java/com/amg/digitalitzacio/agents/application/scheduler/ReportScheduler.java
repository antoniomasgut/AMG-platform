package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduler {

    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(TZ);

    private final TenantChatLinkRepository chatLinkRepository;
    private final LeadRepository leadRepository;
    private final BudgetRepository budgetRepository;
    private final TelegramBotClient telegramBotClient;
    private final StringRedisTemplate redis;
    private final SystemConfigService sysConfig;

    // Resum diari a les 08:00
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyReport() {
        String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) return; // plataforma no activa

        chatLinkRepository.findAll().forEach(chatLink -> {
            if (chatLink.getTelegramChatId() == null) return;
            if (!Boolean.TRUE.equals(chatLink.getIsActive())) return;

            try {
                sendDailyReportForTenant(chatLink.getTenantId(), chatLink.getTelegramChatId());
            } catch (Exception e) {
                log.warn("[Report] Error enviant resum diari a tenant {}: {}", chatLink.getTenantId(), e.getMessage());
            }
        });
    }

    // Resum setmanal els dilluns a les 09:00
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReport() {
        chatLinkRepository.findAll().forEach(chatLink -> {
            if (chatLink.getTelegramChatId() == null) return;
            if (!Boolean.TRUE.equals(chatLink.getIsActive())) return;

            try {
                sendWeeklyReportForTenant(chatLink.getTenantId(), chatLink.getTelegramChatId());
            } catch (Exception e) {
                log.warn("[Report] Error enviant resum setmanal a tenant {}: {}", chatLink.getTenantId(), e.getMessage());
            }
        });
    }

    private void sendDailyReportForTenant(UUID tenantId, Long chatId) {
        String redisKey = "report:daily:" + tenantId + ":" + today();
        if (Boolean.TRUE.equals(redis.hasKey(redisKey))) return;

        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);

        long newLeads = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, since24h);
        long pendingBudgets = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);
        long pendingApproval = conversationsPendingApproval(tenantId);

        if (newLeads == 0 && pendingBudgets == 0 && pendingApproval == 0) return; // no cal enviar si tot buit

        String msg = "📊 *Resum diari — " + DATE_FMT.format(Instant.now()) + "*\n\n"
            + (newLeads > 0      ? "🟢 Nous leads (24h): *" + newLeads + "*\n" : "")
            + (pendingBudgets > 0 ? "⏳ Pressupostos pendents: *" + pendingBudgets + "*\n" : "")
            + (pendingApproval > 0 ? "✍️ Respostes pendents d'aprovació: *" + pendingApproval + "*\n" : "");

        telegramBotClient.sendMessage(chatId, msg);
        redis.opsForValue().set(redisKey, "1", 23, TimeUnit.HOURS);
    }

    private void sendWeeklyReportForTenant(UUID tenantId, Long chatId) {
        String redisKey = "report:weekly:" + tenantId + ":" + thisMonday();
        if (Boolean.TRUE.equals(redis.hasKey(redisKey))) return;

        Instant since7d = Instant.now().minus(7, ChronoUnit.DAYS);
        Instant since14d = Instant.now().minus(14, ChronoUnit.DAYS);

        long leadsThisWeek = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, since7d);
        long leadsLastWeek = leadRepository.countByTenantIdAndCreatedAtAfterAndCreatedAtBefore(
                tenantId, since14d, since7d);
        long totalLeads    = leadRepository.countByTenantId(tenantId);
        long wonLeads      = leadRepository.countByTenantIdAndStage(tenantId, PipelineStage.WON);
        long sentBudgets   = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);
        long acceptedBudgets = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.ACCEPTED);

        double convRate    = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0;
        String trend       = leadsThisWeek >= leadsLastWeek ? "📈" : "📉";

        String msg = "📋 *Resum setmanal*\n\n"
            + trend + " Nous leads aquesta setmana: *" + leadsThisWeek + "* "
            + "(setmana anterior: " + leadsLastWeek + ")\n"
            + "🏆 Taxa de conversió: *" + String.format("%.1f", convRate) + "%*\n"
            + "📄 Pressupostos enviats: *" + sentBudgets + "*"
            + (acceptedBudgets > 0 ? " · acceptats: *" + acceptedBudgets + "*" : "") + "\n";

        telegramBotClient.sendMessage(chatId, msg);
        redis.opsForValue().set(redisKey, "1", 7, TimeUnit.DAYS);
    }

    // Mètode públic per a trucades des del controller (preview manual)
    public String buildDailyReportText(UUID tenantId) {
        Instant since24h = Instant.now().minus(24, ChronoUnit.HOURS);
        long newLeads     = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, since24h);
        long pendingBudgets = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);
        return "📊 Resum diari — " + DATE_FMT.format(Instant.now()) + "\n"
            + "Nous leads (24h): " + newLeads + "\n"
            + "Pressupostos pendents: " + pendingBudgets;
    }

    public String buildWeeklyReportText(UUID tenantId) {
        Instant since7d = Instant.now().minus(7, ChronoUnit.DAYS);
        long leadsThisWeek = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, since7d);
        long totalLeads    = leadRepository.countByTenantId(tenantId);
        long wonLeads      = leadRepository.countByTenantIdAndStage(tenantId, PipelineStage.WON);
        double convRate    = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0;
        return "📋 Resum setmanal\n"
            + "Nous leads: " + leadsThisWeek + "\n"
            + "Conversió: " + String.format("%.1f", convRate) + "%";
    }

    private long conversationsPendingApproval(UUID tenantId) {
        // Depèn de ConversationRepository — s'injecta via el servei de la plataforma
        return 0; // simplificat: el controller pot ampliar-ho
    }

    private String today() {
        return java.time.LocalDate.now(TZ).toString();
    }

    private String thisMonday() {
        return java.time.LocalDate.now(TZ)
                .with(java.time.DayOfWeek.MONDAY).toString();
    }
}
