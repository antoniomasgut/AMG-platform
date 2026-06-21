-- V70: Afegir fases del sector MARE_DE_DIA a sector_phases
-- El SectorPhaseSeeder no torna a córrer si ja hi ha dades, per això cal SQL manual

INSERT INTO sector_phases (id, sector, phase_number, name, description, dependency_type, required_phases, setup_price, monthly_price, ai_budget_eur_cents, whatsapp_message_budget)
VALUES
    (gen_random_uuid(), 'MARE_DE_DIA', 1, 'Gestió d''Inscripcions',
     'El pare/mare consulta places disponibles per WhatsApp → el bot recull les dades del nen i fa l''alta automàticament',
     'BASE', NULL, 79.00, 49.00, 500, 200),
    (gen_random_uuid(), 'MARE_DE_DIA', 2, 'Comunicació Diària amb Pares',
     'Al final del dia el bot envia resum d''activitat (àpats, son, jocs) als pares per WhatsApp',
     'OPTIONAL', '1', 49.00, 22.00, 200, 100),
    (gen_random_uuid(), 'MARE_DE_DIA', 3, 'Control d''Assistència i Horaris',
     'Els pares notifiquen absències i retards per WhatsApp → el bot actualitza el registre i notifica la professional per Telegram',
     'OPTIONAL', '1', 49.00, 18.00, 100, 50),
    (gen_random_uuid(), 'MARE_DE_DIA', 4, 'Facturació i Recordatoris de Pagament',
     'Recordatori mensual de pagament als pares per WhatsApp. Factura per email. Escala impagaments automàticament',
     'OPTIONAL', '1', 69.00, 18.00, 50, 30),
    (gen_random_uuid(), 'MARE_DE_DIA', 5, 'Seguiment i Informes del Nen',
     'La professional registra evolució per Telegram → el bot genera informe mensual i l''envia als pares per email',
     'OPTIONAL', '1,2', 69.00, 15.00, 50, 0),
    (gen_random_uuid(), 'MARE_DE_DIA', 6, 'Llista d''Espera i Reactivació',
     'Gestió automàtica de llista d''espera. Notificació quan es libera una plaça. Ofertes per temporada estival',
     'OPTIONAL', '1', 39.00, 12.00, 50, 0);
