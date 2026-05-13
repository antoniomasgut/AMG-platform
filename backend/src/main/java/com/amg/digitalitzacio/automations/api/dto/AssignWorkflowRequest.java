package com.amg.digitalitzacio.automations.api.dto;

import java.util.Map;

public record AssignWorkflowRequest(String templateKey, Map<String, String> config) {}
