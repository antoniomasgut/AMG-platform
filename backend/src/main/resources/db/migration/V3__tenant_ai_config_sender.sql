ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS sender_email VARCHAR(150),
    ADD COLUMN IF NOT EXISTS sender_name  VARCHAR(100);
