-- Mòdul 04/05: 6 plantilles sectorials per a negocis locals a Mallorca
-- Sectors: perruqueria, fisioterapeuta, restaurant, jardineria, reformes, neteja

-- ============================================================
-- ESQUEMA PROPS REUTILITZABLE (comentari de referència)
-- HERO:         title, subtitle, ctaText, ctaUrl, ctaSecondaryText, ctaSecondaryUrl, bgImage
-- TRUST_BAR:    items[] {value, label, icon}
-- SERVICES:     title, items[] {icon, title, description}
-- TESTIMONIALS: title, items[] {name, text, avatarUrl}
-- FAQ:          title, items[] {question, answer}
-- CONTACT_FORM: title, subtitle
-- ============================================================


-- ============================================================
-- 1. PERRUQUERIA / BARBERIA
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Perruqueria / Barberia Professional',
  'perruqueria-professional',
  'Per a perruqueries i barberies. Colors elegants daurats i negre, 6 serveis, cita en línia.',
  true,
  '{"primaryColor":"#1a1a2e","accentColor":"#c9a96e","fontHeading":"Playfair Display","fontBody":"Lato"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'perruqueria-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s11${"title":{"label":"Títol principal","type":"text","required":true,"placeholder":"La perruqueria que et farà sentir únic/a"},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s11$,
  $p11${
  "title": "La perruqueria que et farà sentir únic/a a {{CITY}}",
  "subtitle": "Tall, color i tractaments amb els millors productes del mercat. Reserva la teva cita en línia i arriba amb tranquil·litat.",
  "ctaText": "Reserva la teva cita",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p11$),

  (2, 'TRUST_BAR'::text,
  $s12${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s12$,
  $p12${
  "items": [
    {"value": "15+",    "label": "anys d'experiència",   "icon": "⭐"},
    {"value": "2.000+", "label": "clients satisfets",    "icon": "💇"},
    {"value": "4.9★",   "label": "valoració Google",     "icon": "✅"},
    {"value": "60 min", "label": "temps mitjà de servei","icon": "⏱"}
  ]
}$p12$),

  (3, 'SERVICES'::text,
  $s13${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de serveis (icon, title, description)","type":"array","required":true}}$s13$,
  $p13${
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "scissors",
      "title": "Tall i pentinat",
      "description": "Tall personalitzat adaptat a la forma del teu rostre i el teu estil. Acabat amb eixugador i productes professionals."
    },
    {
      "icon": "award",
      "title": "Coloració i mecxes",
      "description": "Coloració global, mecxes californianes, balayage i tècniques de degradat. Usem tintes sense amoníac que respecten el cabell."
    },
    {
      "icon": "heart",
      "title": "Tractaments capil·lars",
      "description": "Màscares nutritives, queratina, tractament anticaiguda i hidratació profunda. El teu cabell brilla com el primer dia."
    },
    {
      "icon": "user",
      "title": "Afaitat clàssic",
      "description": "Afaitat tradicional amb navalla, toalla calenta i productes de barberia premium. Un ritual d'home per excel·lència."
    },
    {
      "icon": "star",
      "title": "Permanent i allisat",
      "description": "Permanent per a arrissar, definir o allisar. Resultats duradors que s'adapten a la teva rutina diària."
    },
    {
      "icon": "smile",
      "title": "Pentinats de festa i recollits",
      "description": "Pentinats per a casaments, comunions, festes i esdeveniments especials. Et deixem perfecta per al gran dia."
    }
  ]
}$p13$),

  (4, 'TESTIMONIALS'::text,
  $s14${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s14$,
  $p14${
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Maria T.",
      "text": "Porto anys venint aquí i mai no me'n vaig decebuda. M'escolten, m'assessoren i el resultat sempre supera el que esperava. Molt recomanable.",
      "avatarUrl": ""
    },
    {
      "name": "Jordi P.",
      "text": "El millor tall que m'han fet mai. Ràpid, net i professional. El tracte és molt proper i l'ambient és molt agradable. Hi tornaré sense dubte.",
      "avatarUrl": ""
    },
    {
      "name": "Antònia M.",
      "text": "Em van fer el color per al meu casament i va quedar perfecte. Van entendre exactament el que volia i el resultat va ser espectacular.",
      "avatarUrl": ""
    }
  ]
}$p14$),

  (5, 'FAQ'::text,
  $s15${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s15$,
  $p15${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Cal demanar cita prèvia?",
      "answer": "Recomanem demanar cita per garantir-te l'atenció a l'hora que prefereixis, però si vens sense cita intentem atendre't igualment segons disponibilitat."
    },
    {
      "question": "Quant costa un tall?",
      "answer": "Els preus varien segons el tipus de servei i la llargada del cabell. Et demanem que contactis amb nosaltres o vinguis al saló per rebre un pressupost personalitzat sense compromís."
    },
    {
      "question": "Quins productes feu servir?",
      "answer": "Treballem amb marques professionals de primera línia que respecten el cabell i el cuir cabellut. Prioritzem els productes lliures d'amoníac i els tractaments naturals."
    },
    {
      "question": "Quant dura una sessió de color?",
      "answer": "Depèn de la tècnica i la llargada del cabell. Una coloració global pot durar entre 1h30 i 2h. Les mecxes o balayage poden necessitar entre 2h i 3h30."
    },
    {
      "question": "Feu serveis per a nuvis i esdeveniments?",
      "answer": "Sí, oferim pentinats i maquillatge per a casaments, comunions i festes. Recomanem reservar amb antelació, especialment en temporada alta."
    }
  ]
}$p15$),

  (6, 'CONTACT_FORM'::text,
  $s16${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s16$,
  $p16${
  "title": "Reserva la teva cita",
  "subtitle": "Contacta'ns i et confirmem disponibilitat en menys de 24h"
}$p16$)

) AS bt(sort_order, block_type, props_schema, default_props);


-- ============================================================
-- 2. FISIOTERAPEUTA
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Fisioterapeuta Professional',
  'fisioterapeuta-professional',
  'Per a clíniques de fisioteràpia i fisioterapeutes autònoms. Colors blaus sanitaris, 6 especialitats.',
  true,
  '{"primaryColor":"#1a5276","accentColor":"#2980b9","fontHeading":"Inter","fontBody":"Inter"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'fisioterapeuta-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s21${"title":{"label":"Títol principal","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s21$,
  $p21${
  "title": "El teu fisioterapeuta de confiança a {{CITY}}",
  "subtitle": "Tractaments personalitzats per recuperar el teu benestar i tornar a fer el que t'agrada. Primera visita sense compromís.",
  "ctaText": "Demana la teva cita",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure tractaments",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p21$),

  (2, 'TRUST_BAR'::text,
  $s22${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s22$,
  $p22${
  "items": [
    {"value": "10+",    "label": "anys d'experiència",          "icon": "⭐"},
    {"value": "3.000+", "label": "pacients tractats",           "icon": "🏥"},
    {"value": "4.9★",   "label": "valoració Google",            "icon": "✅"},
    {"value": "48h",    "label": "primera cita disponible",     "icon": "📅"}
  ]
}$p22$),

  (3, 'SERVICES'::text,
  $s23${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de tractaments (icon, title, description)","type":"array","required":true}}$s23$,
  $p23${
  "title": "Els nostres tractaments",
  "items": [
    {
      "icon": "heart",
      "title": "Fisioteràpia manual",
      "description": "Tècniques de mobilització articular, teràpia miofascial i manipulació vertebral per alliberar el dolor i recuperar el moviment natural."
    },
    {
      "icon": "graduation-cap",
      "title": "Teràpia craniocervical i ATM",
      "description": "Tractament especialitzat de cefalees, cervicàlgies i disfuncions de l'articulació temporomandibular. Resultats visibles des de la primera sessió."
    },
    {
      "icon": "award",
      "title": "Rehabilitació esportiva",
      "description": "Recuperació d'esguinços, ruptures musculars i lesions de lligaments. Et tornem a la pista el més aviat possible amb el mínim risc de recaiguda."
    },
    {
      "icon": "shield",
      "title": "Pilates terapèutic",
      "description": "Pilates adaptat a les teves necessitats per millorar la postura, enfortir el sòl pelvià i prevenir lesions de columna. Grups reduïts."
    },
    {
      "icon": "zap",
      "title": "Electoteràpia i ultrasò",
      "description": "TENS, corrents interferencials, laser i ultrasò per accelerar la curació de teixits, reduir la inflamació i alleujar el dolor crònic."
    },
    {
      "icon": "user",
      "title": "Massatge terapèutic i drenatge",
      "description": "Massatge descontracturant, drenatge limfàtic manual i reflexologia podal. Ideal per a l'estrès, la retenció de líquids i la recuperació muscular."
    }
  ]
}$p23$),

  (4, 'TESTIMONIALS'::text,
  $s24${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s24$,
  $p24${
  "title": "El que diuen els nostres pacients",
  "items": [
    {
      "name": "Catalina B.",
      "text": "Vaig anar amb una lumbàlgia que em tenia sense poder caminar bé. En quatre sessions vaig notar un canvi brutal. Molt professional i molt proper.",
      "avatarUrl": ""
    },
    {
      "name": "Miquel A.",
      "text": "Porto mesos de tractament per una lesió de genoll i la millora és increïble. M'han explicat cada pas i ara puc tornar a córrer sense dolor.",
      "avatarUrl": ""
    },
    {
      "name": "Bàrbara F.",
      "text": "Les classes de pilates terapèutic m'han canviat la vida. La meva postura ha millorat moltíssim i les cervicàlgies pràcticament han desaparegut.",
      "avatarUrl": ""
    }
  ]
}$p24$),

  (5, 'FAQ'::text,
  $s25${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s25$,
  $p25${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Cobreix la seguretat social o la meva mútua?",
      "answer": "Treballem amb les principals mútues laborals i assegurances mèdiques. Si tens assegurança privada, consulta'ns i verificarem si tens cobertura. La Seguretat Social no cobreix els nostres serveis privats."
    },
    {
      "question": "Necessito recepta mèdica per venir?",
      "answer": "No és necessari. Pots venir directament sense recepta. En la primera visita fem una valoració completa i dissenyem el pla de tractament personalitzat."
    },
    {
      "question": "Quant dura una sessió?",
      "answer": "La primera visita dura entre 45 i 60 minuts, ja que inclou l'exploració i anamnesi. Les sessions de seguiment solen durar entre 30 i 45 minuts."
    },
    {
      "question": "Quantes sessions necessitaré?",
      "answer": "Depèn del tipus de lesió i del teu estat físic. En la primera visita et donarem una estimació. En general, les lesions agudes responen en 4-6 sessions; les cròniques poden necessitar més."
    },
    {
      "question": "Com puc demanar cita?",
      "answer": "Pots contactar-nos per telèfon, WhatsApp o omplir el formulari d'aquesta pàgina. Et confirmem disponibilitat en menys de 24 hores."
    }
  ]
}$p25$),

  (6, 'CONTACT_FORM'::text,
  $s26${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s26$,
  $p26${
  "title": "Demana la teva cita",
  "subtitle": "Et contactem en menys de 24h per confirmar dia i hora"
}$p26$)

) AS bt(sort_order, block_type, props_schema, default_props);


-- ============================================================
-- 3. RESTAURANT / CAFETERIA
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Restaurant / Cafeteria Professional',
  'restaurant-professional',
  'Per a restaurants, cafeteries i bars. Colors càlids marrons i taronges, cuina de mercat.',
  true,
  '{"primaryColor":"#5d4037","accentColor":"#ff6f00","fontHeading":"Playfair Display","fontBody":"Lato"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'restaurant-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s31${"title":{"label":"Títol principal","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s31$,
  $p31${
  "title": "La cuina que t'enamorarà a {{CITY}}",
  "subtitle": "Productes frescos, cuina de mercat i l'essència de la gastronomia local. Vine a gaudir d'una experiència que et farà tornar.",
  "ctaText": "Reserva la teva taula",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure la carta",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p31$),

  (2, 'TRUST_BAR'::text,
  $s32${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s32$,
  $p32${
  "items": [
    {"value": "15+",   "label": "anys de sabor",          "icon": "⭐"},
    {"value": "50+",   "label": "plats a la carta",       "icon": "🍽"},
    {"value": "4.8★",  "label": "valoració Google",       "icon": "✅"},
    {"value": "Diari", "label": "obert cada dia",         "icon": "📅"}
  ]
}$p32$),

  (3, 'SERVICES'::text,
  $s33${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de propostes (icon, title, description)","type":"array","required":true}}$s33$,
  $p33${
  "title": "La nostra proposta",
  "items": [
    {
      "icon": "star",
      "title": "Cuina de mercat i temporada",
      "description": "La nostra carta canvia amb les estacions. Treballem amb productors locals i productes de proximitat per oferir el millor de cada temporada."
    },
    {
      "icon": "check",
      "title": "Menú del dia",
      "description": "Menú complet de dilluns a divendres amb primer, segon, postres i beguda. Cuina casolana a un preu accessible per als que vénen cada dia."
    },
    {
      "icon": "award",
      "title": "Àpats d'empresa i grups",
      "description": "Menús especials per a dinars d'empresa, reunions i grups a partir de 10 persones. Et preparem una proposta personalitzada sense cost."
    },
    {
      "icon": "smile",
      "title": "Celebracions i esdeveniments",
      "description": "Aniversaris, casaments, comunions i celebracions familiars. Disposem de sala privada i t'ajudem a dissenyar el menú perfecte per al teu esdeveniment."
    },
    {
      "icon": "home",
      "title": "Terrassa i espai exterior",
      "description": "Gaudeix dels nostres plats a la terrassa en un ambient relaxat. Ideal per a dinars tranquils o per pendre un cafè a l'aire lliure."
    },
    {
      "icon": "location",
      "title": "Take away i recollida",
      "description": "Comanda els teus plats preferits per endur-te'ls a casa o a la feina. Ràpid, pràctic i amb la mateixa qualitat de sempre."
    }
  ]
}$p33$),

  (4, 'TESTIMONIALS'::text,
  $s34${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s34$,
  $p34${
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Joan S.",
      "text": "El menú del dia és excepcional per al preu que té. Cuina casolana de veritat, abundosa i ben feta. Vinc cada setmana des de fa anys.",
      "avatarUrl": ""
    },
    {
      "name": "Margalida R.",
      "text": "Vam fer el sopar de casament aquí i va ser perfecte. Ens van ajudar amb tot el menú, van ser molt professionals i el menjar estava deliciós.",
      "avatarUrl": ""
    },
    {
      "name": "Pere O.",
      "text": "La millor paella que he menjat en molt de temps. Productes frescos, serveis excel·lent i un ambient que convida a quedar-se. Molt recomanable.",
      "avatarUrl": ""
    }
  ]
}$p34$),

  (5, 'FAQ'::text,
  $s35${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s35$,
  $p35${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Cal reserva prèvia?",
      "answer": "Per a grups de més de 6 persones és imprescindible. Per a parelles o grups petits, recomanem reservar especialment els caps de setmana i festius per garantir taula."
    },
    {
      "question": "Teniu opcions per a al·lèrgies i intoleràncies?",
      "answer": "Sí. El nostre equip está format per gestionar al·lèrgies al gluten, lactosa, fruits secs i altres. Indiqueu-ho en reservar o en arribar i adaptarem els plats."
    },
    {
      "question": "Hi ha aparcament a prop?",
      "answer": "Disposem d'aparcament públic a menys de 200 metres. Pregunteu-nos en arribar per indicar-vos l'accés més còmode."
    },
    {
      "question": "A quina hora serviu el menú del dia i quin preu té?",
      "answer": "El menú del dia se serveix de dilluns a divendres d'1 del migdia a 3.30 de la tarda. Inclou primer, segon, postres i beguda. Consulteu-nos el preu actual."
    },
    {
      "question": "Podeu organitzar sopars per a grups grans?",
      "answer": "Sí, tenim sala privada per a grups. Dissenyem menús personalitzats per a tot tipus de celebracions. Contacteu-nos amb antelació per garantir disponibilitat."
    }
  ]
}$p35$),

  (6, 'CONTACT_FORM'::text,
  $s36${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s36$,
  $p36${
  "title": "Reserva la teva taula",
  "subtitle": "Digues-nos el dia, l'hora i quantes persones sou i t'ho confirmem"
}$p36$)

) AS bt(sort_order, block_type, props_schema, default_props);


-- ============================================================
-- 4. JARDINERIA / PISCINES
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Jardineria / Piscines Professional',
  'jardineria-professional',
  'Per a empreses de jardineria i manteniment de piscines. Colors verds naturals.',
  true,
  '{"primaryColor":"#276749","accentColor":"#38a169","fontHeading":"Montserrat","fontBody":"Open Sans"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'jardineria-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s41${"title":{"label":"Títol principal","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s41$,
  $p41${
  "title": "El jardí i la piscina que et mereixies a {{CITY}}",
  "subtitle": "Disseny, instal·lació i manteniment de jardins i piscines a tota la zona. Pressupost gratuït sense compromís.",
  "ctaText": "Demana pressupost gratuït",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p41$),

  (2, 'TRUST_BAR'::text,
  $s42${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s42$,
  $p42${
  "items": [
    {"value": "20+",  "label": "anys d'experiència",    "icon": "⭐"},
    {"value": "500+", "label": "jardins i piscines",    "icon": "🌿"},
    {"value": "4.9★", "label": "valoració Google",      "icon": "✅"},
    {"value": "48h",  "label": "resposta garantida",    "icon": "⚡"}
  ]
}$p42$),

  (3, 'SERVICES'::text,
  $s43${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de serveis (icon, title, description)","type":"array","required":true}}$s43$,
  $p43${
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "leaf",
      "title": "Disseny i creació de jardins",
      "description": "Dissenyem jardins a mida que s'adapten al teu espai, clima i estil de vida. Des del projecte fins a la plantació, ens encarreguem de tot."
    },
    {
      "icon": "check",
      "title": "Manteniment mensual de jardins",
      "description": "Servei de manteniment regular: tall de gespa, poda, adob, tractaments fitosanitaris i neteja general. El teu jardí sempre impecable."
    },
    {
      "icon": "home",
      "title": "Instal·lació i manteniment de piscines",
      "description": "Construcció de piscines a mida, manteniment setmanal de la qualitat de l'aigua, neteja de filtres i reparació d'avaries. Totalment a punt per a la temporada."
    },
    {
      "icon": "zap",
      "title": "Sistemes de reg automàtic",
      "description": "Instal·lem sistemes de reg per degoteig i aspersió programables. Estalvia aigua i temps mantenint les teves plantes sempre ben hidratades."
    },
    {
      "icon": "wrench",
      "title": "Neteja de terrenys i desbrossament",
      "description": "Neteja de solars, camps i zones verdes. Desbrossament manual i mecànic, retirada de restes i gestió de residus vegetals."
    },
    {
      "icon": "star",
      "title": "Plantes, arbres i flors",
      "description": "Subministrament i plantació d'espècies autòctones, arbres fruiters, plantes aromàtiques i flors de temporada. Assessorament inclòs."
    }
  ]
}$p43$),

  (4, 'TESTIMONIALS'::text,
  $s44${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s44$,
  $p44${
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Antònia G.",
      "text": "Ens van dissenyar el jardí de la nostra finca des de zero i el resultat és increïble. Molt professionals, nets i sempre puntuals. Els recomanem a tothom.",
      "avatarUrl": ""
    },
    {
      "name": "Rafel M.",
      "text": "Porten dos anys mantenint la nostra piscina i el jardí. Cap problema, sempre impecable. Quan he necessitat alguna cosa extra han respost de seguida.",
      "avatarUrl": ""
    },
    {
      "name": "Joana P.",
      "text": "Ens van instal·lar el reg automàtic i des d'aleshores no hem hagut de preocupar-nos per res. La nostra gespa és la millor del carrer.",
      "avatarUrl": ""
    }
  ]
}$p44$),

  (5, 'FAQ'::text,
  $s45${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s45$,
  $p45${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Amb quina freqüència feu el manteniment del jardí?",
      "answer": "Depèn del tipus de jardí i de la temporada. En general, oferim visites setmanals, quinzenals o mensuals. A l'estiu, la gespa pot necessitar visites més freqüents."
    },
    {
      "question": "Com funciona el pressupost?",
      "answer": "Venim a veure el jardí o la piscina sense cap cost, valorem l'estat i les necessitats, i et presentem un pressupost detallat sense compromís. Normalment en 48 hores."
    },
    {
      "question": "Piscina d'aigua salada o de clor, quina és millor?",
      "answer": "Ambdues opcions tenen avantatges. Les piscines d'aigua salada requereixen menys manteniment i són més suaus per a la pell. Les de clor solen tenir un cost inicial inferior. T'assessorem segons el teu cas."
    },
    {
      "question": "Recomaneu plantes autòctones?",
      "answer": "Sí, sempre que sigui possible prioritzem espècies autòctones de les Illes Balears. Requereixen menys aigua, s'adapten millor al clima i afavoreixen la biodiversitat local."
    },
    {
      "question": "Oferiu contracte anual de manteniment?",
      "answer": "Sí, tenim contractes anuals que inclouen totes les visites pactades, productes i mà d'obra. Un sol pagament mensual i sense sorpreses."
    }
  ]
}$p45$),

  (6, 'CONTACT_FORM'::text,
  $s46${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s46$,
  $p46${
  "title": "Demana el teu pressupost gratuït",
  "subtitle": "Ens posem en contacte en menys de 48h per concretar una visita sense cost"
}$p46$)

) AS bt(sort_order, block_type, props_schema, default_props);


-- ============================================================
-- 5. REFORMES / CONSTRUCCIÓ
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Reformes / Construcció Professional',
  'reformes-professional',
  'Per a empreses de reformes integrals i construcció. Colors grisos i vermell, equip propi.',
  true,
  '{"primaryColor":"#2d3748","accentColor":"#e53e3e","fontHeading":"Montserrat","fontBody":"Open Sans"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'reformes-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s51${"title":{"label":"Títol principal","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s51$,
  $p51${
  "title": "Reformes que transformen la teva llar a {{CITY}}",
  "subtitle": "Reformes integrals, cuines, banys i espais a mida. Equip propi, materials de qualitat i pressupost sense compromís en 48h.",
  "ctaText": "Demana pressupost en 48h",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p51$),

  (2, 'TRUST_BAR'::text,
  $s52${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s52$,
  $p52${
  "items": [
    {"value": "20+",  "label": "anys d'experiència",    "icon": "⭐"},
    {"value": "400+", "label": "obres completades",     "icon": "🏠"},
    {"value": "4.8★", "label": "valoració Google",      "icon": "✅"},
    {"value": "48h",  "label": "pressupost garantit",   "icon": "⚡"}
  ]
}$p52$),

  (3, 'SERVICES'::text,
  $s53${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de serveis (icon, title, description)","type":"array","required":true}}$s53$,
  $p53${
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "home",
      "title": "Reformes integrals",
      "description": "Transformem completament el teu habitatge o local: enderrocaments, distribució, instal·lacions, acabats. Un sol interlocutor per a tot el procés."
    },
    {
      "icon": "award",
      "title": "Cuines i banys",
      "description": "Disseny i reforma completa de cuines i banys. Selecció de materials, mobiliari a mida, taulells, sanitaris i instal·lació inclosa."
    },
    {
      "icon": "shield",
      "title": "Pintura i revestiments",
      "description": "Pintura d'interiors i exteriors, enguixat, alicatat, microciment i tècniques decoratives. Acabats perfectes amb productes de primera qualitat."
    },
    {
      "icon": "zap",
      "title": "Instal·lació elèctrica i fontaneria",
      "description": "Instal·lació i reforma d'instal·lacions elèctriques (amb certificat), fontaneria, calefacció i aire condicionat. Operaris propis homologats."
    },
    {
      "icon": "wrench",
      "title": "Paviments i terratzo",
      "description": "Col·locació de gres porcellànic, parquet, terratzo i paviments continus. Restauració de terres antics. Sempre amb garantia d'acabat."
    },
    {
      "icon": "check",
      "title": "Certificat energètic i llicències",
      "description": "Gestionem el certificat d'eficiència energètica i els tràmits administratius per a llicències d'obres. Et treiem la càrrega burocràtica."
    }
  ]
}$p53$),

  (4, 'TESTIMONIALS'::text,
  $s54${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s54$,
  $p54${
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Francisca T.",
      "text": "Ens van fer la reforma integral del pis en 6 setmanes. Treballen molt net, compliran els terminis i el resultat és millor del que imaginàvem. Totalment recomanables.",
      "avatarUrl": ""
    },
    {
      "name": "Bernat C.",
      "text": "La reforma del bany va ser impecable. Ens van assessorar en la tria de materials i el pressupost final es va ajustar exactament al que havien dit. Res a dir.",
      "avatarUrl": ""
    },
    {
      "name": "Margalida S.",
      "text": "Vam reformar la cuina sencera i estem encantats. Molt professionals, puntuals i amb molt bon tracte. El resultat és exactament el que volíem.",
      "avatarUrl": ""
    }
  ]
}$p54$),

  (5, 'FAQ'::text,
  $s55${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s55$,
  $p55${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Necessito llicència d'obres per a una reforma?",
      "answer": "Depèn del tipus d'obra. Les reformes puntuals (pintura, sanitaris, paviments) no solen necessitar llicència. Les que afecten l'estructura o l'exterior sí. Nosaltres t'assessorem i gestionem tot el necessari."
    },
    {
      "question": "Quant triga una reforma integral?",
      "answer": "Depèn de la superfície i la complexitat. Un bany sol trigar 2-3 setmanes; una reforma integral d'un pis de 80m², entre 6 i 10 setmanes. Et donem un calendari detallat abans de començar."
    },
    {
      "question": "Quina garantia oferiu en les obres?",
      "answer": "Totes les nostres obres inclouen garantia d'un any en mà d'obra i la garantia del fabricant en materials. Si apareix qualsevol incidència, tornem i ho resolem sense cost."
    },
    {
      "question": "Com funciona el pressupost?",
      "answer": "Venim a veure l'espai, agafem mides i t'enviem un pressupost detallat per partides en menys de 48h. Tot desglossat i sense lletra petita. Sense cap cost."
    },
    {
      "question": "Podem viure a casa mentre feu la reforma?",
      "answer": "En reformes parcials (cuina, bany, habitació) normalment sí. En reformes integrals recomanem no habitar el pis. Treballem per minimitzar el temps i les molèsties en tots els casos."
    }
  ]
}$p55$),

  (6, 'CONTACT_FORM'::text,
  $s56${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s56$,
  $p56${
  "title": "Demana el teu pressupost sense compromís",
  "subtitle": "Et contactem en menys de 48h per concertar una visita gratuïta"
}$p56$)

) AS bt(sort_order, block_type, props_schema, default_props);


-- ============================================================
-- 6. NETEJA / SERVEIS DE LLAR
-- ============================================================
INSERT INTO landing_templates (id, name, slug, description, is_active, default_styles, created_at, updated_at)
VALUES (
  gen_random_uuid(),
  'Neteja Professional / Serveis de Llar',
  'neteja-professional',
  'Per a empreses de neteja de pisos, oficines i comunitats. Colors blaus de confiança.',
  true,
  '{"primaryColor":"#2c5282","accentColor":"#63b3ed","fontHeading":"Inter","fontBody":"Inter"}',
  NOW(), NOW()
);

WITH tpl AS (SELECT id FROM landing_templates WHERE slug = 'neteja-professional')
INSERT INTO template_sections (id, template_id, block_type, sort_order, props_schema, default_props, created_at)
SELECT gen_random_uuid(), tpl.id, bt.block_type, bt.sort_order, bt.props_schema, bt.default_props, NOW()
FROM tpl, (VALUES

  (1, 'HERO'::text,
  $s61${"title":{"label":"Títol principal","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false},"ctaText":{"label":"Botó principal","type":"text","required":true},"ctaUrl":{"label":"Enllaç botó","type":"text","required":false},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false}}$s61$,
  $p61${
  "title": "Neteja professional per a la teva llar o empresa a {{CITY}}",
  "subtitle": "Servei de neteja regular, puntual i de confiança. Personal format, assegurat i de confiança. Pressupost personalitzat sense compromís.",
  "ctaText": "Demana pressupost gratuït",
  "ctaUrl": "#contact",
  "ctaSecondaryText": "Veure serveis",
  "ctaSecondaryUrl": "#services",
  "bgImage": ""
}$p61$),

  (2, 'TRUST_BAR'::text,
  $s62${"items":{"label":"Estadístiques (valor + etiqueta + icona emoji)","type":"array","required":true}}$s62$,
  $p62${
  "items": [
    {"value": "8+",   "label": "anys de servei",         "icon": "⭐"},
    {"value": "300+", "label": "clients regulars",       "icon": "🏠"},
    {"value": "5.0★", "label": "valoració Google",       "icon": "✅"},
    {"value": "24h",  "label": "resposta garantida",     "icon": "⚡"}
  ]
}$p62$),

  (3, 'SERVICES'::text,
  $s63${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Llista de serveis (icon, title, description)","type":"array","required":true}}$s63$,
  $p63${
  "title": "Els nostres serveis",
  "items": [
    {
      "icon": "home",
      "title": "Neteja de pisos i cases",
      "description": "Neteja regular o puntual de tota la llar: cuina, banys, dormitoris i espais comuns. Portem el material i els productes. Sense sorpreses."
    },
    {
      "icon": "shield",
      "title": "Neteja de comunitats i escales",
      "description": "Manteniment diari, setmanal o mensual de zones comunes, ascensors, garatges i pàrkings. Tenim contractes per a comunitats de veïns."
    },
    {
      "icon": "wrench",
      "title": "Neteja post-obra",
      "description": "Eliminació de pols de construcció, restes de materials, neteja de vidres i desinfecció completa. Deixem l'espai llest per habitar."
    },
    {
      "icon": "star",
      "title": "Neteja de moquetes i tapisseria",
      "description": "Neteja en profunditat de moquetes, catifes, sofàs i cadires amb maquinària especialitzada. Eliminem taques i àcars amb productes segurs."
    },
    {
      "icon": "award",
      "title": "Neteja d'oficines i locals comercials",
      "description": "Servei de neteja d'empreses, comerços i espais de treball. Horaris adaptats a la vostra activitat, fins i tot en horari nocturn o de cap de setmana."
    },
    {
      "icon": "check",
      "title": "Neteja de vidres i façanes",
      "description": "Neteja exterior de vidres, persianes, tendals i façanes. Disposem d'equips per a altures i accesos difícils amb totes les mesures de seguretat."
    }
  ]
}$p63$),

  (4, 'TESTIMONIALS'::text,
  $s64${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Testimonis (name, text, avatarUrl)","type":"array","required":true}}$s64$,
  $p64${
  "title": "El que diuen els nostres clients",
  "items": [
    {
      "name": "Isabel C.",
      "text": "Porto tres anys amb el servei setmanal i estic molt satisfeta. Sempre puntuals, molt acurades i donen molta atenció als detalls. Molt recomanables.",
      "avatarUrl": ""
    },
    {
      "name": "Tomàs B.",
      "text": "Els vam contractar per a la neteja post-obra del nostre local i van fer una feina excel·lent. En un dia va quedar impecable. Preus molt raonables.",
      "avatarUrl": ""
    },
    {
      "name": "Aina M.",
      "text": "Netegen les escales de la nostra comunitat cada setmana. Mai no ens hem hagut de queixar. Professionals, puntuals i amb molt bon tracte.",
      "avatarUrl": ""
    }
  ]
}$p64$),

  (5, 'FAQ'::text,
  $s65${"title":{"label":"Títol de la secció","type":"text","required":true},"items":{"label":"Preguntes (question + answer)","type":"array","required":true}}$s65$,
  $p65${
  "title": "Preguntes freqüents",
  "items": [
    {
      "question": "Feu servir productes ecològics?",
      "answer": "Sí, treballem preferentment amb productes ecològics i biodegradables que no perjudiquen la salut ni el medi ambient. Si tens al·lèrgies o preferències concretes, ens ho dius i ho adaptem."
    },
    {
      "question": "Com s'estableix el preu del servei?",
      "answer": "El preu depèn de la superfície, la freqüència i el tipus de servei. Et fem un pressupost personalitzat gratuït. En general, el servei regular és més econòmic que el puntual."
    },
    {
      "question": "El personal està assegurat?",
      "answer": "Sí, tot el nostre personal té contracte laboral, seguretat social i assegurança de responsabilitat civil. Ets completament cobert en cas de qualsevol incidència."
    },
    {
      "question": "Com sé que el personal és de confiança?",
      "answer": "Tot el personal passa per un procés de selecció, formació i verificació de referències. Treballem amb les mateixes persones de forma continuada per garantir la màxima confiança."
    },
    {
      "question": "Amb quina freqüència podeu venir i en quins horaris?",
      "answer": "Oferim servei diari, setmanal, quinzenal o mensual. Els horaris s'acorden amb el client i s'adapten a les seves necessitats. Treballem de dilluns a dissabte."
    }
  ]
}$p65$),

  (6, 'CONTACT_FORM'::text,
  $s66${"title":{"label":"Títol del formulari","type":"text","required":true},"subtitle":{"label":"Subtítol","type":"text","required":false}}$s66$,
  $p66${
  "title": "Demana el teu pressupost personalitzat",
  "subtitle": "Et contactem en menys de 24h amb una proposta a mida"
}$p66$)

) AS bt(sort_order, block_type, props_schema, default_props);
