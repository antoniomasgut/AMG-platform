-- Mòdul 15: demo landing redesign
-- 1. Permet service_id null per a landings de demo (no requereixen servei de pagament)
ALTER TABLE landings ALTER COLUMN service_id DROP NOT NULL;

-- 2. Afegir is_active i landing_slug a demo_sessions
ALTER TABLE demo_sessions
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS landing_slug VARCHAR(100);
