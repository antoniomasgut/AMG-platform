ALTER TABLE tenant_ai_configs
    ADD COLUMN IF NOT EXISTS chat_model      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS whatsapp_model  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS email_model     VARCHAR(100);
