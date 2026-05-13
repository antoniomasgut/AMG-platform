package com.amg.digitalitzacio.engine.api.dto;

public record ContactRequest(
    String name,
    String email,
    String phone,
    String message,
    Boolean consent
) {}
