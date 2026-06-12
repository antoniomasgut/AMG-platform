package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.shared.events.AgendaConfigSavedEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Escolta AgendaConfigSavedEvent i crea automàticament un Google Calendar
 * per al tenant via Service Account, sense que l'ADMIN hagi de fer res.
 *
 * Condicions per activar-se:
 *   1. GOOGLE_CALENDAR_SA_JSON configurat a system_settings
 *   2. La config AGENDA no té ja un google_calendar_id
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalendarAutoProvisionListener {

    private final GoogleCalendarService calendarService;
    private final NexeServiceConfigService nexeConfigService;
    private final TenantRepository tenantRepository;
    private final ObjectMapper objectMapper;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendaConfigSaved(AgendaConfigSavedEvent event) {
        var tenantId = event.tenantId();

        if (!calendarService.isConfigured()) {
            log.debug("[CalendarAutoProvision] SA no configurada — omitint auto-provisió per tenant {}", tenantId);
            return;
        }

        try {
            var existing = nexeConfigService.get(tenantId, "AGENDA");
            if (existing.isPresent()) {
                Map<String, Object> cfg = objectMapper.readValue(
                        existing.get().getConfigJson(), new TypeReference<>() {});
                if (cfg.containsKey("google_calendar_id")) {
                    log.debug("[CalendarAutoProvision] Tenant {} ja té calendari: {}", tenantId, cfg.get("google_calendar_id"));
                    return;
                }
            }

            var tenant = tenantRepository.findById(tenantId).orElse(null);
            if (tenant == null) {
                log.warn("[CalendarAutoProvision] Tenant {} no trobat", tenantId);
                return;
            }

            String calendarName = "Cites — " + tenant.getName();
            String calendarId = calendarService.createCalendarForTenant(calendarName, tenant.getEmail());

            // Actualitza la config AGENDA amb el calendarId
            String currentJson = nexeConfigService.get(tenantId, "AGENDA")
                    .map(c -> c.getConfigJson()).orElse("{}");
            Map<String, Object> cfg = new HashMap<>(
                    objectMapper.readValue(currentJson, new TypeReference<>() {}));
            cfg.put("calendar_type", "google");
            cfg.put("google_calendar_id", calendarId);
            nexeConfigService.save(tenantId, "AGENDA", objectMapper.writeValueAsString(cfg));

            log.info("[CalendarAutoProvision] Calendari creat i configurat per tenant {} ({}): {}",
                    tenant.getName(), tenantId, calendarId);

        } catch (Exception e) {
            log.error("[CalendarAutoProvision] Error auto-provisionant calendari per tenant {}: {}",
                    tenantId, e.getMessage(), e);
        }
    }
}
