-- Alineació de deriva: columnes creades manualment a producció sense migració.
-- Idempotent: a producció ja existeixen (no-op); en BD noves les crea.
ALTER TABLE prospects ADD COLUMN IF NOT EXISTS score INTEGER;
ALTER TABLE prospects ADD COLUMN IF NOT EXISTS reviews_json TEXT;
