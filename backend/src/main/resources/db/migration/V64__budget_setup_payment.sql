ALTER TABLE budgets
    ADD COLUMN IF NOT EXISTS setup_paid         BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS setup_stripe_session_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS setup_paid_at      TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS setup_nexe_phases  TEXT;
