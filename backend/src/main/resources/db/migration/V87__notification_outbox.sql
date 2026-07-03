-- Cua de reintents per a notificacions Telegram de la plataforma (bot AMG).
-- Si l'enviament falla (Telegram caigut, timeout), el missatge es persisteix
-- i un scheduler el reintenta — cap avís crític es perd en silenci.
CREATE TABLE notification_outbox (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chat_id      BIGINT NOT NULL,
    message      TEXT NOT NULL,
    reply_markup TEXT,
    attempts     INTEGER NOT NULL DEFAULT 0,
    status       VARCHAR(10) NOT NULL DEFAULT 'PENDING',
    last_error   VARCHAR(300),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at      TIMESTAMPTZ
);
CREATE INDEX idx_notification_outbox_pending ON notification_outbox (status, created_at);
