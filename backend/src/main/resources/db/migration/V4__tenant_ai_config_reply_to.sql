ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS reply_to_email VARCHAR(150);
