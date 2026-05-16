package com.amg.digitalitzacio.automations.application;

public interface N8nClient {

    N8nWorkflowResult deployWorkflow(String workflowJson);

    boolean activateWorkflow(String n8nWorkflowId);

    boolean deactivateWorkflow(String n8nWorkflowId);

    boolean deleteWorkflow(String n8nWorkflowId);

    boolean isConnected();

    String getVersion();

    record N8nWorkflowResult(String id, String webhookUrl) {}
}
