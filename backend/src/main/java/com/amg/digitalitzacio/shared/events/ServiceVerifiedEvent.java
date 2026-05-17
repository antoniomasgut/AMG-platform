package com.amg.digitalitzacio.shared.events;

import java.util.UUID;

public record ServiceVerifiedEvent(UUID tenantId, UUID serviceId, String serviceSlug) {
}
