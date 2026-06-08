-- Afegir camp locale a landing_versions
ALTER TABLE landing_versions
    ADD COLUMN IF NOT EXISTS locale VARCHAR(5) NOT NULL DEFAULT 'ca';

-- Eliminar la restricció d'unicitat antiga (landing_id, version_number)
ALTER TABLE landing_versions
    DROP CONSTRAINT IF EXISTS landing_versions_landing_id_version_number_key;

-- Nova restricció: una landing pot tenir múltiples versions per idioma
ALTER TABLE landing_versions
    ADD CONSTRAINT uq_landing_version_locale
    UNIQUE (landing_id, locale, version_number);
