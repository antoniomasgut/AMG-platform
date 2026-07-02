-- V81: color_schemes per plantilla + camp layout al props_schema del HERO

-- 1. Columna nova
ALTER TABLE landing_templates ADD COLUMN IF NOT EXISTS color_schemes TEXT;

-- 2. Afegir camp 'layout' al props_schema de tots els blocs HERO de totes les plantilles
UPDATE template_sections
SET props_schema = '{"title":{"label":"Títol principal","type":"text","required":true,"placeholder":"El títol principal de la landing"},"subtitle":{"label":"Subtítol","type":"text","required":false,"placeholder":"Descripció breu del negoci i proposta de valor"},"ctaText":{"label":"Botó principal","type":"text","required":true,"placeholder":"Demana pressupost gratuït"},"ctaUrl":{"label":"Enllaç botó principal","type":"text","required":false,"placeholder":"#contact"},"ctaSecondaryText":{"label":"Botó secundari","type":"text","required":false,"placeholder":"Veure serveis"},"ctaSecondaryUrl":{"label":"Enllaç botó secundari","type":"text","required":false,"placeholder":"#services"},"bgImage":{"label":"Imatge de fons (URL)","type":"url","required":false,"placeholder":"https://..."},"layout":{"label":"Distribució del hero","type":"select","options":["center","split","minimal"],"required":false,"placeholder":"center"}}'
WHERE block_type = 'HERO'
  AND template_id IN (SELECT id FROM landing_templates);

-- 3. Color schemes per plantilla (4 paletes cadascuna)

UPDATE landing_templates SET color_schemes = '[
  {"name":"Clàssic","primary":"#2c3e50","accent":"#e67e22","fontHeading":"Montserrat","fontBody":"Open Sans"},
  {"name":"Modern","primary":"#1a1a2e","accent":"#2980b9","fontHeading":"Poppins","fontBody":"Poppins"},
  {"name":"Artesà","primary":"#5d4037","accent":"#ff8f00","fontHeading":"Playfair Display","fontBody":"Lato"},
  {"name":"Fresc","primary":"#2d6a4f","accent":"#52b788","fontHeading":"Montserrat","fontBody":"Open Sans"}
]' WHERE slug = 'pintor-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Elegant","primary":"#1a1a2e","accent":"#c9a96e","fontHeading":"Playfair Display","fontBody":"Lato"},
  {"name":"Rosa","primary":"#880e4f","accent":"#f06292","fontHeading":"Cormorant Garamond","fontBody":"Lato"},
  {"name":"Minimalista","primary":"#212121","accent":"#bdbdbd","fontHeading":"Inter","fontBody":"Inter"},
  {"name":"Càlid","primary":"#bf360c","accent":"#ffab40","fontHeading":"Playfair Display","fontBody":"Lato"}
]' WHERE slug = 'perruqueria-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Professional","primary":"#1a5276","accent":"#2980b9","fontHeading":"Inter","fontBody":"Inter"},
  {"name":"Natural","primary":"#1b5e20","accent":"#66bb6a","fontHeading":"Poppins","fontBody":"Poppins"},
  {"name":"Modern","primary":"#1a237e","accent":"#7986cb","fontHeading":"Inter","fontBody":"Inter"},
  {"name":"Càlid","primary":"#4e342e","accent":"#a1887f","fontHeading":"Merriweather","fontBody":"Open Sans"}
]' WHERE slug = 'fisioterapeuta-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Tradicional","primary":"#5d4037","accent":"#ff6f00","fontHeading":"Playfair Display","fontBody":"Lato"},
  {"name":"Mediterrani","primary":"#1565c0","accent":"#ffb300","fontHeading":"Cormorant Garamond","fontBody":"Lato"},
  {"name":"Modern","primary":"#212121","accent":"#f44336","fontHeading":"Poppins","fontBody":"Poppins"},
  {"name":"Natural","primary":"#33691e","accent":"#c6a700","fontHeading":"Playfair Display","fontBody":"Open Sans"}
]' WHERE slug = 'restaurant-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Natural","primary":"#276749","accent":"#38a169","fontHeading":"Montserrat","fontBody":"Open Sans"},
  {"name":"Terra","primary":"#5d4037","accent":"#8bc34a","fontHeading":"Poppins","fontBody":"Open Sans"},
  {"name":"Fresc","primary":"#00695c","accent":"#26c6da","fontHeading":"Montserrat","fontBody":"Open Sans"},
  {"name":"Premium","primary":"#1b2a1c","accent":"#c6a700","fontHeading":"Cormorant Garamond","fontBody":"Lato"}
]' WHERE slug = 'jardineria-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Industrial","primary":"#2d3748","accent":"#e53e3e","fontHeading":"Montserrat","fontBody":"Open Sans"},
  {"name":"Elegant","primary":"#1a1a2e","accent":"#c9a96e","fontHeading":"Playfair Display","fontBody":"Lato"},
  {"name":"Modern","primary":"#0d47a1","accent":"#ff6f00","fontHeading":"Poppins","fontBody":"Poppins"},
  {"name":"Artesà","primary":"#4e342e","accent":"#ff8f00","fontHeading":"Montserrat","fontBody":"Open Sans"}
]' WHERE slug = 'reformes-professional';

UPDATE landing_templates SET color_schemes = '[
  {"name":"Professional","primary":"#2c5282","accent":"#63b3ed","fontHeading":"Inter","fontBody":"Inter"},
  {"name":"Fresc","primary":"#00695c","accent":"#4db6ac","fontHeading":"Poppins","fontBody":"Poppins"},
  {"name":"Modern","primary":"#1a1a2e","accent":"#00bcd4","fontHeading":"Inter","fontBody":"Inter"},
  {"name":"Amable","primary":"#6a1b9a","accent":"#ab47bc","fontHeading":"Poppins","fontBody":"Poppins"}
]' WHERE slug = 'neteja-professional';
