package com.amg.digitalitzacio.engine.api.dto;

import java.util.Map;

public record GenerateBlockResponse(
    Map<String, Object> props,
    String model
) {}
