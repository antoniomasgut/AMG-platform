CREATE TABLE IF NOT EXISTS sector_templates (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    sector VARCHAR(30) NOT NULL,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    sort_order INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now() NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX idx_sector_templates_sector ON sector_templates(sector);
CREATE INDEX idx_sector_templates_type ON sector_templates(type);
CREATE INDEX idx_sector_templates_sector_type ON sector_templates(sector, type);
