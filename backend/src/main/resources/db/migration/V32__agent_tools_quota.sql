-- Mòdul 41: Agent Tool Calling + auto-quota increment
ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS overage_token_increment   INTEGER DEFAULT 50000,
    ADD COLUMN IF NOT EXISTS monthly_message_budget    INTEGER,
    ADD COLUMN IF NOT EXISTS overage_message_increment INTEGER DEFAULT 100;
