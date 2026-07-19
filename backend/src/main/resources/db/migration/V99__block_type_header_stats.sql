-- Mòdul 04/05: la constraint de block_type anava per darrere de l'enum BlockType
-- (faltaven HEADER i STATS) i el seeder de plantilles petava en BD noves.
ALTER TABLE template_sections DROP CONSTRAINT IF EXISTS template_sections_block_type_check;
ALTER TABLE template_sections ADD CONSTRAINT template_sections_block_type_check
  CHECK (block_type IN ('HERO','HEADER','TEXT','SERVICES','GALLERY','CONTACT_FORM','FAQ',
    'TESTIMONIALS','CTA','FOOTER','MAP','OPENING_HOURS','PRICING','TEAM','VIDEO','REVIEWS',
    'TRUST_BAR','CHAT_CTA','STEPS','STATS','BEFORE_AFTER'));
