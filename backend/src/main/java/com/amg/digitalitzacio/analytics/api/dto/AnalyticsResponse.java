package com.amg.digitalitzacio.analytics.api.dto;

import java.util.Map;

public record AnalyticsResponse(
    // Leads
    long totalLeads,
    long newLeads7d,
    long newLeads30d,
    long wonLeads,
    double conversionRate,
    Map<String, Long> leadsByStage,
    Map<String, Long> leadsBySource,

    // Converses
    long totalConversations,
    long newConversations7d,
    long pendingApproval,
    Map<String, Long> conversationsByChannel,

    // Pressupostos
    long totalBudgets,
    long sentBudgets,
    long acceptedBudgets,
    double budgetConversionRate,

    // Resposta
    long avgResponseMinutes,

    // Reporti del dia
    String dailyReport,
    String weeklyReport
) {}
