package com.amg.digitalitzacio.documents.delivery.domain;

public enum AuditEventType {
    EMAIL_SENT,
    DOWNLOAD_OK,
    DOWNLOAD_EXPIRED,
    DOWNLOAD_EXHAUSTED,
    DOWNLOAD_REVOKED,
    DOWNLOAD_NOT_FOUND,
    DOWNLOAD_ERROR,
    DOCUMENT_VIEWED,
    DOCUMENT_ACCEPTED
}
