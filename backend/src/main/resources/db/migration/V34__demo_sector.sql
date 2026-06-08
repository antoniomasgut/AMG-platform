-- Mòdul 15: afegir sector a les sessions de demo
ALTER TABLE demo_sessions
    ADD COLUMN IF NOT EXISTS sector VARCHAR(50);
