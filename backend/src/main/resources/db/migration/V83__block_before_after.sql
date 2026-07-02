-- Mòdul 04/05: Bloc Before/After
ALTER TABLE template_sections DROP CONSTRAINT IF EXISTS template_sections_block_type_check;
ALTER TABLE template_sections ADD CONSTRAINT template_sections_block_type_check
  CHECK (block_type IN ('HERO','TEXT','SERVICES','GALLERY','CONTACT_FORM','FAQ','TESTIMONIALS',
    'CTA','FOOTER','MAP','OPENING_HOURS','PRICING','TEAM','VIDEO','REVIEWS','TRUST_BAR',
    'CHAT_CTA','STEPS','BEFORE_AFTER'));
