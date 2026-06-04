CREATE TABLE IF NOT EXISTS visit_records (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    contact_identifier VARCHAR(100) NOT NULL,
    contact_name       VARCHAR(150),
    visit_date         DATE NOT NULL,
    treatment_type     VARCHAR(100),
    notes              VARCHAR(1000),
    next_visit_due     DATE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_visit_records_tenant_contact
    ON visit_records(tenant_id, contact_identifier);

CREATE INDEX IF NOT EXISTS idx_visit_records_tenant_date
    ON visit_records(tenant_id, visit_date);
