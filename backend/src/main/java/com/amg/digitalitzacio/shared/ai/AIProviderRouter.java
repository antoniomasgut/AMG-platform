package com.amg.digitalitzacio.shared.ai;

import com.amg.digitalitzacio.shared.exception.MissingApiKeyException;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Crea el proveïdor d'IA adequat a partir del nom del model configurat per tenant.
 *
 * Convenció de noms:
 *   claude-*         → Anthropic
 *   deepseek-*       → DeepSeek (api.deepseek.com, OpenAI-compatible)
 *   qualsevol altra  → Ollama local (OpenAI-compatible)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AIProviderRouter {

    public static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";
    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com";
    private static final String OLLAMA_DEFAULT_URL = "http://localhost:11434";

    private final SystemConfigService sysConfig;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AIProvider forModel(String model) {
        String m = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;

        if (m.startsWith("claude-")) {
            String apiKey = sysConfig.get("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isBlank())
                throw new MissingApiKeyException("Anthropic Claude", "ANTHROPIC_API_KEY");
            return new AnthropicAIProvider(apiKey, m, restClientBuilder, objectMapper);
        }

        if (m.startsWith("deepseek-")) {
            String apiKey = sysConfig.get("DEEPSEEK_API_KEY");
            if (apiKey == null || apiKey.isBlank())
                throw new MissingApiKeyException("DeepSeek", "DEEPSEEK_API_KEY");
            return new OpenAICompatibleAIProvider(DEEPSEEK_BASE_URL, apiKey, m, "deepseek",
                    restClientBuilder, objectMapper);
        }

        // Ollama (qualsevol model local: qwen, gemma, llama, etc.)
        String ollamaUrl = sysConfig.get("OLLAMA_BASE_URL");
        if (ollamaUrl == null || ollamaUrl.isBlank()) ollamaUrl = OLLAMA_DEFAULT_URL;
        return new OpenAICompatibleAIProvider(ollamaUrl, null, m, "ollama",
                restClientBuilder, objectMapper);
    }

    /** Retorna la llista de models disponibles segons les claus configurades. */
    public List<ModelInfo> availableModels() {
        var list = new ArrayList<ModelInfo>();

        if (hasKey("ANTHROPIC_API_KEY")) {
            list.add(new ModelInfo("claude-sonnet-4-6", "Claude Sonnet 4.6", "anthropic", true));
            list.add(new ModelInfo("claude-haiku-4-5-20251001", "Claude Haiku 4.5", "anthropic", true));
        }

        if (hasKey("DEEPSEEK_API_KEY")) {
            list.add(new ModelInfo("deepseek-chat", "DeepSeek V3", "deepseek", true));
            list.add(new ModelInfo("deepseek-reasoner", "DeepSeek R1 (Reasoner)", "deepseek", true));
        }

        // Models Ollama: llegeix els disponibles de l'API local
        String ollamaUrl = sysConfig.get("OLLAMA_BASE_URL");
        if (ollamaUrl == null || ollamaUrl.isBlank()) ollamaUrl = OLLAMA_DEFAULT_URL;
        list.addAll(fetchOllamaModels(ollamaUrl));

        return list;
    }

    private List<ModelInfo> fetchOllamaModels(String baseUrl) {
        try {
            var rc = restClientBuilder.baseUrl(baseUrl).build();
            var raw = rc.get().uri("/api/tags").retrieve().body(String.class);
            var root = objectMapper.readTree(raw);
            var models = new ArrayList<ModelInfo>();
            for (var node : root.path("models")) {
                String name = node.path("name").asText();
                double sizeGb = node.path("size").asDouble() / 1e9;
                models.add(new ModelInfo(name, name + String.format(" (%.1f GB)", sizeGb), "ollama", false));
            }
            return models;
        } catch (Exception e) {
            log.debug("Ollama no disponible a {}: {}", baseUrl, e.getMessage());
            return List.of();
        }
    }

    private boolean hasKey(String key) {
        String v = sysConfig.get(key);
        return v != null && !v.isBlank();
    }

    public record ModelInfo(String id, String label, String provider, boolean requiresApiKey) {}
}
