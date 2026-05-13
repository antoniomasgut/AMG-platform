package com.amg.digitalitzacio.automations.api.dto;

public record HealthResponse(boolean n8nConnected, String n8nVersion, int activeWorkflows, int pendingExecutions) {}
