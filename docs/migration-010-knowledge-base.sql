-- Migration 010 — Knowledge Base + Contact memory
-- Run on production: psql -U amg -d amg -f migration-010-knowledge-base.sql

-- 1. Extend contacts table with memory fields
ALTER TABLE contacts
    ADD COLUMN IF NOT EXISTS conversation_summary TEXT,
    ADD COLUMN IF NOT EXISTS total_message_count  INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS summary_updated_at   TIMESTAMP WITH TIME ZONE;

-- 2. Knowledge bases (1:1 per tenant)
CREATE TABLE IF NOT EXISTS knowledge_bases (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL UNIQUE,
    version     INTEGER NOT NULL DEFAULT 1,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Knowledge entries (structured blocks)
CREATE TABLE IF NOT EXISTS knowledge_entries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id   UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    category            VARCHAR(50)  NOT NULL,
    entry_key           VARCHAR(100) NOT NULL,
    content             TEXT         NOT NULL,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (knowledge_base_id, entry_key)
);
CREATE INDEX IF NOT EXISTS idx_knowledge_entries_kb ON knowledge_entries(knowledge_base_id, category);

-- 4. Knowledge documents (text content, no binary storage for v1)
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id   UUID NOT NULL REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    filename            VARCHAR(255) NOT NULL,
    content_text        TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    uploaded_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
