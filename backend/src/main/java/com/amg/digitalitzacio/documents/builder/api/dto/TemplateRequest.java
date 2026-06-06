package com.amg.digitalitzacio.documents.builder.api.dto;

import com.amg.digitalitzacio.documents.builder.domain.DocumentType;

public record TemplateRequest(
    String name,
    DocumentType documentType,
    String layout,
    String dataBindings,
    String styles
) {}
