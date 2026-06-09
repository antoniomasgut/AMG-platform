package com.amg.digitalitzacio.shared.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class AnthropicAIProvider implements AIProvider {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AnthropicAIProvider(String apiKey, String model, RestClient.Builder builder, ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = builder.requestFactory(factory).baseUrl(BASE_URL).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {
        var messages = new java.util.ArrayList<Map<String, String>>();
        for (var msg : history) {
            messages.add(Map.of("role", mapRole(msg.role()), "content", msg.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        var body = Map.of(
            "model", model,
            "max_tokens", 1024,
            "system", systemPrompt,
            "messages", messages
        );

        try {
            var raw = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            var resp = objectMapper.readTree(raw);
            return resp.path("content").path(0).path("text").asText("");
        } catch (Exception e) {
            log.error("Anthropic API error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String chatWithTools(String systemPrompt, List<ChatMessage> history, String userMessage,
                                 List<ToolDefinition> tools, Function<ToolCall, String> toolExecutor) {
        if (tools == null || tools.isEmpty()) {
            return chat(systemPrompt, history, userMessage);
        }

        var messages = new ArrayList<Map<String, Object>>();
        for (var msg : history) {
            messages.add(Map.of("role", mapRole(msg.role()), "content", msg.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        var toolDefs = tools.stream().map(t -> Map.of(
            "name", (Object) t.name(),
            "description", t.description(),
            "input_schema", t.inputSchema()
        )).toList();

        try {
            for (int iteration = 0; iteration < 3; iteration++) {
                var body = new HashMap<String, Object>();
                body.put("model", model);
                body.put("max_tokens", 1024);
                body.put("system", systemPrompt);
                body.put("messages", messages);
                body.put("tools", toolDefs);

                var raw = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", API_VERSION)
                    .header("Content-Type", "application/json")
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);

                var resp = objectMapper.readTree(raw);
                String stopReason = resp.path("stop_reason").asText("");

                if (!"tool_use".equals(stopReason)) {
                    // Retorna el primer bloc de text
                    for (JsonNode block : resp.path("content")) {
                        if ("text".equals(block.path("type").asText())) {
                            return block.path("text").asText("");
                        }
                    }
                    return "";
                }

                // Executa les eines i continua
                var assistantContent = new ArrayList<Map<String, Object>>();
                var toolResults      = new ArrayList<Map<String, Object>>();

                for (JsonNode block : resp.path("content")) {
                    String type = block.path("type").asText();
                    if ("text".equals(type)) {
                        assistantContent.add(Map.of("type", "text", "text", block.path("text").asText("")));
                    } else if ("tool_use".equals(type)) {
                        String toolUseId = block.path("id").asText();
                        String toolName  = block.path("name").asText();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> toolInput = objectMapper.convertValue(block.path("input"), Map.class);

                        assistantContent.add(Map.of(
                            "type", "tool_use",
                            "id",   toolUseId,
                            "name", toolName,
                            "input", toolInput != null ? toolInput : Map.of()
                        ));

                        String result = toolExecutor.apply(
                            new ToolCall(toolUseId, toolName, toolInput != null ? toolInput : Map.of()));
                        toolResults.add(Map.of(
                            "type",        "tool_result",
                            "tool_use_id", toolUseId,
                            "content",     result != null ? result : ""
                        ));
                    }
                }

                messages.add(Map.of("role", "assistant", "content", assistantContent));
                messages.add(Map.of("role", "user",      "content", toolResults));
            }
            // Màxim d'iteracions assolit: crida final sense eines
            return chat(systemPrompt, history, userMessage);
        } catch (Exception e) {
            log.error("Anthropic tool_use error: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String providerName() {
        return "anthropic/" + model;
    }

    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
