-- V24: Enhance knowledge tables for Module 26 (RAG Knowledge Base)
-- Adds missing columns to knowledge_documents and adds constraints

ALTER TABLE knowledge_documents RENAME COLUMN content_text TO extracted_text;

ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS storage_path VARCHAR(500);
ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS file_size BIGINT;
ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS content_type VARCHAR(50);
ALTER TABLE knowledge_documents ADD COLUMN IF NOT EXISTS is_processed BOOLEAN DEFAULT false;

ALTER TABLE knowledge_documents ALTER COLUMN extracted_text SET DEFAULT '';
UPDATE knowledge_documents SET extracted_text = '' WHERE extracted_text IS NULL;
ALTER TABLE knowledge_documents ALTER COLUMN extracted_text SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'uk_knowledge_bases_tenant'
  ) THEN
    ALTER TABLE knowledge_bases ADD CONSTRAINT uk_knowledge_bases_tenant UNIQUE (tenant_id);
  END IF;
END $$;
