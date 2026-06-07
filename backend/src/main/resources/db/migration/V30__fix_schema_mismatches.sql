-- Fix per totes les columnes i taules que falten per sync entre JPA i DB

-- === TAULES QUE FALTEN ===

-- Tenant team members (Mòdul 24 - Agent Activation Flows)
CREATE TABLE IF NOT EXISTS tenant_team_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    telegram_user_id BIGINT NOT NULL,
    first_name VARCHAR(100),
    interaction_count INT NOT NULL DEFAULT 0,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    upsell_sent_at TIMESTAMPTZ
);

-- Tenant Telegram configs (Mòdul 24 - Agent Activation Flows)
CREATE TABLE IF NOT EXISTS tenant_telegram_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    bot_token_encrypted VARCHAR(1000),
    bot_username VARCHAR(255),
    status VARCHAR(255) NOT NULL,
    webhook_registered BOOLEAN DEFAULT false,
    connected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- === COLUMNES QUE FALTEN ===

-- tenants: active_phases
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS active_phases TEXT;

-- landings: view_count
ALTER TABLE landings ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;

-- tenant_chat_links: meta_page_id
ALTER TABLE tenant_chat_links ADD COLUMN IF NOT EXISTS meta_page_id VARCHAR(30);

-- leads: missing columns from entity
ALTER TABLE leads ADD COLUMN IF NOT EXISTS converted_tenant_id UUID;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS has_whatsapp BOOLEAN;

-- === FIX TYPE MISMATCHES ===

-- leads.notes was erroneously created as OID instead of TEXT
ALTER TABLE leads ALTER COLUMN notes TYPE TEXT USING (notes::TEXT);

-- message_templates: sector
ALTER TABLE message_templates ADD COLUMN IF NOT EXISTS sector VARCHAR(50);
