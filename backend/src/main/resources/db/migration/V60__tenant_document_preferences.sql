-- Preferències de lliurament de documents per tenant
CREATE TABLE tenant_document_preferences (
    tenant_id              UUID         PRIMARY KEY,
    language               VARCHAR(5)   NOT NULL DEFAULT 'ca',
    standard_doc_channel   VARCHAR(10)  NOT NULL DEFAULT 'WHATSAPP',
    sensitive_doc_channel  VARCHAR(10)  NOT NULL DEFAULT 'EMAIL',
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Overrides de plantilles per tenant (tenant_id NULL = plantilla global del sistema)
ALTER TABLE communication_templates
    ADD COLUMN tenant_id UUID;

CREATE INDEX idx_comm_tpl_tenant ON communication_templates (tenant_id)
    WHERE tenant_id IS NOT NULL;

-- Plantilles globals per al lliurament de documents (les 4 llengues × 2 accions × 2 canals)
INSERT INTO communication_templates (id, sector, language, channel, action, subject, body, is_active, sort_order, created_at, updated_at)
VALUES
-- ── DOCUMENT_STANDARD_DELIVERY ──────────────────────────────────────────────
-- WHATSAPP ca
(gen_random_uuid(), 'ALL', 'ca', 'WHATSAPP', 'DOCUMENT_STANDARD_DELIVERY', NULL,
'Hola {RECIPIENT_NAME},

el teu document *{DOCUMENT_NAME}* ja està disponible:
{VIEW_URL}

Vàlid fins al {EXPIRES_AT}.', true, 10, now(), now()),

-- EMAIL ca
(gen_random_uuid(), 'ALL', 'ca', 'EMAIL', 'DOCUMENT_STANDARD_DELIVERY',
'El teu document {DOCUMENT_NAME} ja és disponible',
'Hola {RECIPIENT_NAME},

El teu document "{DOCUMENT_NAME}" ja està disponible.

Accedeix aquí:
{VIEW_URL}

L''enllaç és vàlid fins al {EXPIRES_AT}.

Si tens cap pregunta, posa''t en contacte amb nosaltres.', true, 10, now(), now()),

-- WHATSAPP es
(gen_random_uuid(), 'ALL', 'es', 'WHATSAPP', 'DOCUMENT_STANDARD_DELIVERY', NULL,
'Hola {RECIPIENT_NAME},

tu documento *{DOCUMENT_NAME}* ya está disponible:
{VIEW_URL}

Válido hasta el {EXPIRES_AT}.', true, 10, now(), now()),

-- EMAIL es
(gen_random_uuid(), 'ALL', 'es', 'EMAIL', 'DOCUMENT_STANDARD_DELIVERY',
'Tu documento {DOCUMENT_NAME} ya está disponible',
'Hola {RECIPIENT_NAME},

Tu documento "{DOCUMENT_NAME}" ya está disponible.

Accede aquí:
{VIEW_URL}

El enlace es válido hasta el {EXPIRES_AT}.

Si tienes alguna pregunta, contáctanos.', true, 10, now(), now()),

-- WHATSAPP en
(gen_random_uuid(), 'ALL', 'en', 'WHATSAPP', 'DOCUMENT_STANDARD_DELIVERY', NULL,
'Hello {RECIPIENT_NAME},

your document *{DOCUMENT_NAME}* is ready:
{VIEW_URL}

Valid until {EXPIRES_AT}.', true, 10, now(), now()),

-- EMAIL en
(gen_random_uuid(), 'ALL', 'en', 'EMAIL', 'DOCUMENT_STANDARD_DELIVERY',
'Your document {DOCUMENT_NAME} is ready',
'Hello {RECIPIENT_NAME},

Your document "{DOCUMENT_NAME}" is now available.

Access it here:
{VIEW_URL}

The link is valid until {EXPIRES_AT}.

If you have any questions, please contact us.', true, 10, now(), now()),

-- WHATSAPP de
(gen_random_uuid(), 'ALL', 'de', 'WHATSAPP', 'DOCUMENT_STANDARD_DELIVERY', NULL,
'Hallo {RECIPIENT_NAME},

Ihr Dokument *{DOCUMENT_NAME}* ist bereit:
{VIEW_URL}

Gültig bis {EXPIRES_AT}.', true, 10, now(), now()),

-- EMAIL de
(gen_random_uuid(), 'ALL', 'de', 'EMAIL', 'DOCUMENT_STANDARD_DELIVERY',
'Ihr Dokument {DOCUMENT_NAME} ist bereit',
'Hallo {RECIPIENT_NAME},

Ihr Dokument „{DOCUMENT_NAME}" ist jetzt verfügbar.

Zugang hier:
{VIEW_URL}

Der Link ist gültig bis {EXPIRES_AT}.

Bei Fragen stehen wir gerne zur Verfügung.', true, 10, now(), now()),

-- ── DOCUMENT_SENSITIVE_DELIVERY ─────────────────────────────────────────────
-- WHATSAPP ca
(gen_random_uuid(), 'ALL', 'ca', 'WHATSAPP', 'DOCUMENT_SENSITIVE_DELIVERY', NULL,
'Hola {RECIPIENT_NAME},

tens un document confidencial disponible: *{DOCUMENT_NAME}*.

⚠️ Accés d''un sol ús — caduca en 72h:
{VIEW_URL}', true, 20, now(), now()),

-- EMAIL ca
(gen_random_uuid(), 'ALL', 'ca', 'EMAIL', 'DOCUMENT_SENSITIVE_DELIVERY',
'Document confidencial: {DOCUMENT_NAME}',
'Hola {RECIPIENT_NAME},

El teu document confidencial "{DOCUMENT_NAME}" ja és disponible.

IMPORTANT: Aquest document conté informació sensible. L''accés és d''un sol ús i caduca en 72 hores.

Accedeix aquí:
{VIEW_URL}

Per seguretat, no comparteixis aquest enllaç amb ningú.', true, 20, now(), now()),

-- WHATSAPP es
(gen_random_uuid(), 'ALL', 'es', 'WHATSAPP', 'DOCUMENT_SENSITIVE_DELIVERY', NULL,
'Hola {RECIPIENT_NAME},

tienes un documento confidencial disponible: *{DOCUMENT_NAME}*.

⚠️ Acceso de un solo uso — caduca en 72h:
{VIEW_URL}', true, 20, now(), now()),

-- EMAIL es
(gen_random_uuid(), 'ALL', 'es', 'EMAIL', 'DOCUMENT_SENSITIVE_DELIVERY',
'Documento confidencial: {DOCUMENT_NAME}',
'Hola {RECIPIENT_NAME},

Tu documento confidencial "{DOCUMENT_NAME}" ya está disponible.

IMPORTANTE: Este documento contiene información sensible. El acceso es de un solo uso y caduca en 72 horas.

Accede aquí:
{VIEW_URL}

Por seguridad, no compartas este enlace con nadie.', true, 20, now(), now()),

-- WHATSAPP en
(gen_random_uuid(), 'ALL', 'en', 'WHATSAPP', 'DOCUMENT_SENSITIVE_DELIVERY', NULL,
'Hello {RECIPIENT_NAME},

you have a confidential document available: *{DOCUMENT_NAME}*.

⚠️ Single-use access — expires in 72h:
{VIEW_URL}', true, 20, now(), now()),

-- EMAIL en
(gen_random_uuid(), 'ALL', 'en', 'EMAIL', 'DOCUMENT_SENSITIVE_DELIVERY',
'Confidential document: {DOCUMENT_NAME}',
'Hello {RECIPIENT_NAME},

Your confidential document "{DOCUMENT_NAME}" is now available.

IMPORTANT: This document contains sensitive information. Access is single-use and expires in 72 hours.

Access it here:
{VIEW_URL}

For security reasons, do not share this link with anyone.', true, 20, now(), now()),

-- WHATSAPP de
(gen_random_uuid(), 'ALL', 'de', 'WHATSAPP', 'DOCUMENT_SENSITIVE_DELIVERY', NULL,
'Hallo {RECIPIENT_NAME},

Sie haben ein vertrauliches Dokument: *{DOCUMENT_NAME}*.

⚠️ Einmaliger Zugang — läuft in 72h ab:
{VIEW_URL}', true, 20, now(), now()),

-- EMAIL de
(gen_random_uuid(), 'ALL', 'de', 'EMAIL', 'DOCUMENT_SENSITIVE_DELIVERY',
'Vertrauliches Dokument: {DOCUMENT_NAME}',
'Hallo {RECIPIENT_NAME},

Ihr vertrauliches Dokument „{DOCUMENT_NAME}" ist jetzt verfügbar.

WICHTIG: Dieses Dokument enthält sensible Informationen. Der Zugang ist einmalig und läuft in 72 Stunden ab.

Zugang hier:
{VIEW_URL}

Bitte teilen Sie diesen Link aus Sicherheitsgründen nicht weiter.', true, 20, now(), now());
