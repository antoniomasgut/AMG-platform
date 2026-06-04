package com.amg.digitalitzacio.analytics.application;

import com.amg.digitalitzacio.agents.application.scheduler.ReportScheduler;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRepository;
import com.amg.digitalitzacio.analytics.api.dto.AnalyticsResponse;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final LeadRepository leadRepository;
    private final ConversationRepository conversationRepository;
    private final BudgetRepository budgetRepository;
    private final ReportScheduler reportScheduler;

    public AnalyticsResponse getAnalytics(UUID tenantId) {
        Instant now    = Instant.now();
        Instant ago7d  = now.minus(7,  ChronoUnit.DAYS);
        Instant ago30d = now.minus(30, ChronoUnit.DAYS);

        // Leads
        long totalLeads  = leadRepository.countByTenantId(tenantId);
        long newLeads7d  = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, ago7d);
        long newLeads30d = leadRepository.countByTenantIdAndCreatedAtAfter(tenantId, ago30d);
        long wonLeads    = leadRepository.countByTenantIdAndStage(tenantId, PipelineStage.WON);
        double convRate  = totalLeads > 0 ? (double) wonLeads / totalLeads * 100 : 0;

        Map<String, Long> byStage  = new HashMap<>();
        for (PipelineStage s : PipelineStage.values()) {
            long c = leadRepository.countByTenantIdAndStage(tenantId, s);
            if (c > 0) byStage.put(s.name(), c);
        }

        Map<String, Long> bySource = new HashMap<>();
        for (LeadSource s : LeadSource.values()) {
            long c = leadRepository.countByTenantIdAndSource(tenantId, s);
            if (c > 0) bySource.put(s.name(), c);
        }

        // Converses
        long totalConv   = conversationRepository.countByTenantId(tenantId);
        long newConv7d   = conversationRepository.countByTenantIdAndCreatedAtAfter(tenantId, ago7d);
        long pendingAppr = conversationRepository.countByTenantIdAndPendingApprovalTrue(tenantId);

        Map<String, Long> byChannel = new HashMap<>();
        for (ConversationChannel ch : ConversationChannel.values()) {
            long c = conversationRepository.countByTenantIdAndChannel(tenantId, ch);
            if (c > 0) byChannel.put(ch.name(), c);
        }

        // Pressupostos
        long totalBudgets    = budgetRepository.countByTenantId(tenantId);
        long sentBudgets     = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.SENT);
        long acceptedBudgets = budgetRepository.countByTenantIdAndStatus(tenantId, BudgetStatus.ACCEPTED);
        double budgetConv    = totalBudgets > 0 ? (double) acceptedBudgets / totalBudgets * 100 : 0;

        // Temps de resposta (aproximació: no tenim dades precises, retornem 0 de moment)
        long avgResponseMinutes = 0;

        return new AnalyticsResponse(
            totalLeads, newLeads7d, newLeads30d, wonLeads, convRate,
            byStage, bySource,
            totalConv, newConv7d, pendingAppr, byChannel,
            totalBudgets, sentBudgets, acceptedBudgets, budgetConv,
            avgResponseMinutes,
            reportScheduler.buildDailyReportText(tenantId),
            reportScheduler.buildWeeklyReportText(tenantId)
        );
    }
}
