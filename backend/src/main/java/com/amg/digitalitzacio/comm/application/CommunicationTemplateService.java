package com.amg.digitalitzacio.comm.application;

import com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel;
import com.amg.digitalitzacio.auth.application.EmailService;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplateRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunicationTemplateService {

    private final CommunicationTemplateRepository repo;
    private final EmailService emailService;
    private final WhatsAppChannel whatsAppChannel;
    private final SystemConfigService systemConfigService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<CommunicationTemplate> listAll() {
        return repo.findAllByOrderBySortOrderAscActionAscChannelAsc();
    }

    public CommunicationTemplate create(CommunicationTemplate tpl) {
        if (repo.existsBySectorAndChannelAndActionAndLanguage(
                tpl.getSector(), tpl.getChannel(), tpl.getAction(), tpl.getLanguage())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Already exists: " + tpl.getSector() + "/" + tpl.getChannel() + "/" + tpl.getAction() + "/" + tpl.getLanguage());
        }
        return repo.save(tpl);
    }

    public CommunicationTemplate update(UUID id, CommunicationTemplate patch) {
        CommunicationTemplate tpl = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (patch.getSubject() != null) tpl.setSubject(patch.getSubject());
        if (patch.getBody() != null)    tpl.setBody(patch.getBody());
        if (patch.getIsActive() != null) tpl.setIsActive(patch.getIsActive());
        tpl.setSortOrder(patch.getSortOrder());
        return repo.save(tpl);
    }

    public void delete(UUID id) {
        repo.deleteById(id);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    /** Substitueix variables {KEY} pel valor del map. */
    public String render(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            result = result.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        return result;
    }

    // ── Resolve ───────────────────────────────────────────────────────────────

    /**
     * Busca plantilla: sector+lang → ALL+lang → ALL+"ca".
     * Llança 404 si no existeix cap.
     */
    public CommunicationTemplate resolve(String sector, String channel, String action, String language) {
        String lang = language != null && !language.isBlank() ? language : "ca";
        String sec  = sector  != null && !sector.isBlank()   ? sector  : "ALL";
        return repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(sec, channel, action, lang)
                .or(() -> repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue("ALL", channel, action, lang))
                .or(() -> repo.findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue("ALL", channel, action, "ca"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active template for " + sec + "/" + channel + "/" + action + "/" + lang));
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public String send(String channel, String to, String action, String sector, String language, Map<String, String> variables) {
        CommunicationTemplate tpl = resolve(sector, channel, action, language);
        String renderedBody    = render(tpl.getBody(), variables);
        String renderedSubject = tpl.getSubject() != null ? render(tpl.getSubject(), variables) : null;

        if ("EMAIL".equalsIgnoreCase(channel)) {
            emailService.sendEmail(to, renderedSubject != null ? renderedSubject : action, renderedBody);
        } else if ("WHATSAPP".equalsIgnoreCase(channel)) {
            String from = systemConfigService.get("TWILIO_WHATSAPP_FROM");
            if (from == null || from.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TWILIO_WHATSAPP_FROM not configured");
            }
            whatsAppChannel.sendMessage(from, to, renderedBody);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown channel: " + channel);
        }

        return renderedBody;
    }
}
