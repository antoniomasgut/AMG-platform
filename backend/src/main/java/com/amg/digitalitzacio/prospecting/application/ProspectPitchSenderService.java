package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.prospecting.domain.Prospect;
import com.amg.digitalitzacio.prospecting.domain.ProspectRepository;
import com.amg.digitalitzacio.prospecting.domain.ProspectStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Envia el pitch personalitzat (aiPitch) pel millor canal disponible del prospect.
 * Fase 1: només email. WhatsApp requereix template HSM aprovat per Meta.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProspectPitchSenderService {

    private final ProspectRepository prospectRepository;
    private final ProspectChannelDetectorService channelDetector;
    private final EmailService emailService;

    public record SendResult(String channel, String target, String status, String reason) {}

    @Transactional
    public SendResult send(Prospect prospect) {
        if (prospect.getAiPitch() == null || prospect.getAiPitch().isBlank()) {
            return new SendResult("NONE", null, "SKIPPED", "Pitch no generat — executa generate-pitch primer");
        }
        if (prospect.getPitchSentAt() != null) {
            return new SendResult("NONE", null, "SKIPPED", "Pitch ja enviat el " + prospect.getPitchSentAt());
        }

        var channel = channelDetector.detect(prospect);

        return switch (channel.channel()) {
            case "EMAIL" -> sendEmail(prospect, channel.target());
            case "UNKNOWN" -> new SendResult("NONE", null, "SKIPPED", "Cap canal disponible — enriquiment necessari");
            default -> new SendResult(channel.channel(), channel.target(), "PENDING",
                    "Canal " + channel.channel() + " detectat però no automatitzat — acció manual");
        };
    }

    private SendResult sendEmail(Prospect prospect, String email) {
        try {
            String subject = "Una proposta per a " + prospect.getName() + " — AMG Digitalització";
            String html = buildHtmlEmail(prospect);
            emailService.sendEmailHtml(email, subject, html);

            prospect.setPitchSentAt(Instant.now());
            prospect.setStatus(ProspectStatus.CONTACTED);
            prospectRepository.save(prospect);

            log.info("Pitch enviat per email a {} (prospect {})", email, prospect.getId());
            return new SendResult("EMAIL", email, "SENT", "Email enviat correctament");
        } catch (Exception e) {
            log.error("Error enviant pitch per email a {}: {}", email, e.getMessage());
            return new SendResult("EMAIL", email, "ERROR", e.getMessage());
        }
    }

    private String buildHtmlEmail(Prospect p) {
        String pitchHtml = p.getAiPitch()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        String demoButton = "";
        if (p.getDemoUrl() != null && !p.getDemoUrl().isBlank()) {
            demoButton = """
                    <div style="margin:28px 0;text-align:center">
                      <a href="%s" style="background:#1a1a2e;color:#fff;padding:13px 32px;border-radius:6px;text-decoration:none;font-weight:bold;font-size:15px">
                        Veure la demo preparada per a vostè
                      </a>
                    </div>
                    """.formatted(p.getDemoUrl());
        }

        return """
                <html><body style="font-family:Arial,sans-serif;max-width:600px;margin:auto;padding:28px 20px;color:#222;line-height:1.6">
                <p>%s</p>
                %s
                <hr style="border:none;border-top:1px solid #e5e5e5;margin:28px 0">
                <p style="font-size:12px;color:#888">
                  AMG Digitalització · Toni Mas<br>
                  <a href="https://amgdl.com" style="color:#888">amgdl.com</a> · +34 614 492 062 · info@amgdl.com
                </p>
                <img src="https://api.amgdl.com/api/v1/prospecting/track/%s/open" width="1" height="1" style="display:none" alt="">
                </body></html>
                """.formatted(pitchHtml, demoButton, p.getId());
    }
}
