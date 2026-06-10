-- Afegeix WIDGET als constraints de canal que no el tenien
-- ConversationChannel.WIDGET existia a l'enum però faltava als CHECK constraints de la BD

ALTER TABLE contact_identifiers
  DROP CONSTRAINT IF EXISTS contact_identifiers_channel_check,
  ADD CONSTRAINT contact_identifiers_channel_check
    CHECK (channel IN ('WHATSAPP', 'WHATSAPP_META', 'TELEGRAM', 'EMAIL', 'WIDGET'));

ALTER TABLE conversations
  DROP CONSTRAINT IF EXISTS conversations_channel_check,
  ADD CONSTRAINT conversations_channel_check
    CHECK (channel IN ('WHATSAPP', 'WHATSAPP_META', 'TELEGRAM', 'EMAIL', 'WIDGET'));

ALTER TABLE communication_requests
  DROP CONSTRAINT IF EXISTS communication_requests_channel_check,
  ADD CONSTRAINT communication_requests_channel_check
    CHECK (channel IN ('WHATSAPP', 'TELEGRAM', 'EMAIL', 'WIDGET'));
