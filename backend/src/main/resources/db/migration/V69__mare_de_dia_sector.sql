-- V69: Afegir sector MARE_DE_DIA als constraints i al catàleg de preus

-- 1. sector_pricing: actualitzar constraint de sector
ALTER TABLE sector_pricing DROP CONSTRAINT IF EXISTS sector_pricing_sector_check;
ALTER TABLE sector_pricing ADD CONSTRAINT sector_pricing_sector_check
    CHECK (sector IN (
        'PINTOR','ELECTRICISTA','FONTANER','JARDINER','NETEJA',
        'FISIOTERAPEUTA','PSICOLEG','NUTRICIONISTA',
        'PERRUQUERIA','ESTETICA',
        'GESTORIA','ACADEMIA',
        'TALLER_MECANIC','VETERINARI','PERRUQUERIA_CANINA',
        'RESTAURANTE','INMOBILIARIA',
        'AGENCIA_IA',
        'MARE_DE_DIA'
    ));

-- 2. tenants: actualitzar constraint de sector
ALTER TABLE tenants DROP CONSTRAINT IF EXISTS tenants_sector_check;
ALTER TABLE tenants ADD CONSTRAINT tenants_sector_check
    CHECK (sector IN (
        'PINTOR','ELECTRICISTA','FONTANER','JARDINER','NETEJA',
        'FISIOTERAPEUTA','PSICOLEG','NUTRICIONISTA',
        'PERRUQUERIA','ESTETICA',
        'GESTORIA','ACADEMIA',
        'TALLER_MECANIC','VETERINARI','PERRUQUERIA_CANINA',
        'RESTAURANTE','INMOBILIARIA',
        'AGENCIA_IA',
        'MARE_DE_DIA'
    ));

-- 3. tenant_activated_phases: actualitzar constraint de sector
ALTER TABLE tenant_activated_phases DROP CONSTRAINT IF EXISTS tenant_activated_phases_sector_check;
ALTER TABLE tenant_activated_phases ADD CONSTRAINT tenant_activated_phases_sector_check
    CHECK (sector IN (
        'PINTOR','ELECTRICISTA','FONTANER','JARDINER','NETEJA',
        'FISIOTERAPEUTA','PSICOLEG','NUTRICIONISTA',
        'PERRUQUERIA','ESTETICA',
        'GESTORIA','ACADEMIA',
        'TALLER_MECANIC','VETERINARI','PERRUQUERIA_CANINA',
        'RESTAURANTE','INMOBILIARIA',
        'AGENCIA_IA',
        'MARE_DE_DIA'
    ));

-- 4. Inserir preus (el seeder no re-sembra en producció si ja hi ha dades)
-- Columnes legacy monthly_* tenen NOT NULL; s'omplen amb 0 (no les usa l'entitat actual)
INSERT INTO sector_pricing
    (id, sector, business_size, setup_price, price_f1, price_f2, price_f3, price_f4, price_f5,
     setup_f2, setup_f3, setup_f4, setup_f5,
     monthly_complete, monthlyf1, monthly_f1f2, monthly_f1f2f3,
     created_at, updated_at)
SELECT
    gen_random_uuid(), sector, business_size, setup_price, price_f1, price_f2, price_f3, price_f4, price_f5,
    0, 0, 0, 0,
    0, 0, 0, 0,
    now(), now()
FROM (VALUES
    ('MARE_DE_DIA'::varchar, 'AUTONOMO'::varchar, 175::numeric, 59::numeric, 20::numeric, 20::numeric, 15::numeric, 15::numeric),
    ('MARE_DE_DIA'::varchar, 'PETIT'::varchar,    275::numeric, 89::numeric, 30::numeric, 30::numeric, 15::numeric, 15::numeric)
) AS t(sector, business_size, setup_price, price_f1, price_f2, price_f3, price_f4, price_f5)
WHERE NOT EXISTS (
    SELECT 1 FROM sector_pricing sp
    WHERE sp.sector = t.sector AND sp.business_size = t.business_size
);
