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
            "system", cachedSystem(systemPrompt),
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
                body.put("system", cachedSystem(systemPrompt));
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
    public AIResponse chatWithToolsTracked(String systemPrompt, List<ChatMessage> history, String userMessage,
                                            List<ToolDefinition> tools, Function<ToolCall, String> toolExecutor) {
        if (tools == null || tools.isEmpty()) {
            // Crida simple: extraiem tokens del JSON de chat()
            return chatTracked(systemPrompt, history, userMessage);
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

        int totalInput = 0, totalOutput = 0;
        try {
            for (int iteration = 0; iteration < 3; iteration++) {
                var body = new HashMap<String, Object>();
                body.put("model", model);
                body.put("max_tokens", 1024);
                body.put("system", cachedSystem(systemPrompt));
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
                totalInput  += resp.path("usage").path("input_tokens").asInt(0);
                totalOutput += resp.path("usage").path("output_tokens").asInt(0);
                String stopReason = resp.path("stop_reason").asText("");

                if (!"tool_use".equals(stopReason)) {
                    for (JsonNode block : resp.path("content")) {
                        if ("text".equals(block.path("type").asText())) {
                            return new AIResponse(block.path("text").asText(""), totalInput, totalOutput);
                        }
                    }
                    return new AIResponse("", totalInput, totalOutput);
                }

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
                        assistantContent.add(Map.of("type", "tool_use", "id", toolUseId,
                            "name", toolName, "input", toolInput != null ? toolInput : Map.of()));
                        String result = toolExecutor.apply(
                            new ToolCall(toolUseId, toolName, toolInput != null ? toolInput : Map.of()));
                        toolResults.add(Map.of("type", "tool_result", "tool_use_id", toolUseId,
                            "content", result != null ? result : ""));
                    }
                }
                messages.add(Map.of("role", "assistant", "content", assistantContent));
                messages.add(Map.of("role", "user",      "content", toolResults));
            }
            var fallback = chatTracked(systemPrompt, history, userMessage);
            return new AIResponse(fallback.text(), totalInput + fallback.inputTokens(), totalOutput + fallback.outputTokens());
        } catch (Exception e) {
            log.error("Anthropic tool_use tracked error: {}", e.getMessage());
            return new AIResponse(null, totalInput, totalOutput);
        }
    }

    private AIResponse chatTracked(String systemPrompt, List<ChatMessage> history, String userMessage) {
        try {
            var messages = new ArrayList<Map<String, Object>>();
            for (var msg : history) {
                messages.add(Map.of("role", mapRole(msg.role()), "content", msg.content()));
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            var body = new HashMap<String, Object>();
            body.put("model", model);
            body.put("max_tokens", 1024);
            body.put("system", cachedSystem(systemPrompt));
            body.put("messages", messages);

            var raw = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            var resp = objectMapper.readTree(raw);
            String text = resp.path("content").path(0).path("text").asText("");
            int inputTokens  = resp.path("usage").path("input_tokens").asInt(0);
            int outputTokens = resp.path("usage").path("output_tokens").asInt(0);
            return new AIResponse(text, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("Anthropic API error (tracked): {}", e.getMessage());
            return new AIResponse(null, 0, 0);
        }
    }

    @Override
    public String providerName() {
        return "anthropic/" + model;
    }

    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }

    /**
     * Bloc de sistema amb prompt caching d'Anthropic: marca la part estable del prompt
     * (persona + coneixement + serveis + catàleg) com a cacheable. En els missatges
     * següents dins la finestra de ~5 min, aquesta part es cobra a ~10% del preu d'entrada.
     * Si el prompt no arriba al mínim cacheable, l'API simplement l'ignora (sense error).
     */
    private static List<Map<String, Object>> cachedSystem(String systemPrompt) {
        return List.of(Map.of(
            "type", "text",
            "text", systemPrompt != null ? systemPrompt : "",
            "cache_control", Map.of("type", "ephemeral")
        ));
    }
}
