-- V73: Social Publisher — Spec 52
-- Publicació multi-xarxa (Instagram, Facebook, Google Business) via Telegram + IA

CREATE TABLE social_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    network VARCHAR(30) NOT NULL,           -- INSTAGRAM, FACEBOOK, GOOGLE_BUSINESS
    post_type VARCHAR(30) NOT NULL,         -- FEED_PHOTO, POST_TEXT, POST_PHOTO, WHATS_NEW, OFFER, EVENT
    caption TEXT,
    media_url TEXT,                         -- URL a MinIO
    external_post_id VARCHAR(255),          -- ID retornat per la xarxa
    external_post_url TEXT,                 -- URL pública del post
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',  -- DRAFT, SCHEDULED, PUBLISHED, FAILED, CANCELLED
    scheduled_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_social_posts_tenant ON social_posts(tenant_id, created_at DESC);
CREATE INDEX idx_social_posts_scheduled ON social_posts(status, scheduled_at)
    WHERE status = 'SCHEDULED';

-- Configuració Meta per a publicació social (Page Token + IG Account)
CREATE TABLE social_meta_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    facebook_page_id VARCHAR(100),
    instagram_account_id VARCHAR(100),
    page_access_token_encrypted TEXT,   -- Long-lived token (60 dies), xifrat AES-256
    token_expires_at TIMESTAMPTZ,
    pages_manage_posts_granted BOOLEAN NOT NULL DEFAULT false,
    ig_content_publish_granted BOOLEAN NOT NULL DEFAULT false,
    connected_at TIMESTAMPTZ DEFAULT now(),
    updated_at TIMESTAMPTZ DEFAULT now()
);
