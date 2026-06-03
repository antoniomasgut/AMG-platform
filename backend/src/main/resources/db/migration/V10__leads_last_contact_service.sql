-- Leads CRM: lastContactAt + lastServiceAt + LeadSource nous valors
ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS last_contact_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_service_at TIMESTAMPTZ;

-- Estendre el camp phone a 30 caràcters (números internacionals)
ALTER TABLE leads
    ALTER COLUMN phone TYPE VARCHAR(30);

-- Actualitzar el CHECK constraint de source per incloure nous valors
ALTER TABLE leads DROP CONSTRAINT IF EXISTS leads_source_check;
ALTER TABLE leads ADD CONSTRAINT leads_source_check
    CHECK (source IN (
        'WHATSAPP', 'WEB', 'LANDING_FORM', 'CHAT_WIDGET',
        'EMAIL', 'REFERRAL', 'MANUAL', 'OTHER'
    ));
