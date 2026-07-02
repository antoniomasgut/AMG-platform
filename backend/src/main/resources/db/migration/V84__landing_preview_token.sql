-- Mòdul 04/05: Token de previsualització per a aprovació de client
ALTER TABLE landings ADD COLUMN IF NOT EXISTS preview_token VARCHAR(64) UNIQUE;
