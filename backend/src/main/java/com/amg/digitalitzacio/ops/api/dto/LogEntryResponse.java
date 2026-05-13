package com.amg.digitalitzacio.ops.api.dto;

public record LogEntryResponse(String timestamp, String level, String message, String service) {}
