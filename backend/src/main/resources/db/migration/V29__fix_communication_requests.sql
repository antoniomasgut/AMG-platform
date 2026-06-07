-- Add missing columns for retry logic (entity has them but DB doesn't)
ALTER TABLE communication_requests ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE communication_requests ADD COLUMN IF NOT EXISTS max_retries INT NOT NULL DEFAULT 3;

-- Embedding and vectorization columns for knowledge_entries
ALTER TABLE knowledge_entries ADD COLUMN IF NOT EXISTS embedding TEXT;
ALTER TABLE knowledge_entries ADD COLUMN IF NOT EXISTS is_vectorized BOOLEAN NOT NULL DEFAULT false;
