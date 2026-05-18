-- Migration 007: Conversational Agents (Part 2 - v2)
-- Taules per al sistema d'agents conversacionals amb Claude AI

BEGIN;

-- Add new columns to tenant_chat_links for conversational agent support
ALTER TABLE tenant_chat_links
    ADD COLUMN IF NOT EXISTS agent_mode VARCHAR(20) NOT NULL DEFAULT 'AUTO',
    ADD COLUMN IF NOT EXISTS whatsapp_phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS email_address VARCHAR(100);

-- Create conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id              BIGSERIAL PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    customer_identifier VARCHAR(100) NOT NULL,
    channel         VARCHAR(20) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    pending_approval BOOLEAN NOT NULL DEFAULT FALSE,
    approved_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for conversation queries
CREATE INDEX IF NOT EXISTS idx_conversations_tenant_customer_channel
    ON conversations(tenant_id, customer_identifier, channel);

CREATE INDEX IF NOT EXISTS idx_conversations_tenant_pending
    ON conversations(tenant_id, pending_approval)
    WHERE pending_approval = TRUE;

CREATE INDEX IF NOT EXISTS idx_conversations_tenant_created
    ON conversations(tenant_id, created_at DESC);

COMMIT;
