package com.amg.digitalitzacio.engine.api.dto;

import java.util.UUID;

public record LandingTemplateSectionView(
    UUID id,
    String blockType,
    int sortOrder,
    String propsSchema,
    String defaultProps
) {}
