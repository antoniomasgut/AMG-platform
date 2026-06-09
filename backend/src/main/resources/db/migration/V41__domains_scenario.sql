-- Mòdul 23: Afegeix escenari de domini i verificació DNS
ALTER TABLE managed_domains
    ADD COLUMN IF NOT EXISTS scenario      VARCHAR(30)  NOT NULL DEFAULT 'OPENPROVIDER_NEW',
    ADD COLUMN IF NOT EXISTS dns_verified  BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS dns_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS epp_code      VARCHAR(100);
