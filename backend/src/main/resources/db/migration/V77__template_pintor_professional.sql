-- Mòdul 04/05: Plantilla PINTOR/LACADOR — basada en Pintures i Lacats Toni Planas (Mallorca)
-- Inclou: actualització constraint block_type + inserció plantilla amb 7 blocs

-- 1. Ampliar CHECK constraint per a TRUST_BAR i CHAT_CTA
ALTER TABLE template_sections DROP CONSTRAINT IF EXISTS template_sections_block_type_check;
ALTER TABLE template_sections ADD CONSTRAINT template_sections_block_type_check CHECK (
    block_type::text = ANY (ARRAY[
        'HERO','TEXT','SERVICES','GALLERY','CONTACT_FORM','FAQ','TESTIMONIALS',
        'CTA','FOOTER','MAP','OPENING_HOURS','PRICING','TEAM','VIDEO','REVIEWS',
        'TRUST_BAR','CHAT_CTA'
    ]::text[])
);

-- 2. Plantilla
INSERT INTO landing_templates (id, name, slug, description, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    'Pintor / Lacador Professional',
    'pintor-professional',
    'Per a pintors i lacadors professionals. Inclou trust-bar, 6 serveis, testimonials reals i FAQ.',
    true,
    NOW(),
    NOW()
);

-- 3. Seccions (using CTE to reference the new template id)
WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'pintor-professional')

INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, '{}'::text, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text, $props$
{
  "title": "El pintor professional que necessites a Mallorca",
  "subtitle": "Serveis de pintura interior, lacat i façanes amb 30 anys d'experiència. Pressupost sense compromís en 24h.",
  "ctaText": "Demana pressupost gratis",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}
$props$),

  (2, 'TRUST_BAR'::text, $props$
{
  "items": [
    {"value": "30+", "label": "anys d'experiència", "icon": "⭐"},
    {"value": "31",  "label": "professionals en plantilla", "icon": "👷"},
    {"value": "450 m²", "label": "instal·lacions pròpies", "icon": "🏭"},
    {"value": "100%", "label": "garantia de satisfacció", "icon": "✅"}
  ]
}
$props$),

  (3, 'SERVICES'::text, $props$
{
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "home",
      "title": "Pintura interior",
      "description": "Pintem la teva vivenda, local o hotel amb total garantia de satisfacció. Preparem les superfícies i protegim tots els mobles."
    },
    {
      "icon": "award",
      "title": "Lacats i esmalts",
      "description": "Lacats en cabines pressuritzades d'última generació, úniques a Mallorca. Acabats perfectes en fusteria i mobiliari."
    },
    {
      "icon": "home",
      "title": "Façanes",
      "description": "Pintem façanes amb elevador i màximes garanties de seguretat. Tractaments impermeabilitzants i antihumitat."
    },
    {
      "icon": "leaf",
      "title": "Protecció de fusta",
      "description": "Tractaments a poro obert per a màxima durabilitat. Protegim portes, finestres, pergoles i tarimes exteriors."
    },
    {
      "icon": "shield",
      "title": "Sòls i revestiments",
      "description": "Impermeabilitzacions i paviments continus Kerakoll. Microciment, resines i sistemes de revestiment decoratiu."
    },
    {
      "icon": "star",
      "title": "Pintura decorativa",
      "description": "Efectes decoratius, empapelats i murals personalitzats. Donem vida als espais amb tècniques artesanals i modernes."
    }
  ]
}
$props$),

  (4, 'TESTIMONIALS'::text, $props$
{
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Christian S.",
      "text": "Serietat i qualitat en l'execució del treball. Recomanable al 100%. Van acabar en el temps previst i el resultat va ser excel·lent.",
      "avatarUrl": ""
    },
    {
      "name": "Catalina M.",
      "text": "Professionals i bona gent. Excel·lent. Van tractar la nostra casa com si fos la seva. Molt contents amb el resultat final.",
      "avatarUrl": ""
    },
    {
      "name": "Carsten M.",
      "text": "Very satisfied with the work and result. Beautiful done. The team was professional and clean throughout the whole project.",
      "avatarUrl": ""
    },
    {
      "name": "Lee C.",
      "text": "Highest of standards. No hesitation in recommending them. Punctual, professional and the finish is absolutely perfect.",
      "avatarUrl": ""
    }
  ]
}
$props$),

  (5, 'FAQ'::text, $props$
{
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "En quant temps reberé el pressupost?",
      "answer": "En menys de 24 hores laborables des que ens envieu les dades o ens visiteu. El pressupost és totalment gratuït i sense compromís."
    },
    {
      "question": "Podeu pintar mentre vivim a la casa?",
      "answer": "Sí. Treballem per fases i protegim tots els mobles i estris. Minimitzem les molèsties perquè podeu seguir vivint a casa durant l'obra."
    },
    {
      "question": "Quina garantia oferiu?",
      "answer": "Tots els treballs inclouen garantia de satisfacció. Si alguna cosa no queda com esperàveu, tornem i ho solucionem sense cost addicional."
    },
    {
      "question": "Treballeu amb particulars i empreses?",
      "answer": "Sí, atenem tant particulars (pisos, cases unifamiliars) com empreses (locals comercials, hotels, naus industrials i entitats públiques)."
    },
    {
      "question": "Quins tipus de pintura feu servir?",
      "answer": "Treballem amb les millors marques del mercat: Jotun, Valentine, Titanlux i Kerakoll. Sempre triem el producte més adequat per a cada superfície i ús."
    }
  ]
}
$props$),

  (6, 'CONTACT_FORM'::text, $props$
{
  "title": "Demana el teu pressupost gratuït",
  "subtitle": "Ens posem en contacte en menys de 24h"
}
$props$)

) AS bt(sort_order, block_type, default_props);
