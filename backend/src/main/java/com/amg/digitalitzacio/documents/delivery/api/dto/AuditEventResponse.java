package com.amg.digitalitzacio.documents.delivery.api.dto;

import com.amg.digitalitzacio.documents.delivery.domain.SecureDocumentAudit;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    String eventType,
    String ipAddress,
    String userAgent,
    Instant occurredAt
) {
    public static AuditEventResponse from(SecureDocumentAudit a) {
        return new AuditEventResponse(
            a.getId(), a.getEventType().name(),
            a.getIpAddress(), a.getUserAgent(), a.getOccurredAt()
        );
    }
}
