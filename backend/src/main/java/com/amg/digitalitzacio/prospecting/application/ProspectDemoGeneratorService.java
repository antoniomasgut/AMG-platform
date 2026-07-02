package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.agents.application.DemoInboxService;
import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera automàticament una demo personalitzada per a prospects PRIORITY (score ≥ 81).
 * Crea una DemoSession amb la landing del sector del prospect i desa la URL a Prospect.demoUrl.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectDemoGeneratorService {

    private final DemoInboxService demoInboxService;
    private final ProspectRepository prospectRepository;

    @Value("${app.base-url:https://amgdl.com}")
    private String baseUrl;

    /**
     * Genera una demo de forma síncrona (per a invocació manual des d'endpoint).
     * No comprova el tier; el caller és responsable de validar l'elegibilitat.
     */
    @Transactional
    public void generateSync(Prospect prospect) {
        doGenerate(prospect);
    }

    /**
     * Genera una demo per a un prospect si és PRIORITY i encara no en té.
     * S'invoca asíncronament des de ProspectAnalysisService.
     */
    @Async
    @Transactional
    public void generateIfEligible(Prospect prospect) {
        if (prospect.getDemoUrl() != null && !prospect.getDemoUrl().isBlank()) {
            log.debug("Prospect {} ja té demo URL, s'omet", prospect.getId());
            return;
        }
        if (!"PRIORITY".equals(prospect.getProspectTier())) {
            return;
        }
        doGenerate(prospect);
    }

    private void doGenerate(Prospect prospect) {
        if (prospect.getDemoUrl() != null && !prospect.getDemoUrl().isBlank()) return;
        try {
            String sector = normalizeSector(prospect.getSector());
            String companyName = prospect.getName();
            String email = prospect.getEmail() != null
                ? prospect.getEmail() : "demo@amgdl.com";

            String agentContext = buildAgentContext(prospect);

            var session = demoInboxService.createSession(email, companyName, agentContext, sector, "ca");

            String demoUrl = baseUrl + "/demo/inbox/" + session.getToken();
            prospect.setDemoUrl(demoUrl);
            prospectRepository.save(prospect);

            log.info("Demo generada per prospect {} ({}): {}", prospect.getId(), companyName, demoUrl);
        } catch (Exception e) {
            log.warn("Error generant demo per prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    private String normalizeSector(String raw) {
        if (raw == null || raw.isBlank()) return "DEFAULT";
        return raw.toUpperCase().replace(" ", "_").replace("-", "_");
    }

    private String buildAgentContext(Prospect prospect) {
        var sb = new StringBuilder();
        sb.append("Prospect de qualitat alta (tier PRIORITY). ");
        if (prospect.getWebsite() != null) {
            sb.append("Web actual: ").append(prospect.getWebsite()).append(". ");
        }
        if (prospect.getAiPitch() != null && !prospect.getAiPitch().isBlank()) {
            sb.append("Context: ").append(prospect.getAiPitch());
        }
        return sb.toString();
    }
}
