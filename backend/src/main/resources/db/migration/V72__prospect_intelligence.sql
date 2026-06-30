-- V72: Prospecció Intel·ligent — nous camps a prospects i prospect_campaigns (Spec 12 v2)

-- Anàlisi web
ALTER TABLE prospects
    ADD COLUMN IF NOT EXISTS has_ssl BOOLEAN,
    ADD COLUMN IF NOT EXISTS is_responsive BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_analytics BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_pixel BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_gtm BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_chat_widget BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_booking_system BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_contact_form BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_faq BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_clear_cta BOOLEAN,
    ADD COLUMN IF NOT EXISTS tech_stack VARCHAR(500),
    ADD COLUMN IF NOT EXISTS web_load_ms INTEGER,
    ADD COLUMN IF NOT EXISTS cms_detected VARCHAR(50),
    ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(300),
    ADD COLUMN IF NOT EXISTS linkedin_url VARCHAR(300),
    ADD COLUMN IF NOT EXISTS tiktok_url VARCHAR(300),
    ADD COLUMN IF NOT EXISTS youtube_url VARCHAR(300),
    ADD COLUMN IF NOT EXISTS has_facebook BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_linkedin BOOLEAN,
    ADD COLUMN IF NOT EXISTS has_tiktok BOOLEAN,
    ADD COLUMN IF NOT EXISTS score_breakdown TEXT,
    ADD COLUMN IF NOT EXISTS prospect_tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS ai_report TEXT,
    ADD COLUMN IF NOT EXISTS ai_pitch TEXT,
    ADD COLUMN IF NOT EXISTS demo_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS widget_code TEXT,
    ADD COLUMN IF NOT EXISTS web_analyzed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS ai_analyzed_at TIMESTAMPTZ;

-- Configuració de campanya intel·ligent
ALTER TABLE prospect_campaigns
    ADD COLUMN IF NOT EXISTS auto_demo_threshold INTEGER DEFAULT 61,
    ADD COLUMN IF NOT EXISTS auto_contact_channel VARCHAR(20),
    ADD COLUMN IF NOT EXISTS total_demos_generated INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_discarded INTEGER DEFAULT 0;

-- Índex per tier (útil per filtrar PRIORITY)
CREATE INDEX IF NOT EXISTS idx_prospects_tier ON prospects(campaign_id, prospect_tier);
