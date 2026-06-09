package com.amg.digitalitzacio.agents.application.tools;

import java.util.Map;
import java.util.UUID;

public interface AgentTool {
    String name();
    String description();
    Map<String, Object> inputSchema();
    String execute(UUID tenantId, Map<String, Object> input);
}
