-- Migration 006: Agents (Recordatori 24h via Telegram)
-- Taules per al sistema d'agents que substitueix n8n

BEGIN;

CREATE TABLE IF NOT EXISTS tenant_chat_links (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL UNIQUE,
    telegram_chat_id BIGINT,
    link_code       VARCHAR(20) UNIQUE,
    link_code_expires_at TIMESTAMPTZ,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS scheduled_agent_tasks (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    agent_slug      VARCHAR(50) NOT NULL,
    task_type       VARCHAR(50) NOT NULL,
    payload         TEXT,
    scheduled_at    TIMESTAMPTZ NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    executed_at     TIMESTAMPTZ,
    error_message   TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scheduled_agent_tasks_status
    ON scheduled_agent_tasks(status, scheduled_at);

COMMIT;
