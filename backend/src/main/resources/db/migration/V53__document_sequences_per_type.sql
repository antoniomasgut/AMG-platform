-- V53: redisseny document_number_sequences per tipus i any + afegir image_release

DROP TABLE IF EXISTS document_number_sequences;
CREATE TABLE document_number_sequences (
    tenant_id   UUID        NOT NULL REFERENCES tenants(id),
    doc_type    VARCHAR(20) NOT NULL,
    year        INTEGER     NOT NULL,
    next_number INTEGER     NOT NULL DEFAULT 1,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (tenant_id, doc_type, year)
);

ALTER TABLE document_templates
    DROP CONSTRAINT IF EXISTS document_templates_document_type_check;
ALTER TABLE document_templates
    ADD CONSTRAINT document_templates_document_type_check
    CHECK (document_type IN ('quote','invoice','delivery_note','contract','report','proposal','custom','image_release'));
