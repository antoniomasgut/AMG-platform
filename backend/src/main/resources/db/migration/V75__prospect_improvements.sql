-- V75: Millores prospecció — tracking obertures, seqüència follow-up, importació CSV (Spec 12 v2.1)

-- Tracking obertures d'emails i demos
ALTER TABLE prospects
    ADD COLUMN IF NOT EXISTS pitch_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS pitch_opened_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS demo_opened_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS followup_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS last_followup_at TIMESTAMPTZ;

-- Configuració seqüència automàtica de follow-up per campanya
ALTER TABLE prospect_campaigns
    ADD COLUMN IF NOT EXISTS auto_sequence_enabled BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS followup_days_1 INTEGER DEFAULT 3,
    ADD COLUMN IF NOT EXISTS followup_days_2 INTEGER DEFAULT 7,
    ADD COLUMN IF NOT EXISTS followup_days_3 INTEGER DEFAULT 14;

-- Índex per accelerar la cerca de prospects a seguir
CREATE INDEX IF NOT EXISTS idx_prospects_contacted_pitch ON prospects(status, pitch_sent_at)
    WHERE status = 'CONTACTED' AND pitch_sent_at IS NOT NULL;
