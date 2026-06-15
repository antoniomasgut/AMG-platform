-- Historial d'activació de fases per tenant (traça auditada)
CREATE TABLE tenant_phase_activations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    phase VARCHAR(10) NOT NULL,           -- "F1", "F2", etc.
    source VARCHAR(50) NOT NULL,          -- "BUDGET_ACCEPTED", "MANUAL", "API"
    budget_id UUID,                       -- si ve d'un pressupost acceptat
    activated_by VARCHAR(255),            -- email de qui ha activat (null si automàtic)
    activated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deactivated_at TIMESTAMPTZ,           -- null = activa
    deactivated_by VARCHAR(255)
);
CREATE INDEX idx_tpa_tenant ON tenant_phase_activations(tenant_id);
CREATE INDEX idx_tpa_tenant_phase ON tenant_phase_activations(tenant_id, phase);
