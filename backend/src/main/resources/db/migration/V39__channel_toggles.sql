ALTER TABLE tenant_chat_links
  ADD COLUMN IF NOT EXISTS widget_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS whatsapp_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS email_enabled     BOOLEAN NOT NULL DEFAULT FALSE;

-- Migra l'estat dels registres existents: si el canal estava configurat i actiu, s'habilita
UPDATE tenant_chat_links
  SET whatsapp_enabled = TRUE
  WHERE (whatsapp_phone_number IS NOT NULL AND whatsapp_phone_number <> '')
     OR (whatsapp_meta_phone_number_id IS NOT NULL AND whatsapp_meta_phone_number_id <> '');

UPDATE tenant_chat_links
  SET email_enabled = TRUE
  WHERE email_address IS NOT NULL AND email_address <> '';
