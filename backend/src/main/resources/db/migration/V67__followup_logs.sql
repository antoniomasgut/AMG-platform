CREATE TABLE followup_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    type        VARCHAR(20)  NOT NULL, -- 'APPOINTMENT' | 'SERVICE' | 'LEAD'
    entity_id   UUID         NOT NULL, -- booking_token.id o secure_document_token.id
    contact     VARCHAR(255),          -- telèfon o email enviat
    sent_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_fl_tenant ON followup_logs(tenant_id, sent_at DESC);
