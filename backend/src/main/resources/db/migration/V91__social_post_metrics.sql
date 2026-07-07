-- Mòdul 55 feature 2: mètriques d'engagement dels posts socials
ALTER TABLE social_posts ADD COLUMN IF NOT EXISTS reach INTEGER NULL;
ALTER TABLE social_posts ADD COLUMN IF NOT EXISTS likes INTEGER NULL;
ALTER TABLE social_posts ADD COLUMN IF NOT EXISTS comments INTEGER NULL;
ALTER TABLE social_posts ADD COLUMN IF NOT EXISTS metrics_synced_at TIMESTAMPTZ NULL;
