package com.amg.digitalitzacio.agents.api.dto;

public record AIConfigRequest(String preferredModel, Integer maxTokens, Double temperature,
                              String reasoningModel, Integer monthlyTokenBudget, Integer budgetAlertThreshold,
                              String senderEmail, String senderName, String replyToEmail) {}
