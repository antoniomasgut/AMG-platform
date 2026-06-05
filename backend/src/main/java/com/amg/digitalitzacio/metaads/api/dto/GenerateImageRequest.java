package com.amg.digitalitzacio.metaads.api.dto;

public record GenerateImageRequest(
    String prompt,
    String format,
    String style
) {}
