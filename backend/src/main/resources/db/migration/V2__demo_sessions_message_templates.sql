CREATE TABLE IF NOT EXISTS demo_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token UUID UNIQUE NOT NULL,
    prospect_email VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    agent_context TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    blocked_at TIMESTAMPTZ,
    block_reason VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS message_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    subject VARCHAR(200),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
