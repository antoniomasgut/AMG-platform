-- Llista de supressió global de prospecció (LSSI art. 21 / RGPD art. 21)
-- Un email donat de baixa no es torna a contactar mai, ni des de campanyes futures
CREATE TABLE prospect_optouts (
    email       VARCHAR(255) PRIMARY KEY,
    prospect_id UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
