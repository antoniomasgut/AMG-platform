package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.ConversationRepository;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportScheduler {

    private static final ZoneId TZ = ZoneId.of("Europe/Madrid");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TenantRepository tenantRepository;
    private final NexeServiceConfigService nexeConfigService;
    private final ConversationRepository conversationRepository;
    private final LeadRepository leadRepository;
    private final TelegramBotClient telegramBotClient;
    private final ObjectMapper objectMapper;

    @Scheduled(cron = "0 0 20 * * *")
    public void sendDailyReports() {
        var tenants = tenantRepository.findByContractedPhasesContaining("F5");
        if (tenants.isEmpty()) return;

        log.info("[F5] Enviant informe diari per {} tenants", tenants.size());

        for (var tenant : tenants) {
            try {
                sendReport(tenant.getId(), tenant.getName());
            } catch (Exception e) {
                log.warn("[F5] Error enviant informe per tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }

    private void sendReport(UUID tenantId, String tenantName) throws Exception {
        var configOpt = nexeConfigService.get(tenantId, "EQUIP");
        if (configOpt.isEmpty()) return;

        Map<String, Object> config = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});

        Object groupIdRaw = config.get("telegram_group_id");
        if (groupIdRaw == null) return;

        long groupId;
        try {
            groupId = Long.parseLong(String.valueOf(groupIdRaw));
        } catch (NumberFormatException e) {
            log.warn("[F5] telegram_group_id invàlid per tenant {}: {}", tenantId, groupIdRaw);
            return;
        }

        Instant startOfDay = LocalDate.now(TZ).atStartOfDay(TZ).toInstant();
        long convsAvui   = conversationRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfDay);
        long leadsAvui   = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, startOfDay);
        long pendents    = conversationRepository.countByTenantIdAndPendingApprovalTrue(tenantId);

        String today = LocalDate.now(TZ).format(DATE_FMT);
        String report = "📊 Informe diari — " + tenantName + "\n" +
                "📅 " + today + "\n\n" +
                "💬 Converses avui: " + convsAvui + "\n" +
                "👤 Leads nous avui: " + leadsAvui + "\n" +
                "⏳ Respostes pendents: " + pendents + "\n\n" +
                (pendents > 0 ? "⚠️ Tens respostes pendents d'aprovació al portal.\n" : "✅ Tot respost.\n");

        telegramBotClient.sendMessage(groupId, report);
        log.info("[F5] Informe diari enviat a tenant {} (grup {})", tenantId, groupId);
    }
}
