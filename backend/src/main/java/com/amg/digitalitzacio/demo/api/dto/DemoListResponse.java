package com.amg.digitalitzacio.demo.api.dto;

import java.util.List;

public record DemoListResponse(List<DemoFlowSummary> demos) {
    public record DemoFlowSummary(String id, String title, String description) {}
}
