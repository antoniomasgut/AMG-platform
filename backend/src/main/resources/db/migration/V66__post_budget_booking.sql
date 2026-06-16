-- Mòdul 45: Post-Budget Booking (F3 → F2 integration)

-- 1. booking_tokens: camps addicionals per reserva post-pressupost
ALTER TABLE booking_tokens
    ALTER COLUMN lead_id DROP NOT NULL,
    ADD COLUMN source_document_id UUID,
    ADD COLUMN recipient_phone    VARCHAR(30),
    ADD COLUMN recipient_name     VARCHAR(255),
    ADD COLUMN booking_label      VARCHAR(100);

-- 2. tenant_document_preferences: activació automàtica de reserva en acceptar
ALTER TABLE tenant_document_preferences
    ADD COLUMN auto_booking_on_accept BOOLEAN NOT NULL DEFAULT true;

-- 3. Plantilles BOOKING_INVITATION (4 idiomes × 2 canals = 8 files)
INSERT INTO communication_templates (id, sector, language, channel, action, subject, body, is_active, sort_order, created_at, updated_at, tenant_id)
VALUES

-- WHATSAPP ca
(gen_random_uuid(), 'ALL', 'ca', 'WHATSAPP', 'BOOKING_INVITATION', NULL,
'Hola {RECIPIENT_NAME},

has acceptat el pressupost *{DOCUMENT_NAME}*. Gràcies!

Ara pots {BOOKING_LABEL} aquí:
{BOOKING_URL}

L''enllaç és vàlid 14 dies.', true, 30, now(), now(), NULL),

-- EMAIL ca
(gen_random_uuid(), 'ALL', 'ca', 'EMAIL', 'BOOKING_INVITATION',
'Reserva la teva cita — {DOCUMENT_NAME}',
'Hola {RECIPIENT_NAME},

Hem rebut la teva acceptació del pressupost "{DOCUMENT_NAME}". Gràcies per confiar en nosaltres!

Per confirmar la data, pots {BOOKING_LABEL} a continuació:
{BOOKING_URL}

L''enllaç caduca en 14 dies. Si tens cap dubte, posa''t en contacte amb nosaltres.', true, 30, now(), now(), NULL),

-- WHATSAPP es
(gen_random_uuid(), 'ALL', 'es', 'WHATSAPP', 'BOOKING_INVITATION', NULL,
'Hola {RECIPIENT_NAME},

has aceptado el presupuesto *{DOCUMENT_NAME}*. ¡Gracias!

Ahora puedes {BOOKING_LABEL} aquí:
{BOOKING_URL}

El enlace es válido 14 días.', true, 30, now(), now(), NULL),

-- EMAIL es
(gen_random_uuid(), 'ALL', 'es', 'EMAIL', 'BOOKING_INVITATION',
'Reserva tu cita — {DOCUMENT_NAME}',
'Hola {RECIPIENT_NAME},

Hemos recibido tu aceptación del presupuesto "{DOCUMENT_NAME}". ¡Gracias por confiar en nosotros!

Para confirmar la fecha, puedes {BOOKING_LABEL} a continuación:
{BOOKING_URL}

El enlace caduca en 14 días. Si tienes alguna duda, no dudes en contactarnos.', true, 30, now(), now(), NULL),

-- WHATSAPP en
(gen_random_uuid(), 'ALL', 'en', 'WHATSAPP', 'BOOKING_INVITATION', NULL,
'Hi {RECIPIENT_NAME},

you have accepted the quote *{DOCUMENT_NAME}*. Thank you!

You can now {BOOKING_LABEL} here:
{BOOKING_URL}

The link is valid for 14 days.', true, 30, now(), now(), NULL),

-- EMAIL en
(gen_random_uuid(), 'ALL', 'en', 'EMAIL', 'BOOKING_INVITATION',
'Book your appointment — {DOCUMENT_NAME}',
'Hi {RECIPIENT_NAME},

We have received your acceptance of the quote "{DOCUMENT_NAME}". Thank you for your trust!

To confirm the date, you can {BOOKING_LABEL} below:
{BOOKING_URL}

The link expires in 14 days. If you have any questions, please contact us.', true, 30, now(), now(), NULL),

-- WHATSAPP de
(gen_random_uuid(), 'ALL', 'de', 'WHATSAPP', 'BOOKING_INVITATION', NULL,
'Hallo {RECIPIENT_NAME},

Sie haben den Kostenvoranschlag *{DOCUMENT_NAME}* akzeptiert. Vielen Dank!

Sie können jetzt {BOOKING_LABEL} hier:
{BOOKING_URL}

Der Link ist 14 Tage gültig.', true, 30, now(), now(), NULL),

-- EMAIL de
(gen_random_uuid(), 'ALL', 'de', 'EMAIL', 'BOOKING_INVITATION',
'Termin buchen — {DOCUMENT_NAME}',
'Hallo {RECIPIENT_NAME},

Wir haben Ihre Akzeptanz des Kostenvoranschlags „{DOCUMENT_NAME}" erhalten. Vielen Dank für Ihr Vertrauen!

Um den Termin zu bestätigen, können Sie {BOOKING_LABEL} unten:
{BOOKING_URL}

Der Link läuft in 14 Tagen ab. Bei Fragen stehen wir gerne zur Verfügung.', true, 30, now(), now(), NULL);
