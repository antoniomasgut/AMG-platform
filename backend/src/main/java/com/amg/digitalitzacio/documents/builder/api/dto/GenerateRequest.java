package com.amg.digitalitzacio.documents.builder.api.dto;

import java.util.Map;
import java.util.UUID;

public record GenerateRequest(
    UUID templateId,
    UUID customerId,
    Map<String, String> customerData,
    Map<String, Object> variables,
    java.util.List<ArticleLine> articles
) {
    public record ArticleLine(String description, Integer quantity, Double unitPrice) {}
}
