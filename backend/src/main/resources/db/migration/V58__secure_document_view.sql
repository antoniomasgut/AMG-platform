ALTER TABLE secure_document_tokens
    ADD COLUMN document_type      VARCHAR(30)  NOT NULL DEFAULT 'GENERIC',
    ADD COLUMN source_entity_id   UUID,
    ADD COLUMN source_entity_type VARCHAR(30),
    ADD COLUMN accepted_at        TIMESTAMPTZ,
    ADD COLUMN signer_name        VARCHAR(255),
    ADD COLUMN signer_ip          VARCHAR(45);
