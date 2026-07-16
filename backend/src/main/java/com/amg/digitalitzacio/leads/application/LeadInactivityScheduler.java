package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeadInactivityScheduler {

    private final LeadRepository leadRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService systemConfigService;

    @Scheduled(cron = "0 30 9 * * MON-FRI")
    @Transactional
    public void checkInactiveLeads() {
        String chatIdStr = systemConfigService.get("AMG_SALES_CHAT_ID");
        if (chatIdStr == null || chatIdStr.isBlank()) return;
        Long chatId;
        try { chatId = Long.parseLong(chatIdStr.trim()); } catch (NumberFormatException e) { return; }

        Instant now = Instant.now();

        // Alerta SLA vençut (proposta/negociació sense resposta en temps)
        checkSla(chatId, now);

        // Alerta inactivitat per etapa
        checkGroup("🔴 Negociació sense resposta",
                List.of(PipelineStage.NEGOTIATION), now.minus(2, ChronoUnit.DAYS), chatId);
        checkGroup("🟡 Proposta sense seguiment",
                List.of(PipelineStage.PROPOSAL), now.minus(3, ChronoUnit.DAYS), chatId);
        checkGroup("🟠 Qualificats inactius",
                List.of(PipelineStage.QUALIFIED), now.minus(5, ChronoUnit.DAYS), chatId);
        checkGroup("💤 NURTURING per revisar",
                List.of(PipelineStage.NURTURING), now.minus(30, ChronoUnit.DAYS), chatId);
    }

    private void checkSla(Long chatId, Instant now) {
        try {
            List<Lead> expired = leadRepository.findExpiredSlaLeads(now);
            if (expired.isEmpty()) return;

            StringBuilder sb = new StringBuilder("🚨 <b>SLA vençut</b>\n");
            for (Lead l : expired) {
                sb.append("• ").append(l.getName() != null ? l.getName() : "Sense nom");
                sb.append(" [").append(l.getStage() != null ? l.getStage().name() : "?").append("]");
                if (l.getPhone() != null) sb.append(" · ").append(l.getPhone());
                sb.append("\n");
                l.setSlaAlerted(true);
            }
            leadRepository.saveAll(expired);
            telegramBotClient.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            log.warn("[LeadInactivity] Error comprovant SLA: {}", e.getMessage());
        }
    }

    private void checkGroup(String title, List<PipelineStage> stages, Instant cutoff, Long chatId) {
        try {
            List<Lead> leads = leadRepository.findActiveByStageInAndLastContactBefore(stages, cutoff);
            if (leads.isEmpty()) return;

            StringBuilder sb = new StringBuilder("⚠️ <b>" + title + "</b>\n");
            for (Lead l : leads) {
                sb.append("• ").append(l.getName() != null ? l.getName() : "Sense nom");
                if (l.getPhone() != null) sb.append(" · ").append(l.getPhone());
                if (l.getLastContactAt() != null) {
                    long days = ChronoUnit.DAYS.between(l.getLastContactAt(), Instant.now());
                    sb.append(" · fa ").append(days).append("d");
                } else {
                    sb.append(" · sense contacte registrat");
                }
                sb.append("\n");
            }
            telegramBotClient.sendMessage(chatId, sb.toString());
        } catch (Exception e) {
            log.warn("[LeadInactivity] Error comprovant {}: {}", stages, e.getMessage());
        }
    }
}
