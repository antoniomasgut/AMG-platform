-- Amplia el CHECK constraint de block_type a template_sections per incloure
-- tots els valors de l'enum BlockType.java
ALTER TABLE template_sections DROP CONSTRAINT IF EXISTS template_sections_block_type_check;
ALTER TABLE template_sections ADD CONSTRAINT template_sections_block_type_check CHECK (
    block_type::text = ANY (ARRAY[
        'HERO', 'TEXT', 'SERVICES', 'GALLERY', 'CONTACT_FORM',
        'FAQ', 'TESTIMONIALS', 'CTA', 'FOOTER', 'MAP',
        'OPENING_HOURS', 'PRICING', 'TEAM', 'VIDEO', 'REVIEWS'
    ]::text[])
);
