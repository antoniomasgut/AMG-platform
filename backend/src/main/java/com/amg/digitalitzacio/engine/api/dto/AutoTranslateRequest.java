package com.amg.digitalitzacio.engine.api.dto;

import java.util.List;

public record AutoTranslateRequest(
    String sourceLocale,
    List<String> targetLocales
) {}
