-- Mòdul 52/55: reintents automàtics per a posts FAILED.
-- retry_count: nombre de reintents realitzats (max 2 = 3 intents en total).
ALTER TABLE social_posts ADD COLUMN IF NOT EXISTS retry_count INTEGER NOT NULL DEFAULT 0;
