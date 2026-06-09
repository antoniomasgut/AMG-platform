package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.agents.application.GoogleCalendarService;
import com.amg.digitalitzacio.agents.application.NexeServiceConfigService;
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
    private final ObjectMapper objectMapper;

    @Override
    public String name() { return "create_calendar_event"; }

    @Override
    public String description() {
        return "Crea un event al Google Calendar del tenant. Cal data (yyyy-MM-dd), hora (HH:mm), títol i durada en minuts.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "title",    Map.of("type", "string",  "description", "Títol de l'event"),
                "date",     Map.of("type", "string",  "description", "Data en format yyyy-MM-dd"),
                "time",     Map.of("type", "string",  "description", "Hora en format HH:mm"),
                "duration", Map.of("type", "integer", "description", "Durada en minuts (per defecte 60)"),
                "notes",    Map.of("type", "string",  "description", "Descripció o notes opcionals")
            ),
            "required", new String[]{"title", "date", "time"}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        try {
            String title    = String.valueOf(input.getOrDefault("title", "Cita"));
            String date     = String.valueOf(input.get("date"));
            String time     = String.valueOf(input.get("time"));
            int    duration = input.get("duration") instanceof Number n ? n.intValue() : 60;
            String notes    = input.get("notes") instanceof String s ? s : "";

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
            return "Event creat correctament: '" + title + "' el " + date + " a les " + time + " (" + duration + " min).";
        } catch (Exception e) {
            log.warn("[CreateCalendarEventTool] Error: {}", e.getMessage());
            return "No s'ha pogut crear l'event: " + e.getMessage();
        }
    }
}
