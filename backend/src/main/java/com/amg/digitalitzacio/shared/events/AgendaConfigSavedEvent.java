package com.amg.digitalitzacio.shared.events;

import java.util.UUID;

/**
 * Publicat quan la config AGENDA es crea per primera vegada per a un tenant.
 * Permet auto-provisionar el Google Calendar via SA sense acció manual.
 */
public record AgendaConfigSavedEvent(UUID tenantId) {}
