-- M50: pipeline_events — traçabilitat de transicions d'etapa per lead
CREATE TABLE pipeline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL REFERENCES leads(id),
    from_stage VARCHAR(30),
    to_stage VARCHAR(30) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL,
    actor_id UUID,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pipeline_events_lead ON pipeline_events(lead_id);
CREATE INDEX idx_pipeline_events_created ON pipeline_events(created_at DESC);

-- M50: camps de pipeline per lead
ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS pipeline_stage VARCHAR(30) NOT NULL DEFAULT 'PROSPECT',
    ADD COLUMN IF NOT EXISTS sla_deadline TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sla_alerted BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_leads_pipeline_stage ON leads(pipeline_stage);
CREATE INDEX idx_leads_sla_deadline ON leads(sla_deadline) WHERE sla_deadline IS NOT NULL;
