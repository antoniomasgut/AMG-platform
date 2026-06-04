-- Webs externes (EXTERNAL): widget i formulari de contacte sense hosting propi
ALTER TABLE websites
    ADD COLUMN IF NOT EXISTS contact_email        VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_redirect_url VARCHAR(500);
