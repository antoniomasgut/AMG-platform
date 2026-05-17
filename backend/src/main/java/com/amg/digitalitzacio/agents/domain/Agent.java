package com.amg.digitalitzacio.agents.domain;

import java.util.UUID;

public interface Agent {

    String getServiceSlug();

    String getDisplayName();

    void onActivate(UUID tenantId);

    void onDeactivate(UUID tenantId);

    String handleMessage(UUID tenantId, String text, Long chatId);

    void executeTask(UUID tenantId, String taskType, String payload);
}
