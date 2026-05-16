package com.amg.digitalitzacio.automations.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@ConditionalOnProperty(name = "app.automations.provider", havingValue = "n8n")
@Slf4j
public class N8nRealClient implements N8nClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${app.automations.n8n.api-url:#{null}}")
    private String apiUrl;

    @Value("${app.automations.n8n.api-key:#{null}}")
    private String apiKey;

    public N8nRealClient() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    void init() {
        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("N8nRealClient initialized without api-url — set app.automations.n8n.api-url");
        } else {
            log.info("N8nRealClient initialized (api-url: {})", apiUrl);
        }
    }

    @Override
    public N8nWorkflowResult deployWorkflow(String workflowJson) {
        if (apiUrl == null || apiUrl.isBlank()) {
            log.warn("n8n API URL not configured");
            return null;
        }
        try {
            var json = objectMapper.readTree(workflowJson);
            var response = webClient.post()
                    .uri(apiUrl + "/workflows")
                    .header("X-N8N-API-KEY", apiKey != null ? apiKey : "")
                    .bodyValue(json)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return null;

            var id = response.path("id").asText();
            var webhookUrl = response.path("webhookUrl").asText(null);

            log.info("Deployed workflow to n8n: id={}", id);
            return new N8nWorkflowResult(id, webhookUrl);
        } catch (Exception e) {
            log.error("Failed to deploy workflow to n8n: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean activateWorkflow(String n8nWorkflowId) {
        return executeAction("/workflows/" + n8nWorkflowId + "/activate", "activate");
    }

    @Override
    public boolean deactivateWorkflow(String n8nWorkflowId) {
        return executeAction("/workflows/" + n8nWorkflowId + "/deactivate", "deactivate");
    }

    @Override
    public boolean deleteWorkflow(String n8nWorkflowId) {
        try {
            webClient.delete()
                    .uri(apiUrl + "/workflows/" + n8nWorkflowId)
                    .header("X-N8N-API-KEY", apiKey != null ? apiKey : "")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Deleted workflow {} from n8n", n8nWorkflowId);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete workflow {} from n8n: {}", n8nWorkflowId, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        try {
            webClient.get()
                    .uri(apiUrl != null ? apiUrl + "/healthz" : "http://localhost:5678/healthz")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            log.warn("n8n connection check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getVersion() {
        try {
            var response = webClient.get()
                    .uri(apiUrl + "/healthz")
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (response != null) {
                var version = response.path("version").asText(null);
                if (version != null) return version;
            }
        } catch (Exception e) {
            log.debug("Could not fetch n8n version: {}", e.getMessage());
        }
        return "unknown";
    }

    private boolean executeAction(String path, String action) {
        try {
            webClient.post()
                    .uri(apiUrl + path)
                    .header("X-N8N-API-KEY", apiKey != null ? apiKey : "")
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("{}d workflow {} on n8n", action, path);
            return true;
        } catch (Exception e) {
            log.error("Failed to {} workflow {} on n8n: {}", action, path, e.getMessage());
            return false;
        }
    }
}
