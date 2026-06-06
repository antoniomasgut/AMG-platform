-- V20: Fix column types in document builder tables
-- V18 created JSONB columns but entities use TEXT (ddl-auto:validate fails)
ALTER TABLE document_templates ALTER COLUMN data_bindings TYPE TEXT;
ALTER TABLE document_templates ALTER COLUMN layout TYPE TEXT;
ALTER TABLE document_templates ALTER COLUMN styles TYPE TEXT;
ALTER TABLE document_template_versions ALTER COLUMN data_bindings TYPE TEXT;
ALTER TABLE document_template_versions ALTER COLUMN layout TYPE TEXT;
ALTER TABLE document_template_versions ALTER COLUMN styles TYPE TEXT;
ALTER TABLE generated_documents ALTER COLUMN customer_data TYPE TEXT;
ALTER TABLE generated_documents ALTER COLUMN variables TYPE TEXT;
ALTER TABLE generated_documents ALTER COLUMN articles TYPE TEXT;
ALTER TABLE generated_documents ALTER COLUMN calculated TYPE TEXT;
