package com.amg.digitalitzacio.shared.ai;

import java.util.Map;

public record ToolCall(String toolUseId, String name, Map<String, Object> input) {}
