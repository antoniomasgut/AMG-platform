package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.agents.application.DemoInboxService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera automàticament una demo personalitzada per a prospects amb tier DEMO (61-80)
 * o PRIORITY (81+), segons Spec 12 §5.4/§7. Crea una DemoSession amb la landing del
 * sector del prospect i desa la URL a Prospect.demoUrl.
 * Per a PRIORITY, notifica el SUPER_ADMIN per Telegram.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectDemoGeneratorService {

    private static final int DEFAULT_TTL_DAYS = 7;

    private final DemoInboxService demoInboxService;
    private final ProspectRepository prospectRepository;
    private final TelegramBotClient telegramBotClient;
    private final SystemConfigService sysConfig;

    @Value("${app.api.base-url:https://api.amgdl.com}")
    private String apiBaseUrl;

    /**
     * Genera una demo de forma síncrona (per a invocació manual des d'endpoint).
     * No comprova el tier; el caller és responsable de validar l'elegibilitat.
     */
    @Transactional
    public void generateSync(Prospect prospect) {
        doGenerate(prospect);
    }

    /**
     * Genera una demo per a un prospect si és DEMO o PRIORITY i encara no en té.
     * S'invoca asíncronament des del flux d'anàlisi (analyzeWeb / generateAiReport).
     */
    @Async
    @Transactional
    public void generateIfEligible(Prospect prospect) {
        if (prospect.getDemoUrl() != null && !prospect.getDemoUrl().isBlank()) {
            log.debug("Prospect {} ja té demo URL, s'omet", prospect.getId());
            return;
        }
        String tier = prospect.getProspectTier();
        if (!"PRIORITY".equals(tier) && !"DEMO".equals(tier)) {
            return;
        }
        doGenerate(prospect);
        if ("PRIORITY".equals(tier)) {
            notifyPriority(prospect);
        }
    }

    private void doGenerate(Prospect prospect) {
        if (prospect.getDemoUrl() != null && !prospect.getDemoUrl().isBlank()) return;
        try {
            String sector = normalizeSector(prospect.getSector());
            String companyName = prospect.getName();
            String email = prospect.getEmail() != null
                ? prospect.getEmail() : "demo@amgdl.com";

            String agentContext = buildAgentContext(prospect);

            var session = demoInboxService.createSession(
                email, companyName, agentContext, sector, "ca", ttlDays() * 24);

            String demoUrl = apiBaseUrl + "/api/v1/engine/render/demo-" + session.getToken() + "/ca";
            prospect.setDemoUrl(demoUrl);
            prospectRepository.save(prospect);

            log.info("Demo generada per prospect {} ({}): {}", prospect.getId(), companyName, demoUrl);
        } catch (Exception e) {
            log.warn("Error generant demo per prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    private int ttlDays() {
        try {
            String v = sysConfig.get("PROSPECTING_DEMO_TTL_DAYS");
            if (v != null && !v.isBlank()) return Integer.parseInt(v.trim());
        } catch (Exception ignored) {}
        return DEFAULT_TTL_DAYS;
    }

    private void notifyPriority(Prospect prospect) {
        try {
            String chatIdStr = sysConfig.get("TELEGRAM_CHAT_ID");
            if (chatIdStr == null || chatIdStr.isBlank()) return;

            String text = String.format(
                "🔥 <b>Prospect PRIORITY</b> — %s\n\n" +
                "Score: %d | Sector: %s | Municipi: %s\n" +
                "%s%s",
                escapeHtml(prospect.getName()),
                prospect.getScore() != null ? prospect.getScore() : 0,
                prospect.getSector() != null ? prospect.getSector() : "—",
                prospect.getCity() != null ? prospect.getCity() : "—",
                prospect.getPhone() != null ? "Tel: " + prospect.getPhone() + "\n" : "",
                prospect.getDemoUrl() != null ? "Demo: " + prospect.getDemoUrl() : "");
            telegramBotClient.sendMessage(Long.parseLong(chatIdStr.trim()), text);
        } catch (Exception e) {
            log.warn("Error notificant PRIORITY per prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String normalizeSector(String raw) {
        if (raw == null || raw.isBlank()) return "DEFAULT";
        return raw.toUpperCase().replace(" ", "_").replace("-", "_");
    }

    private String buildAgentContext(Prospect prospect) {
        var sb = new StringBuilder();
        sb.append("Prospect de qualitat alta (tier ").append(prospect.getProspectTier()).append("). ");
        if (prospect.getWebsite() != null) {
            sb.append("Web actual: ").append(prospect.getWebsite()).append(". ");
        }
        if (prospect.getAiPitch() != null && !prospect.getAiPitch().isBlank()) {
            sb.append("Context: ").append(prospect.getAiPitch());
        }
        return sb.toString();
    }
}
