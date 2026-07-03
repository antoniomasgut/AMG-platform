-- Mòdul 04/05: plantilla sectorial per a MARES DE DIA (criança en llar familiar)
-- Estructura inspirada en les webs del sector (qui soc / què és una mare de dia /
-- metodologia / què ofereixo / fotos / contacte) modernitzada al format de blocs.

INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, color_schemes, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Mare de Dia Professional',
  'mare-de-dia-professional',
  'Per a mares de dia i criança en llar familiar. Colors càlids i acollidors, valors de criança respectuosa, rutina del dia i FAQ educativa.',
  true,
  '{"primaryColor":"#7a5c50","accentColor":"#e9a178","fontHeading":"Josefin Sans","fontBody":"Nunito"}',
  '[
    {"name":"Càlid",  "primary":"#7a5c50","accent":"#e9a178","fontHeading":"Josefin Sans","fontBody":"Nunito","bg":"#fdf9f5","text":"#3d2f28"},
    {"name":"Sàlvia", "primary":"#5c7a5e","accent":"#d9a05b","fontHeading":"Nunito","fontBody":"Nunito","bg":"#f7faf6","text":"#2f3d30"},
    {"name":"Rosa",   "primary":"#9d5c63","accent":"#e8b4bc","fontHeading":"Cormorant Garamond","fontBody":"Lato","bg":"#fdf7f8","text":"#3d282b"},
    {"name":"Cel",    "primary":"#4a6d8c","accent":"#f4b860","fontHeading":"Poppins","fontBody":"Nunito","bg":"#f6f9fc","text":"#26333d"}
  ]',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'mare-de-dia-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s91${"title":{"label":"Títol principal","type":"text","required":true,"placeholder":"Una llar on créixer acompanyat"},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"ctaSecondaryUrl":{"label":"Enllaç botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false},"layout":{"label":"Distribució del hero","type":"select","options":["center","split","minimal"],"required":false,"placeholder":"split"}}$s91$,
  $p91${
  "title": "Una llar on créixer, jugar i sentir-se estimat",
  "subtitle": "{{BUSINESS_NAME}} és una llar de criança a {{CITY}}: un espai petit, càlid i preparat on el teu infant creix acompanyat, respectant el seu ritme. Places molt limitades.",
  "ctaText": "Vine a conèixer-nos",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Com és un dia aquí",
  "ctaSecondaryUrl": "#steps",
  "bgImage": "",
  "layout": "split"
}$p91$),

  (2, 'TRUST_BAR'::text,
  $s92${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s92$,
  $p92${
  "items": [
    {"value": "màx. 4", "label": "infants alhora",        "icon": "🧸"},
    {"value": "0-3",    "label": "anys d'edat",           "icon": "👶"},
    {"value": "100%",   "label": "ambient de llar",       "icon": "🏡"},
    {"value": "flexible","label": "horaris adaptats",     "icon": "🕐"}
  ]
}$p92$),

  (3, 'SERVICES'::text,
  $s93${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de serveis (icon, title, description)","type":"array","required":true}}$s93$,
  $p93${
  "title": "Què hi trobaràs a ca nostra",
  "items": [
    {
      "icon": "home",
      "title": "Ambient de llar, no d'escola",
      "description": "Un espai domèstic adaptat i segur, pensat perquè els infants se sentin com a casa: llum natural, materials nobles i racons de joc al seu abast."
    },
    {
      "icon": "users",
      "title": "Grup molt reduït",
      "description": "Un màxim de 4 infants alhora. Això em permet conèixer de veritat cada nin i nina, i donar-li l'atenció individual que necessita a cada moment."
    },
    {
      "icon": "heart",
      "title": "Criança respectuosa",
      "description": "Acompanyo sense presses ni pressions: respecte pel ritme de cada infant, pel seu moviment lliure i per les seves emocions. Molt d'amor i cap crit."
    },
    {
      "icon": "sun",
      "title": "Joc lliure i natura",
      "description": "El joc és la feina dels infants. Materials no estructurats, experimentació i sortides a l'aire lliure sempre que el temps ho permet."
    },
    {
      "icon": "coffee",
      "title": "Alimentació casolana",
      "description": "Menjar cuinat a casa el mateix dia, amb producte fresc i de proximitat. M'adapto a al·lèrgies, intoleràncies i a l'etapa de cada infant."
    },
    {
      "icon": "message-circle",
      "title": "Acompanyament a les famílies",
      "description": "Comunicació diària de com ha anat el dia, fotos dels moments especials i una porta sempre oberta per parlar del que necessiteu."
    }
  ]
}$p93$),

  (4, 'STEPS'::text,
  $s94${"title":{"label":"Títol de la secció","type":"text","required":true,"placeholder":"Un dia a ca nostra"},"items":{"label":"Passos (title + description)","type":"array","required":true}}$s94$,
  $p94${
  "title": "Un dia a ca nostra",
  "items": [
    {"title": "Arribada tranquil·la", "description": "Cada família arriba al seu ritme. Ens saludem sense presses i l'infant s'incorpora al joc quan se sent preparat."},
    {"title": "Joc lliure", "description": "El moment més important del dia: exploració, moviment i joc amb materials pensats per a la seva etapa."},
    {"title": "Berenar i cura", "description": "Berenar saludable tots junts, canvi de bolquers i moments de cura individual amb calma i respecte."},
    {"title": "Sortida o activitat", "description": "Si el temps acompanya, sortim a passejar o jugar a l'aire lliure. Si no, música, contes o experimentació a dins."},
    {"title": "Dinar casolà i descans", "description": "Dinar cuinat a casa i migdiada en un ambient tranquil. A la tarda, recollida amb un resum de com ha anat el dia."}
  ]
}$p94$),

  (5, 'TESTIMONIALS'::text,
  $s95${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text)","type":"array","required":true}}$s95$,
  $p95${
  "title": "Famílies que hi han confiat",
  "items": [
    {
      "name": "Marta i Joan, pares d'en Pau",
      "text": "En Pau hi va cada dia content i torna feliç. Es nota que no és una guarderia: és una segona casa. La tranquil·litat que ens dona saber-lo tan ben acompanyat no té preu."
    },
    {
      "name": "Laura, mare de na Júlia",
      "text": "El que més valorem és el tracte individual. Na Júlia és tímida i aquí ha florit: li han respectat el seu ritme des del primer dia. La comunicació diària és meravellosa."
    },
    {
      "name": "Aina i Marc, pares d'en Biel",
      "text": "Vam visitar moltes opcions i aquesta va ser l'única on vam sortir amb el cor tranquil. Grups petits, menjar de veritat i una persona que estima el que fa. Totalment recomanable."
    }
  ]
}$p95$),

  (6, 'FAQ'::text,
  $s96${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question, answer)","type":"array","required":true}}$s96$,
  $p96${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Què és exactament una mare de dia?",
      "answer": "Som professionals de l'educació infantil que acollim un grup molt reduït d'infants (màxim 4) a la nostra pròpia llar, adaptada i preparada per a ells. És una alternativa a l'escoleta: mateixa professionalitat, però en un ambient familiar, amb atenció individualitzada i ràtios que cap centre pot oferir."
    },
    {
      "question": "Quines edats acolliu i quantes places teniu?",
      "answer": "Acollim infants de 0 a 3 anys, amb un màxim de 4 alhora. Les places són molt limitades precisament perquè l'atenció individual és la base del projecte — contacta'ns per saber la disponibilitat actual."
    },
    {
      "question": "Quins horaris feu?",
      "answer": "L'horari base és de matí, però ens adaptem a les necessitats de cada família sempre que sigui possible. Explica'ns la teva situació i buscarem la fórmula que us funcioni."
    },
    {
      "question": "Com funciona el menjar?",
      "answer": "El menjar es cuina a casa el mateix dia amb producte fresc. Ens adaptem a al·lèrgies, intoleràncies i a l'etapa d'alimentació de cada infant (BLW, triturats...). Els menús es comparteixen amb les famílies."
    },
    {
      "question": "Com és el període d'adaptació?",
      "answer": "Gradual i respectuós, sense fórmules rígides: els primers dies la família hi és present i anem allargant les estones segons com se senti l'infant. Cada nin marca el seu ritme i això es respecta sempre."
    },
    {
      "question": "Podem venir a conèixer l'espai abans de decidir?",
      "answer": "I tant — de fet és el que recomanem sempre. Vine amb el teu infant, coneix l'espai, fes totes les preguntes que necessitis i comprova com s'hi sent. Truca'ns o escriu-nos per WhatsApp al {{PHONE}} i quedem."
    }
  ]
}$p96$),

  (7, 'CONTACT_FORM'::text,
  $s97${"title":{"label":"Títol de la secció","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s97$,
  $p97${
  "title": "Vine a conèixer-nos",
  "subtitle": "Escriu-nos i quedem un dia perquè vingueu a veure l'espai amb el vostre infant. Sense compromís — les decisions importants es prenen amb calma."
}$p97$)

) AS bt(sort_order, block_type, props_schema, default_props);
