-- Mòdul 59 · mode d'agent per canal (nullable → hereten agent_mode global)
ALTER TABLE tenant_chat_links ADD COLUMN IF NOT EXISTS email_mode    VARCHAR(10);
ALTER TABLE tenant_chat_links ADD COLUMN IF NOT EXISTS whatsapp_mode VARCHAR(10);
ALTER TABLE tenant_chat_links ADD COLUMN IF NOT EXISTS widget_mode   VARCHAR(10);
