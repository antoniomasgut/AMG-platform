-- V17: Meta Ads, Booking tokens, Meeting settings
-- Totes les sentències usen IF NOT EXISTS / IF EXISTS per ser idempotents

-- ─── ad_campaigns ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ad_campaigns (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    meta_campaign_id VARCHAR(50),
    name             VARCHAR(200) NOT NULL,
    objective        VARCHAR(50)  NOT NULL,
    status           VARCHAR(30)  NOT NULL,
    daily_budget     NUMERIC(10,2),
    lifetime_budget  NUMERIC(10,2),
    notes            TEXT,
    start_time       TIMESTAMPTZ,
    stop_time        TIMESTAMPTZ,
    meta_error       TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ad_campaigns_tenant ON ad_campaigns(tenant_id);

-- ─── ad_creatives ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ad_creatives (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID,
    meta_creative_id VARCHAR(50),
    name             VARCHAR(200),
    title            VARCHAR(255),
    body             VARCHAR(600),
    headline         VARCHAR(50),
    description      VARCHAR(50),
    call_to_action   VARCHAR(100),
    link_url         VARCHAR(500),
    image_asset_id   UUID,
    meta_image_hash  VARCHAR(50),
    meta_video_id    VARCHAR(50),
    meta_lead_form_id VARCHAR(50),
    object_story_spec TEXT,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── ad_sets ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ad_sets (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    campaign_id         UUID         NOT NULL REFERENCES ad_campaigns(id),
    tenant_id           UUID,
    meta_ad_set_id      VARCHAR(50),
    name                VARCHAR(200) NOT NULL,
    status              VARCHAR(30)  NOT NULL,
    daily_budget        NUMERIC(10,2),
    optimization_goal   VARCHAR(50),
    billing_event       VARCHAR(50),
    bid_amount          NUMERIC(10,2),
    age_min             INTEGER,
    age_max             INTEGER,
    genders             VARCHAR(20),
    geo_locations_json  TEXT,
    interests_json      TEXT,
    publisher_platforms VARCHAR(100),
    start_time          TIMESTAMPTZ,
    stop_time           TIMESTAMPTZ,
    meta_error          TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ad_sets_campaign ON ad_sets(campaign_id);

-- ─── ads ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ads (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_set_id   UUID         NOT NULL REFERENCES ad_sets(id),
    tenant_id   UUID,
    meta_ad_id  VARCHAR(50),
    name        VARCHAR(200),
    status      VARCHAR(30)  NOT NULL,
    creative_id UUID         REFERENCES ad_creatives(id),
    meta_error  TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ads_ad_set ON ads(ad_set_id);
CREATE INDEX IF NOT EXISTS idx_ads_creative ON ads(creative_id);

-- ─── campaign_spend ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS campaign_spend (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id),
    campaign_id   VARCHAR(50)  NOT NULL,
    campaign_name VARCHAR(200) NOT NULL,
    spend         NUMERIC(10,2),
    impressions   BIGINT,
    clicks        BIGINT,
    spend_date    DATE         NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, campaign_id, spend_date)
);
CREATE INDEX IF NOT EXISTS idx_campaign_spend_tenant ON campaign_spend(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaign_spend_date ON campaign_spend(spend_date);

-- ─── meta_ads_configs ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meta_ads_configs (
    tenant_id     UUID        PRIMARY KEY REFERENCES tenants(id),
    ad_account_id VARCHAR(50),
    access_token  TEXT,
    enabled       BOOLEAN     NOT NULL DEFAULT false,
    last_sync_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── booking_tokens ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS booking_tokens (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id),
    lead_id         UUID         NOT NULL,
    token           VARCHAR(64)  NOT NULL UNIQUE,
    lead_name       VARCHAR(150) NOT NULL,
    lead_email      VARCHAR(150),
    expires_at      TIMESTAMPTZ  NOT NULL,
    confirmed       BOOLEAN      NOT NULL DEFAULT false,
    meeting_at      TIMESTAMPTZ,
    meet_link       VARCHAR(500),
    google_event_id VARCHAR(255),
    created_at      TIMESTAMPTZ  DEFAULT NOW()
);

-- ─── meeting_settings ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS meeting_settings (
    tenant_id             UUID        PRIMARY KEY REFERENCES tenants(id),
    working_days          VARCHAR(50) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI',
    start_time            TIME        NOT NULL DEFAULT '09:00',
    end_time              TIME        NOT NULL DEFAULT '18:00',
    slot_duration_minutes INTEGER     NOT NULL DEFAULT 45,
    buffer_minutes        INTEGER     NOT NULL DEFAULT 15,
    min_notice_hours      INTEGER     NOT NULL DEFAULT 24,
    max_advance_days      INTEGER     NOT NULL DEFAULT 30,
    calendar_id           VARCHAR(500),
    updated_at            TIMESTAMPTZ
);

-- ─── Columnes afegides a leads (Meta Lead Ads + UTMs) ────────────────────────
ALTER TABLE leads ADD COLUMN IF NOT EXISTS meta_lead_id VARCHAR(50);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_source VARCHAR(100);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_medium VARCHAR(100);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS utm_campaign VARCHAR(150);

-- ─── Columnes afegides a tenants ──────────────────────────────────────────
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS billing_start_date DATE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS implementation_delivered_at TIMESTAMPTZ;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS onboarding_completed_at TIMESTAMPTZ;
