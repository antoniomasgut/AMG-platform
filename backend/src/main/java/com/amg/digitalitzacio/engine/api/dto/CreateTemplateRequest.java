package com.amg.digitalitzacio.engine.api.dto;

public record CreateTemplateRequest(
    String name,
    String slug,
    String description,
    String defaultStyles
) {}
