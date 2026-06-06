-- V18: Tenant Document Builder (Spec 37)

CREATE TABLE IF NOT EXISTS document_templates (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id),
    name           VARCHAR(100) NOT NULL,
    document_type  VARCHAR(20)  NOT NULL CHECK (document_type IN ('quote','invoice','delivery_note','contract','report','proposal','custom')),
    version        INTEGER      NOT NULL DEFAULT 1,
    active         BOOLEAN      NOT NULL DEFAULT true,
    layout         JSONB        NOT NULL DEFAULT '[]',
    data_bindings  JSONB        NOT NULL DEFAULT '{}',
    styles         JSONB        NOT NULL DEFAULT '{}',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_template_versions (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id    UUID         NOT NULL REFERENCES document_templates(id) ON DELETE CASCADE,
    version        INTEGER      NOT NULL,
    layout         JSONB        NOT NULL,
    data_bindings  JSONB        NOT NULL DEFAULT '{}',
    styles         JSONB        NOT NULL DEFAULT '{}',
    notes          TEXT,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE(template_id, version)
);

CREATE TABLE IF NOT EXISTS generated_documents (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL REFERENCES tenants(id),
    template_id      UUID         NOT NULL REFERENCES document_templates(id),
    template_version INTEGER      NOT NULL,
    number           VARCHAR(50)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','FINALIZED','SENT','PAID','CANCELLED')),
    customer_id      UUID         REFERENCES leads(id),
    customer_data    JSONB        NOT NULL DEFAULT '{}',
    variables        JSONB        NOT NULL DEFAULT '{}',
    articles         JSONB        NOT NULL DEFAULT '[]',
    calculated       JSONB        NOT NULL DEFAULT '{}',
    html_content     TEXT,
    pdf_url          VARCHAR(255),
    generated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS document_number_sequences (
    tenant_id    UUID        PRIMARY KEY REFERENCES tenants(id),
    prefix       VARCHAR(10) NOT NULL DEFAULT 'DOC',
    next_number  INTEGER     NOT NULL DEFAULT 1,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_document_templates_tenant ON document_templates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_document_template_versions_template ON document_template_versions(template_id);
CREATE INDEX IF NOT EXISTS idx_generated_documents_tenant ON generated_documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_generated_documents_template ON generated_documents(template_id);
CREATE INDEX IF NOT EXISTS idx_generated_documents_status ON generated_documents(status);
