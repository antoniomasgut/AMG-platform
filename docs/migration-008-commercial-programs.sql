-- Migration 008: Commercial Programs (Early Adopter, Annual Contract, Referral)
-- Ampliació del mòdul Billing amb mecàniques comercials

BEGIN;

-- Add new columns to discounts table for program-based discounts
ALTER TABLE discounts
    ADD COLUMN IF NOT EXISTS program VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    ADD COLUMN IF NOT EXISTS applies_to_setup BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS applies_to_monthly BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_lifetime BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS commitment_months INTEGER NOT NULL DEFAULT 0;

-- Early Adopter Program configuration (singleton)
CREATE TABLE IF NOT EXISTS early_adopter_programs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    max_slots               INTEGER NOT NULL DEFAULT 20,
    used_slots              INTEGER NOT NULL DEFAULT 0,
    setup_discount_pct      INTEGER NOT NULL DEFAULT 100,
    monthly_discount_pct    INTEGER NOT NULL DEFAULT 30,
    commitment_months       INTEGER NOT NULL DEFAULT 6,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Insert default singleton row
INSERT INTO early_adopter_programs (max_slots, used_slots, setup_discount_pct, monthly_discount_pct, commitment_months, active)
VALUES (20, 0, 100, 30, 6, true)
ON CONFLICT DO NOTHING;

-- Referral codes
CREATE TABLE IF NOT EXISTS referral_codes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_tenant_id         UUID NOT NULL,
    code                    VARCHAR(20) NOT NULL UNIQUE,
    used_by_tenant_id       UUID,
    used_at                 TIMESTAMPTZ,
    referrer_credit_months  INTEGER NOT NULL DEFAULT 1,
    referred_setup_free     BOOLEAN NOT NULL DEFAULT TRUE,
    credit_applied          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_referral_codes_owner
    ON referral_codes(owner_tenant_id);

CREATE INDEX IF NOT EXISTS idx_referral_codes_code
    ON referral_codes(code);

COMMIT;
