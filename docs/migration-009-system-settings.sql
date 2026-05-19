BEGIN;

CREATE TABLE IF NOT EXISTS system_settings (
    key             VARCHAR(80)  PRIMARY KEY,
    encrypted_value TEXT         NOT NULL,
    is_secret       BOOLEAN      NOT NULL DEFAULT TRUE,
    description     VARCHAR(255),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

COMMIT;
