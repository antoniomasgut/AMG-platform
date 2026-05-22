package com.amg.digitalitzacio.shared.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

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
        this.restClient = builder.baseUrl(BASE_URL).build();
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
    public String providerName() {
        return "anthropic/" + model;
    }

    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
