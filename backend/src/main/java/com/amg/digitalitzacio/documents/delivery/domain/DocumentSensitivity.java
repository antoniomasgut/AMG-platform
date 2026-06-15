package com.amg.digitalitzacio.documents.delivery.domain;

import java.time.Duration;

public enum DocumentSensitivity {
    STANDARD(Duration.ofDays(7), null),
    SENSITIVE(Duration.ofHours(72), 1);

    public final Duration ttl;
    public final Integer maxDownloads;

    DocumentSensitivity(Duration ttl, Integer maxDownloads) {
        this.ttl = ttl;
        this.maxDownloads = maxDownloads;
    }
}
