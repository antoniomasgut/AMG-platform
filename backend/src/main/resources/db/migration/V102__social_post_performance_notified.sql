-- P42: camp per evitar duplicats a la notificació de rendiment 24h post-publicació
ALTER TABLE social_posts
    ADD COLUMN IF NOT EXISTS performance_notified_at TIMESTAMPTZ;
