package com.amg.digitalitzacio.shared.storage.api.dto;

public record StorageStatusResponse(
    String activeProvider,
    boolean connected,
    String testResult
) {}
