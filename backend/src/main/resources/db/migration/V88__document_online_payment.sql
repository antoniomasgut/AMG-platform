-- Mòdul 53: cobrament online opcional a l'acceptació de documents F3
-- El pagament es traça al token de lliurament (Stripe del tenant, no d'AMG)
ALTER TABLE secure_document_tokens
  ADD COLUMN payment_status     VARCHAR(20),
  ADD COLUMN payment_session_id VARCHAR(120),
  ADD COLUMN payment_amount     NUMERIC(10,2),
  ADD COLUMN payment_paid_at    TIMESTAMPTZ;
