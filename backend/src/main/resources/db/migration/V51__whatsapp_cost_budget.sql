-- Cost en micros d'€ per missatge WA (calculat en el moment de l'enviament)
ALTER TABLE channel_usage_logs
    ADD COLUMN IF NOT EXISTS cost_eur_micros BIGINT NOT NULL DEFAULT 0;

-- Pressupost mensual WhatsApp en cèntims d'€ (500 = €5)
ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS monthly_whatsapp_budget_eur_cents INTEGER;
