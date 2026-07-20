-- Mòdul 52/55: atribució de trànsit social — visites diàries agregades per font.
-- Sense PII (només comptadors): RGPD-safe.
CREATE TABLE IF NOT EXISTS landing_visit_daily (
    landing_id UUID NOT NULL REFERENCES landings(id) ON DELETE CASCADE,
    visit_date DATE NOT NULL,
    source     VARCHAR(30) NOT NULL,
    views      INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (landing_id, visit_date, source)
);
CREATE INDEX IF NOT EXISTS idx_lvd_date ON landing_visit_daily (visit_date);
