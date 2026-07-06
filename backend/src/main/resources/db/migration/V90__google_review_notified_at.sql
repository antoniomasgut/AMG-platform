-- Mòdul 54 · Google Business Reviews → Telegram
-- Marca quan s'ha notificat el tenant d'una ressenya nova (NULL = pendent de notificar).
ALTER TABLE google_business_reviews ADD COLUMN IF NOT EXISTS notified_at TIMESTAMPTZ NULL;

-- Backfill: les ressenyes que ja existien són històric — es marquen com notificades
-- per no fer spam al tenant al primer run del scheduler horari.
UPDATE google_business_reviews SET notified_at = now() WHERE notified_at IS NULL;
