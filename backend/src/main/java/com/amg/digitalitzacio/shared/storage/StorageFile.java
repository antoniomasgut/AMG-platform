package com.amg.digitalitzacio.shared.storage;

public record StorageFile(String fileId, String fileName, String mimeType, long size, String provider) {}
