package com.amg.digitalitzacio.documents.builder.api.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AIApplyRequest(
    String prompt,
    UUID templateId
) {
    public record AIOperation(
        String action,
        String blockId,
        Integer x, Integer y, Integer w, Integer h,
        Map<String, Object> style
    ) {}
    public record AIApplyResponse(
        List<AIOperation> operations
    ) {}
}
