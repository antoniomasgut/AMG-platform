package com.amg.digitalitzacio.agents.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenant_ai_configs")
@Getter @Setter @NoArgsConstructor
public class TenantAIConfig {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "preferred_model", nullable = false)
    private String preferredModel = "claude-haiku-4-5-20251001";

    @Column(name = "max_tokens")
    private Integer maxTokens = 1024;

    @Column(name = "temperature")
    private Double temperature = 0.7;

    @Column(name = "reasoning_model")
    private String reasoningModel;

    @Column(name = "monthly_token_budget")
    private Integer monthlyTokenBudget;

    @Column(name = "budget_alert_threshold")
    private Integer budgetAlertThreshold = 80;

    public static TenantAIConfig defaultFor(UUID tenantId) {
        var c = new TenantAIConfig();
        c.tenantId = tenantId;
        return c;
    }
}
