package com.amg.digitalitzacio.shared.ai;

import java.util.Map;

public record ToolDefinition(String name, String description, Map<String, Object> inputSchema) {}
