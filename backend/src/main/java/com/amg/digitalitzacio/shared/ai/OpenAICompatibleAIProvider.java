package com.amg.digitalitzacio.shared.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proveïdor OpenAI-compatible. Funciona amb DeepSeek i Ollama (mateixa API).
 */
@Slf4j
public class OpenAICompatibleAIProvider implements AIProvider {

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String provider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenAICompatibleAIProvider(String baseUrl, String apiKey, String model,
                                       String provider, RestClient.Builder builder,
                                       ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model;
        this.provider = provider;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restClient = builder.requestFactory(factory).baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String chat(String systemPrompt, List<ChatMessage> history, String userMessage) {
        var messages = new ArrayList<Map<String, String>>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (var msg : history) {
            messages.add(Map.of("role", mapRole(msg.role()), "content", msg.content()));
        }
        messages.add(Map.of("role", "user", "content", userMessage));

        var body = new HashMap<String, Object>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);

        try {
            var rb = restClient.post()
                .uri("/v1/chat/completions")
                .header("Content-Type", "application/json");

            if (apiKey != null && !apiKey.isBlank()) {
                rb = rb.header("Authorization", "Bearer " + apiKey);
            }

            var raw = rb.body(objectMapper.writeValueAsString(body))
                .retrieve()
                .body(String.class);

            var resp = objectMapper.readTree(raw);
            return resp.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception e) {
            log.error("{} API error (model={}): {}", provider, model, e.getMessage());
            return null;
        }
    }

    @Override
    public String providerName() {
        return provider + "/" + model;
    }

    private String mapRole(String role) {
        return "ASSISTANT".equalsIgnoreCase(role) ? "assistant" : "user";
    }
}
