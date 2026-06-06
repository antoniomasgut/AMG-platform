-- Mòdul 38 — Tenant Storage
-- Taula de configuració de proveïdors d'emmagatzematge per tenant

CREATE TABLE storage_provider_configs (
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    provider_key VARCHAR(30) NOT NULL,
    config_json TEXT NOT NULL DEFAULT '{}',
    is_active BOOLEAN NOT NULL DEFAULT false,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, provider_key)
);

CREATE INDEX idx_storage_provider_configs_tenant ON storage_provider_configs(tenant_id);
CREATE INDEX idx_storage_provider_configs_active ON storage_provider_configs(is_active);

-- Ampliar generated_documents amb camps de storage extern
ALTER TABLE generated_documents
    ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(30),
    ADD COLUMN IF NOT EXISTS storage_file_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);
