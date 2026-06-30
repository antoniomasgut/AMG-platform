-- V74: Social Publisher — entrada al catàleg de serveis
-- Setup 50€, 0€/mes (inclòs en F1). Requereix F1 (Telegram configurat).

INSERT INTO catalog_services (
    id, slug, name, description, type,
    cost, sale_price, monthly_price,
    is_addon, sort_order, version,
    created_at, updated_at
) VALUES (
    gen_random_uuid(),
    'social-publisher',
    'Social Publisher',
    'Publicació multi-xarxa (Instagram, Facebook, Google Business) des del Telegram del client. Captions generats per IA. Foto enviada directament pel xat. Historial al portal. Requereix F1.',
    'OTHER',
    50.00,   -- cost intern (1h eng × 50€/h)
    50.00,   -- preu de venda al client
    0.00,    -- sense mensual addicional (inclòs en F1)
    true,    -- és un add-on
    100,
    1,
    now(), now()
);
