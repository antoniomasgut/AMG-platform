package com.amg.digitalitzacio.engine.api.dto;

public record TemplateSectionRequest(
    String blockType,
    Integer sortOrder,
    String propsSchema,
    String defaultProps
) {}
