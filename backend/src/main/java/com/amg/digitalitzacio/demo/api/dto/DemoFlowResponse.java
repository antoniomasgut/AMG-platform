package com.amg.digitalitzacio.demo.api.dto;

import java.util.List;

public record DemoFlowResponse(String id, String title, String description, List<DemoStepResponse> steps) {}
