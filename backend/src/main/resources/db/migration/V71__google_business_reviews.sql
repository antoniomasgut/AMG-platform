-- V71: Ressenyes de Google Business Profile (Spec 52 §12)
-- Cache local de ressenyes per renderitzar al bloc 'reviews' de les landings (source='google_business')

CREATE TABLE google_business_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    review_id VARCHAR(255) NOT NULL,
    author_name VARCHAR(255),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    reply TEXT,
    review_time TIMESTAMPTZ,
    synced_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (tenant_id, review_id)
);

CREATE INDEX idx_gbr_tenant ON google_business_reviews(tenant_id, rating DESC, review_time DESC);

-- Afegir location_id de Google Business Profile a la config de Google per tenant
ALTER TABLE google_module_configs
    ADD COLUMN IF NOT EXISTS business_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS business_location_id VARCHAR(255);
