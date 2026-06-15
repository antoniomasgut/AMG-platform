CREATE TABLE secure_document_tokens (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token                VARCHAR(64)  NOT NULL UNIQUE,
    tenant_id            UUID         NOT NULL,
    storage_file_id      VARCHAR(500) NOT NULL,
    file_name            VARCHAR(255) NOT NULL,
    mime_type            VARCHAR(100) NOT NULL,
    sensitivity          VARCHAR(20)  NOT NULL,
    recipient_email      VARCHAR(255) NOT NULL,
    recipient_name       VARCHAR(255),
    description          VARCHAR(500),
    expires_at           TIMESTAMPTZ  NOT NULL,
    max_downloads        INTEGER,
    download_count       INTEGER      NOT NULL DEFAULT 0,
    first_downloaded_at  TIMESTAMPTZ,
    last_downloaded_at   TIMESTAMPTZ,
    email_sent_at        TIMESTAMPTZ,
    revoked              BOOLEAN      NOT NULL DEFAULT false,
    revoked_at           TIMESTAMPTZ,
    created_by           UUID,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sdt_tenant_created ON secure_document_tokens (tenant_id, created_at DESC);
CREATE INDEX idx_sdt_expires        ON secure_document_tokens (expires_at);

CREATE TABLE secure_document_audit (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token_id    UUID         NOT NULL REFERENCES secure_document_tokens(id) ON DELETE CASCADE,
    tenant_id   UUID         NOT NULL,
    event_type  VARCHAR(30)  NOT NULL,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    occurred_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_sda_token ON secure_document_audit (token_id, occurred_at DESC);
