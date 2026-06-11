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

    @Column(name = "sender_email", length = 150)
    private String senderEmail;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    @Column(name = "reply_to_email", length = 150)
    private String replyToEmail;

    @Column(name = "overage_token_increment")
    private Integer overageTokenIncrement = 50_000;

    @Column(name = "monthly_message_budget")
    private Integer monthlyMessageBudget;

    @Column(name = "overage_message_increment")
    private Integer overageMessageIncrement = 100;

    // null o "auto" = detecta l'idioma del client; "ca"/"es"/"en"/"de" = sempre en aquest idioma
    @Column(name = "response_language", length = 10)
    private String responseLanguage;

    // null = usa preferredModel; valor = model específic per a aquest canal
    @Column(name = "chat_model")
    private String chatModel;

    @Column(name = "whatsapp_model")
    private String whatsappModel;

    @Column(name = "email_model")
    private String emailModel;

    // model de backup: s'usa si el budget s'esgota o si el provider principal falla
    @Column(name = "fallback_model")
    private String fallbackModel;

    // pressupost mensual IA en cèntims d'euro (p.ex. 500 = €5.00); null = sense límit
    @Column(name = "monthly_cost_budget_eur_cents")
    private Integer monthlyCostBudgetEurCents;

    // pressupost mensual WhatsApp en cèntims d'euro; null = sense límit
    @Column(name = "monthly_whatsapp_budget_eur_cents")
    private Integer monthlyWhatsappBudgetEurCents;

    public static TenantAIConfig defaultFor(UUID tenantId) {
        var c = new TenantAIConfig();
        c.tenantId = tenantId;
        return c;
    }
}
