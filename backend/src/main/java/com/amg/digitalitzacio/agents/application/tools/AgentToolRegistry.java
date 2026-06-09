package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.shared.ai.ToolDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentToolRegistry {

    private final List<AgentTool> tools;

    public List<ToolDefinition> definitions() {
        return tools.stream()
            .map(t -> new ToolDefinition(t.name(), t.description(), t.inputSchema()))
            .toList();
    }

    /** Executa una eina per nom per a un tenant concret. */
    public String execute(UUID tenantId, String toolName, Map<String, Object> input) {
        return tools.stream()
            .filter(t -> t.name().equals(toolName))
            .findFirst()
            .map(t -> t.execute(tenantId, input))
            .orElse("Eina desconeguda: " + toolName);
    }

    /** Funció curried per passar al provider com a executor. */
    public Function<com.amg.digitalitzacio.shared.ai.ToolCall, String> executorFor(UUID tenantId) {
        return call -> execute(tenantId, call.name(), call.input());
    }
}
