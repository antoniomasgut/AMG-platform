-- V19: Millora system_settings amb tipus, valors per defecte i audit log

ALTER TABLE system_settings
  ADD COLUMN IF NOT EXISTS value_type VARCHAR(20) NOT NULL DEFAULT 'secret',
  ADD COLUMN IF NOT EXISTS default_value TEXT,
  ADD COLUMN IF NOT EXISTS validation_rules TEXT,
  ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS system_config_audit_log (
    id UUID PRIMARY KEY,
    config_key VARCHAR(80) NOT NULL,
    action VARCHAR(10) NOT NULL,
    previous_value TEXT,
    user_id UUID,
    user_email VARCHAR(150),
    ip VARCHAR(45),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scal_config_key ON system_config_audit_log(config_key);
CREATE INDEX IF NOT EXISTS idx_scal_changed_at ON system_config_audit_log(changed_at);
