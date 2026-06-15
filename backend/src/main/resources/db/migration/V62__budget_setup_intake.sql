-- V62: Budget Setup Intake — fitxa de configuració vinculada a un pressupost
CREATE TABLE budget_setup_intakes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    tenant_name VARCHAR(255),
    sector VARCHAR(50),
    phase_numbers VARCHAR(50),
    token VARCHAR(64) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    intake_data TEXT,
    completed_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_bsi_budget ON budget_setup_intakes(budget_id);
CREATE INDEX idx_bsi_tenant ON budget_setup_intakes(tenant_id);
