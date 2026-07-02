-- V82: Bloc STEPS — afegir al constraint + inserir a les 7 plantilles actives

-- 1. Actualitzar constraint block_type per incloure STEPS
ALTER TABLE template_sections DROP CONSTRAINT IF EXISTS template_sections_block_type_check;
ALTER TABLE template_sections ADD CONSTRAINT template_sections_block_type_check CHECK (
    block_type::text = ANY (ARRAY[
        'HERO','TEXT','SERVICES','GALLERY','CONTACT_FORM','FAQ','TESTIMONIALS',
        'CTA','FOOTER','MAP','OPENING_HOURS','PRICING','TEAM','VIDEO','REVIEWS',
        'TRUST_BAR','CHAT_CTA','STEPS'
    ]::text[])
);

-- 2. Desplaçar sort_order 4,5,6,7 → 5,6,7,8 per fer lloc a STEPS (sort_order=4)
UPDATE template_sections
SET sort_order = sort_order + 1
WHERE sort_order >= 4
  AND template_id IN (SELECT id FROM landing_templates WHERE is_active = true);

-- 3. Inserir secció STEPS (sort_order=4) a cada plantilla activa
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT
  gen_random_uuid(),
  lt.id,
  'STEPS',
  4,
  '{"title":{"label":"Títol de la secció","type":"text","required":true,"placeholder":"Com treballem"},"items":{"label":"Passos (title + description)","type":"array","required":true}}',
  CASE lt.slug
    WHEN 'pintor-professional' THEN $p1${
  "title": "Com treballem",
  "items": [
    {"title": "Visita i pressupost gratuït", "description": "Venim a veure la feina i t'entreguem el pressupost en menys de 24h, sense compromís."},
    {"title": "Preparem l'espai", "description": "Protegim mobles, terres i obertures perquè no quedi cap resta de pintura fora del seu lloc."},
    {"title": "Executem l'obra", "description": "El nostre equip treballa de forma neta, ràpida i professional amb els millors materials."},
    {"title": "Entrega i garantia", "description": "Revisem junts el resultat. Si alguna cosa no t'agrada, ho arreglem de seguida."}
  ]
}$p1$
    WHEN 'perruqueria-professional' THEN $p2${
  "title": "La teva experiència amb nosaltres",
  "items": [
    {"title": "Reserva la teva cita", "description": "Truca'ns o escriu-nos per WhatsApp i et confirmem disponibilitat en minuts."},
    {"title": "Diagnosi personalitzada", "description": "Analitzem el teu tipus de cabell i escoltem el que busques per recomanar-te el millor servei."},
    {"title": "El teu servei", "description": "Tallem, coloram o tractem el teu cabell amb productes premium en un ambient relaxat."},
    {"title": "Marxes guapa/o", "description": "Et mostrem com estilitzar el resultat a casa i et recomanem els productes adequats."}
  ]
}$p2$
    WHEN 'fisioterapeuta-professional' THEN $p3${
  "title": "Com és el procés de tractament",
  "items": [
    {"title": "Primera visita", "description": "Fem una valoració completa de la teva lesió o dolor per establir el diagnòstic fisioterapèutic."},
    {"title": "Pla de tractament", "description": "Dissenyem un pla personalitzat amb les tècniques més adequades per al teu cas."},
    {"title": "Sessions de tractament", "description": "Apliquem les tècniques de fisioteràpia manual, electroterapia o exercici terapèutic."},
    {"title": "Seguiment i alta", "description": "Avaluem la teva evolució en cada sessió fins assolir els objectius marcats."}
  ]
}$p3$
    WHEN 'restaurant-professional' THEN $p4${
  "title": "La teva experiència al nostre restaurant",
  "items": [
    {"title": "Reserva taula", "description": "Fes la teva reserva per telèfon o WhatsApp i t'assegurem el millor racó del restaurant."},
    {"title": "Us rebem", "description": "Arriba al restaurant i l'equip de sala t'atendrà amb el somriure que ens caracteritza."},
    {"title": "Gaudeix de la carta", "description": "Productes frescos de mercat, cuinats al moment amb receptes tradicionals i tocs moderns."},
    {"title": "Repeteix", "description": "Més del 80% dels nostres clients tornen. Et guardem la teva taula preferida."}
  ]
}$p4$
    WHEN 'jardineria-professional' THEN $p5${
  "title": "Com treballem",
  "items": [
    {"title": "Visita i pressupost", "description": "Venim a veure el teu jardí o piscina i t'entreguem un pressupost detallat i gratuït."},
    {"title": "Disseny i planificació", "description": "Planifiquem els treballs per minimitzar molèsties i optimitzar el resultat final."},
    {"title": "Execució professional", "description": "El nostre equip treballa amb les millors eines i plantes de vivers de qualitat contrastada."},
    {"title": "Manteniment continu", "description": "T'oferim un servei de manteniment regular perquè el teu jardí estigui sempre perfecte."}
  ]
}$p5$
    WHEN 'reformes-professional' THEN $p6${
  "title": "Com gestionem la teva reforma",
  "items": [
    {"title": "Visita i pressupost", "description": "Venim a veure l'espai i t'entreguem un pressupost detallat en 48h, sense compromís."},
    {"title": "Projecte i llicències", "description": "Gestionem els permisos i la documentació necessària per a la teva reforma."},
    {"title": "Execució de l'obra", "description": "El nostre equip propi executa la reforma en els terminis acordats amb la màxima qualitat."},
    {"title": "Entrega i garantia", "description": "Revisem junts cada detall. Tots els treballs inclouen garantia d'1 any."}
  ]
}$p6$
    WHEN 'neteja-professional' THEN $p7${
  "title": "Com funciona el nostre servei",
  "items": [
    {"title": "Contacta amb nosaltres", "description": "Explica'ns les teves necessitats i et prepararem un pressupost personalitzat sense compromís."},
    {"title": "Primera visita", "description": "Visitem el teu espai per valorar la feina i acordar freqüència, horari i preu definitiu."},
    {"title": "Servei regular", "description": "El nostre equip arriba puntual, treballa amb eficàcia i usa productes de qualitat professional."},
    {"title": "Tu descansa", "description": "Nosaltres ens encarreguem de tot. Tu arribes a una llar o oficina perfectament neta."}
  ]
}$p7$
    ELSE $p0${
  "title": "Com treballem",
  "items": [
    {"title": "Contacte inicial", "description": "Posa't en contacte amb nosaltres i t'atendrem en menys de 24h."},
    {"title": "Pressupost gratuït", "description": "T'enviem un pressupost personalitzat sense cap compromís."},
    {"title": "Execució", "description": "El nostre equip professional s'encarrega de tot."},
    {"title": "Entrega i satisfacció", "description": "Revisem el resultat junts fins que estiguis completament satisfet."}
  ]
}$p0$
  END,
  NOW()
FROM landing_templates lt
WHERE lt.is_active = true;
