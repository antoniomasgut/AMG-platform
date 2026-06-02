CREATE TABLE channel_usage_logs (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  UUID         NOT NULL,
    channel    VARCHAR(20)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_channel_usage_tenant_ts ON channel_usage_logs (tenant_id, created_at);
