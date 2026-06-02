-- Mòdul 29: Web Hosting & Import
CREATE TABLE IF NOT EXISTS websites (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING_REVIEW',
    domain          VARCHAR(255),
    container_name  VARCHAR(100),
    storage_bytes   BIGINT,
    client_notes    TEXT,
    review_notes    TEXT,
    reviewed_by     UUID,
    reviewed_at     TIMESTAMPTZ,
    deployed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_websites_tenant_id ON websites (tenant_id);
CREATE INDEX IF NOT EXISTS idx_websites_status ON websites (status);
