-- Unificar el terra dels AUTONOMOS a 59€: PERRUQUERIA_CANINA era l'única a 49€
UPDATE sector_pricing SET price_f1 = 59.00
 WHERE sector = 'PERRUQUERIA_CANINA' AND business_size = 'AUTONOMO' AND price_f1 = 49.00;
