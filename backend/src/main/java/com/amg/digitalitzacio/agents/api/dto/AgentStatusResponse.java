package com.amg.digitalitzacio.agents.api.dto;

import com.amg.digitalitzacio.agents.domain.AgentMode;

public record AgentStatusResponse(
        AgentMode agentMode,
        Boolean telegramLinked,
        Boolean whatsappConfigured,
        Boolean emailConfigured
) {}
