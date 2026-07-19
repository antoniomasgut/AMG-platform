-- Mòdul 29: Web Hosting & Import.
-- Idempotent: V8 (catchup afegit posteriorment amb número anterior) ja crea la taula
-- en BD noves; aquí només garantim que existeix amb els índexs.
CREATE TABLE IF NOT EXISTS websites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    domain VARCHAR(255),
    container_name VARCHAR(100),
    storage_bytes BIGINT DEFAULT 0,
    review_notes TEXT,
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    deployed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE websites ADD COLUMN IF NOT EXISTS client_notes TEXT;
CREATE INDEX IF NOT EXISTS idx_websites_tenant ON websites(tenant_id);
CREATE INDEX IF NOT EXISTS idx_websites_status ON websites(status);
