package com.amg.digitalitzacio.automations.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.automations.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockN8nClient implements N8nClient {

    @Override
    public N8nWorkflowResult deployWorkflow(String workflowJson) {
        var id = UUID.randomUUID().toString();
        log.info("[MOCK n8n] Deployed workflow {} ({} bytes)", id, workflowJson != null ? workflowJson.length() : 0);
        return new N8nWorkflowResult(id, "https://hooks.mock.n8n/webhook/" + id);
    }

    @Override
    public boolean activateWorkflow(String n8nWorkflowId) {
        log.info("[MOCK n8n] Activated workflow {}", n8nWorkflowId);
        return true;
    }

    @Override
    public boolean deactivateWorkflow(String n8nWorkflowId) {
        log.info("[MOCK n8n] Deactivated workflow {}", n8nWorkflowId);
        return true;
    }

    @Override
    public boolean deleteWorkflow(String n8nWorkflowId) {
        log.info("[MOCK n8n] Deleted workflow {}", n8nWorkflowId);
        return true;
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public String getVersion() {
        return "1.80.0";
    }
}
