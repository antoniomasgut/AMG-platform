package com.amg.digitalitzacio.engine.api.dto;

import java.util.Map;

public record CreateVersionRequest(
    Map<String, Object> content,
    Map<String, Object> styles
) {}
