package com.amg.digitalitzacio.shared.storage;

import java.time.Instant;

public record FileMetadata(String fileId, String fileName, String mimeType, long size, Instant createdAt, String etag) {}
