package com.amg.digitalitzacio.documents.delivery.api.dto;

import com.amg.digitalitzacio.documents.delivery.domain.SecureDocumentToken;

import java.time.Instant;
import java.util.UUID;

public record TokenResponse(
    UUID id,
    String fileName,
    String sensitivity,
    String recipientEmail,
    String recipientName,
    String description,
    Instant expiresAt,
    Integer maxDownloads,
    int downloadCount,
    Instant firstDownloadedAt,
    Instant lastDownloadedAt,
    Instant emailSentAt,
    boolean revoked,
    Instant revokedAt,
    Instant createdAt,
    String status,
    String downloadUrl
) {
    public static TokenResponse from(SecureDocumentToken t, String baseUrl) {
        String status = t.isRevoked() ? "REVOCAT"
            : t.isExpired() ? "EXPIRAT"
            : t.isDownloadsExhausted() ? "ESGOTAT"
            : "ACTIU";
        String url = baseUrl + "/api/v1/documents/download/" + t.getToken();
        return new TokenResponse(
            t.getId(), t.getFileName(), t.getSensitivity().name(),
            t.getRecipientEmail(), t.getRecipientName(), t.getDescription(),
            t.getExpiresAt(), t.getMaxDownloads(), t.getDownloadCount(),
            t.getFirstDownloadedAt(), t.getLastDownloadedAt(), t.getEmailSentAt(),
            t.isRevoked(), t.getRevokedAt(), t.getCreatedAt(), status, url
        );
    }
}
