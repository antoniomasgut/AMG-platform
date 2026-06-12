ALTER TABLE token_usage_logs
    ADD COLUMN IF NOT EXISTS cost_eur_micros BIGINT NOT NULL DEFAULT 0;

ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS monthly_cost_budget_eur_cents INTEGER;
