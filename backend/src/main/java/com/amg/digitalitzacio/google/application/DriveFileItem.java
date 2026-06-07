package com.amg.digitalitzacio.google.application;

import java.time.Instant;

public record DriveFileItem(
    String id,
    String name,
    String mimeType,
    long size,
    Instant createdTime,
    String webViewLink
) {
    public boolean isFolder() {
        return "application/vnd.google-apps.folder".equals(mimeType);
    }
}
