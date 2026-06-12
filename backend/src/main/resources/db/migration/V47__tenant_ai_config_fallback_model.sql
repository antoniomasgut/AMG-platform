ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS fallback_model VARCHAR(100);
