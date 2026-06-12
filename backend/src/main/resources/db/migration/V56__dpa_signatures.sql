CREATE TABLE dpa_signatures (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    token           VARCHAR(64)  NOT NULL UNIQUE,
    document_version VARCHAR(10) NOT NULL DEFAULT '1.0',
    signer_name     VARCHAR(150),
    signer_position VARCHAR(100),
    signer_email    VARCHAR(150),
    signed_at       TIMESTAMPTZ,
    ip_address      VARCHAR(45),
    user_agent      TEXT,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_dpa_signatures_tenant  ON dpa_signatures(tenant_id);
CREATE INDEX idx_dpa_signatures_token   ON dpa_signatures(token);
CREATE INDEX idx_dpa_signatures_signed  ON dpa_signatures(signed_at) WHERE signed_at IS NOT NULL;
