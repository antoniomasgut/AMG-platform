-- Dades entrevista al lead
ALTER TABLE leads ADD COLUMN IF NOT EXISTS interview_notes TEXT;
ALTER TABLE leads ADD COLUMN IF NOT EXISTS web_need VARCHAR(30);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS interview_sector VARCHAR(50);
ALTER TABLE leads ADD COLUMN IF NOT EXISTS interview_business_size VARCHAR(30);

-- Proposta comercial lligada a lead
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS lead_id UUID REFERENCES leads(id) ON DELETE SET NULL;
ALTER TABLE budgets ADD COLUMN IF NOT EXISTS offer_percent INTEGER;

CREATE INDEX IF NOT EXISTS idx_budget_lead_id ON budgets(lead_id) WHERE lead_id IS NOT NULL;
