package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateCalendarEventTool implements AgentTool {

    private final GoogleCalendarService googleCalendarService;
    private final NexeServiceConfigService nexeServiceConfigService;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String name() { return "create_calendar_event"; }

    @Override
    public String description() {
        return "Crea un event al Google Calendar del tenant. Cal data (yyyy-MM-dd), hora (HH:mm), títol i durada en minuts. Si passes clientPhone o clientEmail, avança automàticament l'etapa del lead al CRM.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "title",       Map.of("type", "string",  "description", "Títol de l'event"),
                "date",        Map.of("type", "string",  "description", "Data en format yyyy-MM-dd"),
                "time",        Map.of("type", "string",  "description", "Hora en format HH:mm"),
                "duration",    Map.of("type", "integer", "description", "Durada en minuts (per defecte 60)"),
                "notes",       Map.of("type", "string",  "description", "Descripció o notes opcionals"),
                "clientPhone", Map.of("type", "string",  "description", "Telèfon del client per actualitzar el CRM (opcional)"),
                "clientEmail", Map.of("type", "string",  "description", "Email del client per actualitzar el CRM (opcional)")
            ),
            "required", new String[]{"title", "date", "time"}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        try {
            String title       = String.valueOf(input.getOrDefault("title", "Cita"));
            String date        = String.valueOf(input.get("date"));
            String time        = String.valueOf(input.get("time"));
            int    duration    = input.get("duration") instanceof Number n ? n.intValue() : 60;
            String notes       = input.get("notes") instanceof String s ? s : "";
            String clientPhone = input.get("clientPhone") instanceof String s ? s.trim() : null;
            String clientEmail = input.get("clientEmail") instanceof String s ? s.trim() : null;

            var configOpt = nexeServiceConfigService.get(tenantId, "AGENDA");
            if (configOpt.isEmpty()) return "La configuració d'agenda no està disponible.";

            Map<String, Object> agenda = objectMapper.readValue(
                configOpt.get().getConfigJson(), new TypeReference<>() {});
            String calType = String.valueOf(agenda.get("calendar_type"));
            String calId   = (String) agenda.get("google_calendar_id");

            if (calId == null || calId.isBlank()) return "No hi ha calendari configurat.";

            LocalDateTime start = LocalDateTime.parse(date + "T" + time,
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

            if ("google_oauth".equals(calType)) {
                String refreshToken = (String) agenda.get("google_refresh_token");
                googleCalendarService.createEventOAuth(refreshToken, calId, title, start, duration, notes);
            } else {
                googleCalendarService.createEvent(calId, title, start, duration, notes);
            }

            // Avança l'etapa del lead al CRM si tenim referència del client
            advanceLeadStage(tenantId, clientPhone, clientEmail);

            return "Event creat correctament: '" + title + "' el " + date + " a les " + time + " (" + duration + " min).";
        } catch (Exception e) {
            log.warn("[CreateCalendarEventTool] Error: {}", e.getMessage());
            return "No s'ha pogut crear l'event: " + e.getMessage();
        }
    }

    private void advanceLeadStage(UUID tenantId, String phone, String email) {
        try {
            java.util.Optional<Lead> leadOpt;
            if (phone != null && !phone.isBlank()) {
                leadOpt = leadRepository.findFirstByTenantIdAndPhone(tenantId, phone);
            } else if (email != null && !email.isBlank()) {
                leadOpt = leadRepository.findFirstByTenantIdAndEmail(tenantId, email);
            } else {
                return;
            }
            leadOpt.ifPresent(lead -> {
                if (lead.getStage() == PipelineStage.NEW || lead.getStage() == PipelineStage.CONTACTED) {
                    lead.setStage(PipelineStage.QUALIFIED);
                    leadRepository.save(lead);
                    log.info("[CreateCalendarEventTool] Lead {} avançat a QUALIFIED (cita creada)", lead.getId());
                }
            });
        } catch (Exception e) {
            log.warn("[CreateCalendarEventTool] No s'ha pogut actualitzar l'etapa del lead: {}", e.getMessage());
        }
    }
}
