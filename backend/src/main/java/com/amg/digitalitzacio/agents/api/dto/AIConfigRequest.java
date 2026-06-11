package com.amg.digitalitzacio.agents.api.dto;

public record AIConfigRequest(String preferredModel, Integer maxTokens, Double temperature,
                              String reasoningModel, Integer monthlyTokenBudget, Integer budgetAlertThreshold,
                              String senderEmail, String senderName, String replyToEmail,
                              String responseLanguage,
                              String chatModel, String whatsappModel, String emailModel,
                              String fallbackModel,
                              Integer monthlyCostBudgetEurCents,
                              Integer monthlyWhatsappBudgetEurCents) {}
