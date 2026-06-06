package com.amg.digitalitzacio.google.api.dto;

public record ModuleConfigRequest(
    boolean driveEnabled,
    boolean gmailEnabled,
    boolean calendarEnabled,
    boolean sheetsEnabled
) {}
