-- V7: Captura tots els canvis de schema fets manualment o via Hibernate DDL des de V1
-- Totes les sentències usen IF NOT EXISTS / IF EXISTS per ser idempotents

-- ─── tenants ──────────────────────────────────────────────────────────────────
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS contracted_phases TEXT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS agent_system_prompt TEXT;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS is_free BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS business_size VARCHAR(20);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS preferred_channel VARCHAR(20);

-- ─── tenant_ai_configs ────────────────────────────────────────────────────────
-- sender_email/sender_name coberts per V3; reply_to_email per V4
ALTER TABLE tenant_ai_configs ADD COLUMN IF NOT EXISTS reasoning_model VARCHAR(100);
ALTER TABLE tenant_ai_configs ADD COLUMN IF NOT EXISTS monthly_token_budget INT;
ALTER TABLE tenant_ai_configs ADD COLUMN IF NOT EXISTS budget_alert_threshold INT;

-- ─── sector_pricing ──────────────────────────────────────────────────────────
ALTER TABLE sector_pricing ADD COLUMN IF NOT EXISTS setup_f2 NUMERIC(10,2);
ALTER TABLE sector_pricing ADD COLUMN IF NOT EXISTS setup_f3 NUMERIC(10,2);
ALTER TABLE sector_pricing ADD COLUMN IF NOT EXISTS setup_f4 NUMERIC(10,2);
ALTER TABLE sector_pricing ADD COLUMN IF NOT EXISTS setup_f5 NUMERIC(10,2);

-- ─── budgets ─────────────────────────────────────────────────────────────────
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS recommendation TEXT;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS recommended_phase_ids TEXT;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS sector VARCHAR(50);
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS business_size VARCHAR(50);

-- ─── monthly_invoices ────────────────────────────────────────────────────────
ALTER TABLE monthly_invoices ADD COLUMN IF NOT EXISTS tenant_name VARCHAR(200);
ALTER TABLE monthly_invoices ADD COLUMN IF NOT EXISTS tenant_nif VARCHAR(50);
ALTER TABLE monthly_invoices ADD COLUMN IF NOT EXISTS tenant_address VARCHAR(500);
ALTER TABLE monthly_invoices ADD COLUMN IF NOT EXISTS tenant_email VARCHAR(200);

-- ─── contacts ────────────────────────────────────────────────────────────────
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS conversation_summary TEXT;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS total_message_count INT NOT NULL DEFAULT 0;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS summary_updated_at TIMESTAMPTZ;

-- ─── conversations ────────────────────────────────────────────────────────────
ALTER TABLE conversations ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ;

-- ─── whatsapp_waba_configs ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS whatsapp_waba_configs (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL UNIQUE,
    phone_number_id  VARCHAR(50)  NOT NULL,
    waba_id          VARCHAR(50),
    display_name     VARCHAR(100),
    access_token     TEXT,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DISCONNECTED',
    verified_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── nexe_service_configs ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS nexe_service_configs (
    tenant_id   UUID         NOT NULL,
    service_key VARCHAR(30)  NOT NULL,
    config_json TEXT,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, service_key)
);

-- ─── sector_phases ────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sector_phases (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    sector      VARCHAR(40)  NOT NULL,
    phase_key   VARCHAR(5)   NOT NULL,
    phase_name  VARCHAR(100),
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (sector, phase_key)
);

-- ─── budget_lines: monthly_price ─────────────────────────────────────────────
ALTER TABLE budget_lines ADD COLUMN IF NOT EXISTS monthly_price NUMERIC(10,2);
