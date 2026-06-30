package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Genera un informe estructurat de la situació digital del prospect via IA,
 * i un missatge de prospecció hiper-personalitzat.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectAnalysisService {

    private final AIProviderRouter aiProviderRouter;

    private static final String REPORT_SYSTEM = """
        Ets un consultor expert en digitalització de negocis locals a Mallorca.
        Analitza les dades d'un negoci i genera un informe JSON estructurat.
        Respon ÚNICAMENT amb el JSON, sense cap text addicional, sense markdown, sense ```json.
        """;

    private static final String PITCH_SYSTEM = """
        Ets un agent comercial de AMG Digitalitzacions (Mallorca).
        Escriu un missatge de prospecció en CATALÀ, breu (màx 4 paràgrafs), professional i proper.
        Menciona DADES CONCRETES del negoci (nom, rating, carències detectades).
        MAI siguis genèric. Acaba amb un CTA clar (demo, trucada, reunió).
        Retorna ÚNICAMENT el text del missatge, sense cap nota ni metadada.
        """;

    public void generateReport(Prospect prospect) {
        try {
            var prompt = buildReportPrompt(prospect);
            var aiProvider = aiProviderRouter.forModel(aiProviderRouter.defaultModel());
            var report = aiProvider.chat(REPORT_SYSTEM, List.of(), prompt);
            prospect.setAiReport(report);
            prospect.setAiAnalyzedAt(Instant.now());
            log.debug("AI report generated for prospect {}", prospect.getId());
        } catch (Exception e) {
            log.warn("AI report failed for prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    public void generatePitch(Prospect prospect) {
        try {
            var prompt = buildPitchPrompt(prospect);
            var aiProvider = aiProviderRouter.forModel(aiProviderRouter.defaultModel());
            var pitch = aiProvider.chat(PITCH_SYSTEM, List.of(), prompt);
            prospect.setAiPitch(pitch);
            if (prospect.getAiAnalyzedAt() == null) prospect.setAiAnalyzedAt(Instant.now());
            log.debug("AI pitch generated for prospect {}", prospect.getId());
        } catch (Exception e) {
            log.warn("AI pitch failed for prospect {}: {}", prospect.getId(), e.getMessage());
        }
    }

    private String buildReportPrompt(Prospect p) {
        return String.format("""
            Negoci: %s
            Sector: %s | Municipi: %s
            Rating Google: %s (%s ressenyes)
            Té web: %s | SSL: %s | Responsive: %s | Velocitat: %sms
            CMS: %s | Tech stack: %s
            Analytics: %s | GTM: %s | Meta Pixel: %s
            Chat widget: %s | Booking: %s | Formulari: %s | FAQ: %s | CTA clara: %s
            WhatsApp: %s | Instagram: %s | Facebook: %s | LinkedIn: %s | TikTok: %s

            Genera un JSON amb exactament aquest format:
            {
              "digitalProblems": ["problema 1", "problema 2"],
              "automationOpportunities": ["oportunitat 1", "oportunitat 2"],
              "manualProcesses": ["procés 1", "procés 2"],
              "channelsUsed": ["canal 1", "canal 2"],
              "commercialOpportunities": ["oportunitat 1"],
              "recommendedDemo": "descripció breu del tipus de demo més adequat",
              "estimatedPhases": ["F1", "F2"],
              "pitchAngle": "frase concisa de l'angle de pitch principal"
            }
            """,
            p.getName(),
            p.getSector() != null ? p.getSector() : "desconegut",
            p.getCity() != null ? p.getCity() : "Mallorca",
            p.getGoogleRating() != null ? p.getGoogleRating() + "★" : "?",
            p.getGoogleReviews() != null ? p.getGoogleReviews() : 0,
            bool(p.getHasWebsite()), bool(p.getHasSsl()), bool(p.getIsResponsive()),
            p.getWebLoadMs() != null ? p.getWebLoadMs() : "?",
            p.getCmsDetected() != null ? p.getCmsDetected() : "desconegut",
            p.getTechStack() != null ? p.getTechStack() : "cap",
            bool(p.getHasAnalytics()), bool(p.getHasGtm()), bool(p.getHasPixel()),
            bool(p.getHasChatWidget()), bool(p.getHasBookingSystem()),
            bool(p.getHasContactForm()), bool(p.getHasFaq()), bool(p.getHasClearCta()),
            bool(p.getHasWhatsapp()), bool(p.getHasInstagram()),
            bool(p.getHasFacebook()), bool(p.getHasLinkedin()), bool(p.getHasTiktok()));
    }

    private String buildPitchPrompt(Prospect p) {
        var sb = new StringBuilder();
        sb.append("Negoci: ").append(p.getName()).append("\n");
        sb.append("Sector: ").append(p.getSector()).append(" a ").append(p.getCity()).append("\n");
        if (p.getGoogleRating() != null)
            sb.append("Rating: ").append(p.getGoogleRating()).append("★ (").append(p.getGoogleReviews()).append(" ressenyes)\n");
        if (p.getAiReport() != null)
            sb.append("Informe de diagnòstic: ").append(p.getAiReport()).append("\n");
        else {
            sb.append("Carències detectades:\n");
            if (!Boolean.TRUE.equals(p.getHasWhatsapp())) sb.append("- Sense WhatsApp Business\n");
            if (!Boolean.TRUE.equals(p.getHasChatWidget())) sb.append("- Sense chat a la web\n");
            if (!Boolean.TRUE.equals(p.getHasBookingSystem())) sb.append("- Sense sistema de reserves\n");
            if (!Boolean.TRUE.equals(p.getHasAnalytics())) sb.append("- Sense analítiques web\n");
            if (p.getWebLoadMs() != null && p.getWebLoadMs() > 3000) sb.append("- Web lenta (" + p.getWebLoadMs() + "ms)\n");
        }
        if (p.getDemoUrl() != null)
            sb.append("URL demo generada: ").append(p.getDemoUrl()).append("\n");
        return sb.toString();
    }

    private String bool(Boolean b) {
        if (b == null) return "?";
        return b ? "Sí" : "No";
    }
}
