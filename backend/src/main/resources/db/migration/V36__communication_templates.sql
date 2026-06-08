CREATE TABLE communication_templates (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sector      VARCHAR(50)  NOT NULL DEFAULT 'ALL',
    channel     VARCHAR(20)  NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    language    VARCHAR(5)   NOT NULL DEFAULT 'ca',
    subject     VARCHAR(200),
    body        TEXT         NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (sector, channel, action, language)
);
