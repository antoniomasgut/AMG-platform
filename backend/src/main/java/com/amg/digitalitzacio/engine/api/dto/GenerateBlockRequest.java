package com.amg.digitalitzacio.engine.api.dto;

public record GenerateBlockRequest(
    String blockType,
    String businessName,
    String sector,
    String language,
    String model
) {}
