package com.amg.digitalitzacio.demo.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.FollowupLogRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DemoFollowUpScheduler {

    private static final Set<PipelineStage> PENDING_STAGES = Set.of(PipelineStage.NEW, PipelineStage.CONTACTED);

    private final FollowupLogRepository followupLogRepository;
    private final LeadRepository leadRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    @Scheduled(cron = "0 15 10 * * *")
    public void checkPendingDemos() {
        Instant now = Instant.now();
        var logs = followupLogRepository.findByTypeAndSentAtBetween(
                "DEMO_SENT",
                now.minus(50, ChronoUnit.HOURS),
                now.minus(46, ChronoUnit.HOURS));

        if (logs.isEmpty()) return;

        String chatIdStr = sysConfig.get("AMG_SALES_CHAT_ID");
        if (chatIdStr == null || chatIdStr.isBlank()) return;

        long chatId;
        try {
            chatId = Long.parseLong(chatIdStr.trim());
        } catch (NumberFormatException e) {
            log.warn("AMG_SALES_CHAT_ID no és vàlid: {}", chatIdStr);
            return;
        }

        List<String> lines = logs.stream()
                .map(fl -> {
                    Lead lead = leadRepository.findById(fl.getEntityId()).orElse(null);
                    if (lead == null || !PENDING_STAGES.contains(lead.getStage())) return null;
                    String name = lead.getName() != null ? lead.getName() : "—";
                    String contact = fl.getContact() != null ? fl.getContact() : "—";
                    String stage = lead.getStage() != null ? lead.getStage().name() : "—";
                    return "• <b>" + name + "</b> · " + contact + " · " + stage;
                })
                .filter(line -> line != null)
                .collect(Collectors.toList());

        if (lines.isEmpty()) return;

        String msg = "📬 <b>Seguiment demos enviades fa ~48h</b>\n"
                + "<i>" + lines.size() + " lead(s) sense resposta</i>\n\n"
                + String.join("\n", lines);

        telegramBotClient.sendMessage(chatId, msg);
        log.info("DemoFollowUp: digest enviat per {} leads", lines.size());
    }
}
