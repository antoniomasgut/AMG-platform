package com.amg.digitalitzacio.google.api.dto;

public record GoogleStatusResponse(
    boolean connected,
    String email,
    boolean driveEnabled,
    boolean gmailEnabled,
    boolean calendarEnabled,
    boolean sheetsEnabled,
    boolean businessEnabled,
    String businessLocationId,
    String driveFolderId
) {}
