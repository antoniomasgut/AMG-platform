package com.amg.digitalitzacio.shared.api;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Slf4j
public class PublicContactController {

    private final EmailService emailService;
    private final LeadRepository leadRepository;
    private final SystemConfigService sysConfig;
    private final TelegramBotClient telegramBotClient;

    public record ContactRequest(String name, String email, String message) {}
    public record ContactResponse(boolean ok) {}

    @PostMapping
    public ResponseEntity<ContactResponse> submit(@RequestBody ContactRequest req) {
        if (req.name() == null || req.name().isBlank() ||
            req.email() == null || !req.email().contains("@") ||
            req.message() == null || req.message().isBlank()) {
            return ResponseEntity.badRequest().body(new ContactResponse(false));
        }
        if (req.name().length() > 100 || req.email().length() > 200 || req.message().length() > 2000) {
            return ResponseEntity.badRequest().body(new ContactResponse(false));
        }

        String name    = req.name().strip();
        String email   = req.email().strip().toLowerCase();
        String message = req.message().strip();

        // Crea Lead si hi ha PLATFORM_TENANT_ID configurat
        try {
            String platformTenantIdStr = sysConfig.get("PLATFORM_TENANT_ID");
            if (platformTenantIdStr != null && !platformTenantIdStr.isBlank()) {
                UUID platformTenantId = UUID.fromString(platformTenantIdStr);
                if (!leadRepository.existsByTenantIdAndEmail(platformTenantId, email)) {
                    var lead = new Lead();
                    lead.setTenantId(platformTenantId);
                    lead.setName(name);
                    lead.setEmail(email);
                    lead.setSource(LeadSource.WEB);
                    lead.setNotes("Missatge web: " + message);
                    leadRepository.save(lead);
                }
            }
        } catch (Exception e) {
            log.warn("Could not create lead from web form (non-fatal): {}", e.getMessage());
        }

        // Notificació Telegram directa
        try {
            String flag = sysConfig.get("AMG_NOTIFY_WEB_CONTACT");
            boolean enabled = flag == null || !"false".equalsIgnoreCase(flag.trim());
            String chatIdStr = sysConfig.get("AMG_SALES_CHAT_ID");
            if (enabled && chatIdStr != null && !chatIdStr.isBlank()) {
                Long chatId = Long.parseLong(chatIdStr.trim());
                String tgText = "📩 <b>Nova consulta web</b>\n\n" +
                        "<b>Nom:</b> " + escapeHtml(name) + "\n" +
                        "<b>Email:</b> " + escapeHtml(email) + "\n\n" +
                        escapeHtml(message);
                telegramBotClient.sendMessage(chatId, tgText);
            }
        } catch (Exception e) {
            log.warn("Telegram notification failed (non-fatal): {}", e.getMessage());
        }

        // Còpia per arxiu — a Gmail directament per evitar que Cloudflare el reprocessi com a email entrant
        try {
            String text = "Nova consulta des de amgdl.com\n\nNom: %s\nEmail: %s\n\nMissatge:\n%s"
                    .formatted(name, email, message);
            emailService.sendEmail("amgdigitalitzacions@gmail.com", "Consulta web: " + name, text);
        } catch (Exception e) {
            log.warn("Contact form email failed (non-fatal): {}", e.getMessage());
        }

        return ResponseEntity.ok(new ContactResponse(true));
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
