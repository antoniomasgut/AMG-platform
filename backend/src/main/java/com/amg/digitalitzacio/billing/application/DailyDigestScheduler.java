package com.amg.digitalitzacio.billing.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetSetupIntakeRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessPaymentRepository;
import com.amg.digitalitzacio.gocardless.domain.GoCardlessPaymentStatus;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Digest diari a les 8:00 al xat de vendes: què tens pendent avui.
 * Complementa les notificacions reactives amb una vista accionable del matí.
 * Desactivable amb AMG_DAILY_DIGEST=false.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class DailyDigestScheduler {

    private final LeadRepository leadRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetSetupIntakeRepository intakeRepository;
    private final GoCardlessPaymentRepository goCardlessPaymentRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    @Scheduled(cron = "0 0 8 * * *")
    public void sendMorningDigest() {
        try {
            String flag = sysConfig.get("AMG_DAILY_DIGEST");
            if (flag != null && "false".equalsIgnoreCase(flag.trim())) return;
            String chatIdStr = sysConfig.get("AMG_SALES_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) return;

            Instant now = Instant.now();
            var sb = new StringBuilder();
            sb.append("☀️ <b>Bon dia — ")
              .append(LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ca"))))
              .append("</b>\n\n");

            boolean hasContent = false;

            // Leads nous de les últimes 24h
            long newLeads = leadRepository.countByCreatedAtAfterAndIsActiveTrue(now.minus(24, ChronoUnit.HOURS));
            if (newLeads > 0) {
                sb.append("📥 ").append(newLeads).append(" lead").append(newLeads != 1 ? "s" : "")
                  .append(" nou").append(newLeads != 1 ? "s" : "").append(" per contactar\n");
                hasContent = true;
            }

            // Pressupostos SENT esperant resposta (amb l'edat del més antic)
            var sent = budgetRepository.findByStatusOrderByCreatedAtDesc(BudgetStatus.SENT, PageRequest.of(0, 50));
            if (sent.getTotalElements() > 0) {
                long oldestDays = sent.getContent().stream()
                        .map(b -> b.getSentAt() != null ? b.getSentAt() : b.getCreatedAt())
                        .filter(java.util.Objects::nonNull)
                        .map(t -> Duration.between(t, now).toDays())
                        .max(Long::compare).orElse(0L);
                sb.append("📋 ").append(sent.getTotalElements()).append(" pressupost")
                  .append(sent.getTotalElements() != 1 ? "os" : "").append(" esperant resposta");
                if (oldestDays >= 3) sb.append(" (el més antic fa ").append(oldestDays).append(" dies ⚠️)");
                sb.append("\n");
                hasContent = true;
            }

            // Fitxes de configuració a mig omplir
            var pendingIntakes = intakeRepository.findByStatusInAndCreatedAtBefore(
                    List.of("PENDING", "IN_PROGRESS"), now);
            if (!pendingIntakes.isEmpty()) {
                sb.append("⏳ ").append(pendingIntakes.size()).append(" fitx")
                  .append(pendingIntakes.size() != 1 ? "es" : "a")
                  .append(" de configuració sense completar\n");
                hasContent = true;
            }

            // Cobraments SEPA fallits dels últims 30 dies
            var failed = goCardlessPaymentRepository.findByStatusAndCreatedAtAfter(
                    GoCardlessPaymentStatus.FAILED, now.minus(30, ChronoUnit.DAYS));
            if (!failed.isEmpty()) {
                sb.append("🔴 ").append(failed.size()).append(" cobrament")
                  .append(failed.size() != 1 ? "s" : "").append(" SEPA fallit")
                  .append(failed.size() != 1 ? "s" : "").append(" per resoldre\n");
                hasContent = true;
            }

            if (!hasContent) {
                sb.append("✅ Res pendent — dia net!\n");
            }
            sb.append("\n<a href=\"https://amgdl.com/portal/admin/pipeline\">Obrir el pipeline →</a>");

            telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), sb.toString());
            log.info("[Digest] Digest del matí enviat");
        } catch (Exception e) {
            log.warn("[Digest] Error enviant digest del matí: {}", e.getMessage());
        }
    }
}
