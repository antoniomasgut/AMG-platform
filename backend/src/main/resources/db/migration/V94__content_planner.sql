-- Mòdul 58 · Content Planner & Brief Automation

CREATE TABLE content_plans (
    id                UUID PRIMARY KEY,
    tenant_id         UUID NOT NULL,
    period            VARCHAR(7)  NOT NULL,           -- YYYY-MM
    status            VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    content_language  VARCHAR(5),
    created_by        UUID,
    notes             TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_plans_tenant_period UNIQUE (tenant_id, period)
);

CREATE INDEX idx_content_plans_tenant ON content_plans (tenant_id);
CREATE INDEX idx_content_plans_status ON content_plans (status);

CREATE TABLE content_plan_items (
    id                   UUID PRIMARY KEY,
    plan_id              UUID NOT NULL REFERENCES content_plans (id) ON DELETE CASCADE,
    tenant_id            UUID NOT NULL,
    week_number          INTEGER NOT NULL,
    pillar               VARCHAR(20) NOT NULL,
    brief_text           TEXT,
    example_text         TEXT,
    networks             VARCHAR(100),
    content_language     VARCHAR(5),
    photo_deadline       DATE,
    target_publish_date  DATE,
    status               VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    media_url            TEXT,
    caption              TEXT,
    error                TEXT,
    brief_sent_at        TIMESTAMPTZ,
    reminder_sent_at     TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_content_plan_items_plan_week UNIQUE (plan_id, week_number)
);

CREATE INDEX idx_content_plan_items_plan ON content_plan_items (plan_id);
CREATE INDEX idx_content_plan_items_tenant_status ON content_plan_items (tenant_id, status);

-- Referència inversa: un SocialPost pot venir d'un item del planner (multi-xarxa)
ALTER TABLE social_posts ADD COLUMN content_plan_item_id UUID;

-- Idioma per defecte de publicacions del tenant
ALTER TABLE social_meta_configs ADD COLUMN default_content_language VARCHAR(5);
