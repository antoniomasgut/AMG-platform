ALTER TABLE sector_phases
    ADD COLUMN IF NOT EXISTS ai_budget_eur_cents        INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN IF NOT EXISTS whatsapp_message_budget    INTEGER NOT NULL DEFAULT 200;

-- Valors per defecte per fase (iguals per a tots els sectors; l'admin pot ajustar)
UPDATE sector_phases SET ai_budget_eur_cents =  500, whatsapp_message_budget = 200 WHERE phase_number = 1;
UPDATE sector_phases SET ai_budget_eur_cents =  200, whatsapp_message_budget = 150 WHERE phase_number = 2;
UPDATE sector_phases SET ai_budget_eur_cents =  150, whatsapp_message_budget =  50 WHERE phase_number = 3;
UPDATE sector_phases SET ai_budget_eur_cents =   50, whatsapp_message_budget =  30 WHERE phase_number = 4;
UPDATE sector_phases SET ai_budget_eur_cents =   50, whatsapp_message_budget =   0 WHERE phase_number >= 5;
