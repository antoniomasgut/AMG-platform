-- Mòdul 33: Absence & Reschedule Cascade
CREATE TABLE IF NOT EXISTS absence_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    absence_date DATE NOT NULL,
    triggered_by BIGINT,
    affected_count INT NOT NULL DEFAULT 0,
    notified_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_absence_tenant_date ON absence_records(tenant_id, absence_date);
