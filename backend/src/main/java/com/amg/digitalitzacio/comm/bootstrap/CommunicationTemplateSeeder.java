package com.amg.digitalitzacio.comm.bootstrap;

import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
@RequiredArgsConstructor
@Slf4j
public class CommunicationTemplateSeeder implements ApplicationRunner {

    private final CommunicationTemplateRepository repo;

    @Override
    public void run(ApplicationArguments args) {

        // ── DEMO_SEND ──────────────────────────────────────────────────────────

        seed("ALL", "EMAIL", "DEMO_SEND", "ca", 0,
                "La teva demo personalitzada per a {NOM_NEGOCI}",
                """
                Hola!

                Hem preparat una demo interactiva personalitzada per a {NOM_NEGOCI} ({SECTOR}).

                Accedeix a la demo aquí (vàlida durant {HORES_VALIDA} hores):
                {URL_DEMO}

                La demo inclou una landing web amb un agent d'IA que pot respondre preguntes sobre els teus serveis.

                Un salut,
                Equip AMG Digitalització""");

        seed("ALL", "WHATSAPP", "DEMO_SEND", "ca", 0,
                null,
                """
                Hola! 👋

                Hem preparat una demo interactiva per a *{NOM_NEGOCI}* 🚀

                Accedeix-hi aquí (vàlida {HORES_VALIDA} hores):
                {URL_DEMO}

                La demo inclou una landing web amb un agent d'IA. Fes-li preguntes! 🤖""");

        seed("ALL", "EMAIL", "DEMO_SEND", "es", 0,
                "Tu demo personalizada para {NOM_NEGOCI}",
                """
                ¡Hola!

                Hemos preparado una demo interactiva personalizada para {NOM_NEGOCI} ({SECTOR}).

                Accede a la demo aquí (válida durante {HORES_VALIDA} horas):
                {URL_DEMO}

                La demo incluye una landing web con un agente de IA que puede responder preguntas sobre tus servicios.

                Un saludo,
                Equipo AMG Digitalització""");

        seed("ALL", "WHATSAPP", "DEMO_SEND", "es", 0,
                null,
                """
                ¡Hola! 👋

                Hemos preparado una demo interactiva para *{NOM_NEGOCI}* 🚀

                Accede aquí (válida {HORES_VALIDA} horas):
                {URL_DEMO}

                La demo incluye una landing con un agente de IA. ¡Hazle preguntas! 🤖""");

        seed("ALL", "EMAIL", "DEMO_SEND", "en", 0,
                "Your personalized demo for {NOM_NEGOCI}",
                """
                Hello!

                We've prepared an interactive demo for {NOM_NEGOCI} ({SECTOR}).

                Access the demo here (valid for {HORES_VALIDA} hours):
                {URL_DEMO}

                The demo includes a web landing page with an AI agent that can answer questions about your services.

                Best regards,
                AMG Digitalització Team""");

        seed("ALL", "WHATSAPP", "DEMO_SEND", "en", 0,
                null,
                """
                Hello! 👋

                We've prepared an interactive demo for *{NOM_NEGOCI}* 🚀

                Access it here (valid {HORES_VALIDA} hours):
                {URL_DEMO}

                The demo includes an AI agent landing page. Ask it questions! 🤖""");

        seed("ALL", "EMAIL", "DEMO_SEND", "de", 0,
                "Ihre personalisierte Demo für {NOM_NEGOCI}",
                """
                Hallo!

                Wir haben eine interaktive Demo für {NOM_NEGOCI} ({SECTOR}) vorbereitet.

                Hier gelangen Sie zur Demo (gültig {HORES_VALIDA} Stunden):
                {URL_DEMO}

                Die Demo enthält eine Landingpage mit einem KI-Agenten, der Fragen zu Ihren Dienstleistungen beantwortet.

                Mit freundlichen Grüßen,
                AMG Digitalització""");

        seed("ALL", "WHATSAPP", "DEMO_SEND", "de", 0,
                null,
                """
                Hallo! 👋

                Wir haben eine interaktive Demo für *{NOM_NEGOCI}* vorbereitet 🚀

                Hier gelangen Sie hin (gültig {HORES_VALIDA} Stunden):
                {URL_DEMO}

                Die Demo hat einen KI-Agenten. Stellen Sie ihm Fragen! 🤖""");

        // ── BUDGET_ACCEPTED ────────────────────────────────────────────────────

        seed("ALL", "EMAIL", "BUDGET_ACCEPTED", "ca", 1,
                "Pressupost #{NUM_PRESSUPOST} acceptat — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT}!

                Confirmes que el pressupost #{NUM_PRESSUPOST} per un import de {IMPORT}€ ha estat acceptat el {DATA_ACCEPTACIO}.

                Gràcies per confiar en nosaltres. Ens posarem en contacte aviat per coordinar els pròxims passos.

                Un salut,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "BUDGET_ACCEPTED", "ca", 1,
                null,
                """
                ✅ *Pressupost #{NUM_PRESSUPOST} acceptat!*

                Hola {NOM_CLIENT}, gràcies per acceptar el pressupost.

                Import: *{IMPORT}€*
                Data: {DATA_ACCEPTACIO}

                Ens posem en contacte aviat. 😊""");

        seed("ALL", "EMAIL", "BUDGET_ACCEPTED", "es", 1,
                "Presupuesto #{NUM_PRESSUPOST} aceptado — {NOM_NEGOCI}",
                """
                ¡Hola {NOM_CLIENT}!

                Confirmamos que el presupuesto #{NUM_PRESSUPOST} por importe de {IMPORT}€ ha sido aceptado el {DATA_ACCEPTACIO}.

                Gracias por confiar en nosotros. Nos pondremos en contacto pronto para coordinar los próximos pasos.

                Un saludo,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "BUDGET_ACCEPTED", "es", 1,
                null,
                """
                ✅ *¡Presupuesto #{NUM_PRESSUPOST} aceptado!*

                Hola {NOM_CLIENT}, gracias por aceptar el presupuesto.

                Importe: *{IMPORT}€*
                Fecha: {DATA_ACCEPTACIO}

                Nos ponemos en contacto pronto. 😊""");

        seed("ALL", "EMAIL", "BUDGET_ACCEPTED", "en", 1,
                "Quote #{NUM_PRESSUPOST} accepted — {NOM_NEGOCI}",
                """
                Hello {NOM_CLIENT}!

                We confirm that quote #{NUM_PRESSUPOST} for {IMPORT}€ has been accepted on {DATA_ACCEPTACIO}.

                Thank you for your trust. We'll be in touch soon to coordinate next steps.

                Best regards,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "BUDGET_ACCEPTED", "en", 1,
                null,
                """
                ✅ *Quote #{NUM_PRESSUPOST} accepted!*

                Hi {NOM_CLIENT}, thanks for accepting the quote.

                Amount: *{IMPORT}€*
                Date: {DATA_ACCEPTACIO}

                We'll be in touch soon! 😊""");

        seed("ALL", "EMAIL", "BUDGET_ACCEPTED", "de", 1,
                "Angebot #{NUM_PRESSUPOST} angenommen — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT}!

                Wir bestätigen, dass Angebot #{NUM_PRESSUPOST} über {IMPORT}€ am {DATA_ACCEPTACIO} angenommen wurde.

                Vielen Dank für Ihr Vertrauen. Wir melden uns bald, um die nächsten Schritte zu koordinieren.

                Mit freundlichen Grüßen,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "BUDGET_ACCEPTED", "de", 1,
                null,
                """
                ✅ *Angebot #{NUM_PRESSUPOST} angenommen!*

                Hallo {NOM_CLIENT}, vielen Dank für die Annahme.

                Betrag: *{IMPORT}€*
                Datum: {DATA_ACCEPTACIO}

                Wir melden uns bald! 😊""");

        // ── APPOINTMENT_CONFIRM ───────────────────────────────────────────────

        seed("ALL", "EMAIL", "APPOINTMENT_CONFIRM", "ca", 2,
                "Cita confirmada — {DATA_CITA} a les {HORA_CITA}",
                """
                Hola {NOM_CLIENT}!

                La teva cita amb {NOM_NEGOCI} ha estat confirmada.

                Detalls:
                  · Data: {DATA_CITA}
                  · Hora: {HORA_CITA}
                  · Servei: {SERVEI}

                Si necessites canviar o cancel·lar la cita, contacta'ns amb antelació.

                Fins aviat!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CONFIRM", "ca", 2,
                null,
                """
                📅 *Cita confirmada!*

                Hola {NOM_CLIENT}! La teva cita amb *{NOM_NEGOCI}*:

                📆 {DATA_CITA} a les {HORA_CITA}
                ✂️ Servei: {SERVEI}

                Si la necessites canviar, avisa'ns. Fins aviat! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_CONFIRM", "es", 2,
                "Cita confirmada — {DATA_CITA} a las {HORA_CITA}",
                """
                ¡Hola {NOM_CLIENT}!

                Tu cita con {NOM_NEGOCI} ha sido confirmada.

                Detalles:
                  · Fecha: {DATA_CITA}
                  · Hora: {HORA_CITA}
                  · Servicio: {SERVEI}

                Si necesitas cambiar o cancelar la cita, contáctanos con antelación.

                ¡Hasta pronto!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CONFIRM", "es", 2,
                null,
                """
                📅 *¡Cita confirmada!*

                ¡Hola {NOM_CLIENT}! Tu cita con *{NOM_NEGOCI}*:

                📆 {DATA_CITA} a las {HORA_CITA}
                ✂️ Servicio: {SERVEI}

                Si necesitas cambiarla, avísanos. ¡Hasta pronto! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_CONFIRM", "en", 2,
                "Appointment confirmed — {DATA_CITA} at {HORA_CITA}",
                """
                Hello {NOM_CLIENT}!

                Your appointment with {NOM_NEGOCI} has been confirmed.

                Details:
                  · Date: {DATA_CITA}
                  · Time: {HORA_CITA}
                  · Service: {SERVEI}

                If you need to reschedule or cancel, please contact us in advance.

                See you soon!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CONFIRM", "en", 2,
                null,
                """
                📅 *Appointment confirmed!*

                Hi {NOM_CLIENT}! Your appointment with *{NOM_NEGOCI}*:

                📆 {DATA_CITA} at {HORA_CITA}
                ✂️ Service: {SERVEI}

                Need to change it? Just let us know. See you soon! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_CONFIRM", "de", 2,
                "Termin bestätigt — {DATA_CITA} um {HORA_CITA}",
                """
                Hallo {NOM_CLIENT}!

                Ihr Termin bei {NOM_NEGOCI} wurde bestätigt.

                Details:
                  · Datum: {DATA_CITA}
                  · Uhrzeit: {HORA_CITA}
                  · Dienst: {SERVEI}

                Falls Sie den Termin ändern oder absagen möchten, kontaktieren Sie uns bitte rechtzeitig.

                Bis bald!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CONFIRM", "de", 2,
                null,
                """
                📅 *Termin bestätigt!*

                Hallo {NOM_CLIENT}! Ihr Termin bei *{NOM_NEGOCI}*:

                📆 {DATA_CITA} um {HORA_CITA}
                ✂️ Dienst: {SERVEI}

                Änderungen? Bitte melden Sie sich. Bis bald! 👋""");

        // ── APPOINTMENT_REMINDER ──────────────────────────────────────────────

        seed("ALL", "EMAIL", "APPOINTMENT_REMINDER", "ca", 3,
                "Recordatori: cita demà a les {HORA_CITA} — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT}!

                Et recordem que demà tens cita amb {NOM_NEGOCI}:

                  · Data: {DATA_CITA}
                  · Hora: {HORA_CITA}
                  · Servei: {SERVEI}

                Si necessites cancel·lar, contacta'ns com abans millor.

                Fins demà!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_REMINDER", "ca", 3,
                null,
                """
                ⏰ *Recordatori de cita!*

                Hola {NOM_CLIENT}! Demà tens cita amb *{NOM_NEGOCI}*:

                📆 {DATA_CITA} a les {HORA_CITA}
                ✂️ Servei: {SERVEI}

                Fins demà! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_REMINDER", "es", 3,
                "Recordatorio: cita mañana a las {HORA_CITA} — {NOM_NEGOCI}",
                """
                ¡Hola {NOM_CLIENT}!

                Te recordamos que mañana tienes cita con {NOM_NEGOCI}:

                  · Fecha: {DATA_CITA}
                  · Hora: {HORA_CITA}
                  · Servicio: {SERVEI}

                Si necesitas cancelar, contáctanos lo antes posible.

                ¡Hasta mañana!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_REMINDER", "es", 3,
                null,
                """
                ⏰ *¡Recordatorio de cita!*

                ¡Hola {NOM_CLIENT}! Mañana tienes cita con *{NOM_NEGOCI}*:

                📆 {DATA_CITA} a las {HORA_CITA}
                ✂️ Servicio: {SERVEI}

                ¡Hasta mañana! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_REMINDER", "en", 3,
                "Reminder: appointment tomorrow at {HORA_CITA} — {NOM_NEGOCI}",
                """
                Hello {NOM_CLIENT}!

                This is a reminder that you have an appointment tomorrow with {NOM_NEGOCI}:

                  · Date: {DATA_CITA}
                  · Time: {HORA_CITA}
                  · Service: {SERVEI}

                If you need to cancel, please contact us as soon as possible.

                See you tomorrow!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_REMINDER", "en", 3,
                null,
                """
                ⏰ *Appointment reminder!*

                Hi {NOM_CLIENT}! Tomorrow you have an appointment with *{NOM_NEGOCI}*:

                📆 {DATA_CITA} at {HORA_CITA}
                ✂️ Service: {SERVEI}

                See you tomorrow! 👋""");

        seed("ALL", "EMAIL", "APPOINTMENT_REMINDER", "de", 3,
                "Erinnerung: Termin morgen um {HORA_CITA} — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT}!

                Zur Erinnerung: Morgen haben Sie einen Termin bei {NOM_NEGOCI}:

                  · Datum: {DATA_CITA}
                  · Uhrzeit: {HORA_CITA}
                  · Dienst: {SERVEI}

                Falls Sie absagen müssen, kontaktieren Sie uns bitte so früh wie möglich.

                Bis morgen!
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_REMINDER", "de", 3,
                null,
                """
                ⏰ *Terminerinnerung!*

                Hallo {NOM_CLIENT}! Morgen haben Sie einen Termin bei *{NOM_NEGOCI}*:

                📆 {DATA_CITA} um {HORA_CITA}
                ✂️ Dienst: {SERVEI}

                Bis morgen! 👋""");

        // ── APPOINTMENT_CANCEL ────────────────────────────────────────────────

        seed("ALL", "EMAIL", "APPOINTMENT_CANCEL", "ca", 4,
                "Cita cancel·lada — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT}!

                La teva cita del {DATA_CITA} a les {HORA_CITA} amb {NOM_NEGOCI} ha estat cancel·lada.

                Motiu: {MOTIU}

                Per reservar una nova cita, posa't en contacte amb nosaltres.

                Un salut,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CANCEL", "ca", 4,
                null,
                """
                ❌ *Cita cancel·lada*

                Hola {NOM_CLIENT}, la teva cita del *{DATA_CITA} a les {HORA_CITA}* amb {NOM_NEGOCI} ha estat cancel·lada.

                Motiu: {MOTIU}

                Per reservar una nova cita, contacta'ns. 🙏""");

        seed("ALL", "EMAIL", "APPOINTMENT_CANCEL", "es", 4,
                "Cita cancelada — {NOM_NEGOCI}",
                """
                ¡Hola {NOM_CLIENT}!

                Tu cita del {DATA_CITA} a las {HORA_CITA} con {NOM_NEGOCI} ha sido cancelada.

                Motivo: {MOTIU}

                Para reservar una nueva cita, contáctanos.

                Un saludo,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CANCEL", "es", 4,
                null,
                """
                ❌ *Cita cancelada*

                Hola {NOM_CLIENT}, tu cita del *{DATA_CITA} a las {HORA_CITA}* con {NOM_NEGOCI} ha sido cancelada.

                Motivo: {MOTIU}

                Para reservar una nueva cita, contáctanos. 🙏""");

        seed("ALL", "EMAIL", "APPOINTMENT_CANCEL", "en", 4,
                "Appointment cancelled — {NOM_NEGOCI}",
                """
                Hello {NOM_CLIENT}!

                Your appointment on {DATA_CITA} at {HORA_CITA} with {NOM_NEGOCI} has been cancelled.

                Reason: {MOTIU}

                To book a new appointment, please contact us.

                Best regards,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CANCEL", "en", 4,
                null,
                """
                ❌ *Appointment cancelled*

                Hi {NOM_CLIENT}, your appointment on *{DATA_CITA} at {HORA_CITA}* with {NOM_NEGOCI} has been cancelled.

                Reason: {MOTIU}

                To book a new appointment, contact us. 🙏""");

        seed("ALL", "EMAIL", "APPOINTMENT_CANCEL", "de", 4,
                "Termin abgesagt — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT}!

                Ihr Termin am {DATA_CITA} um {HORA_CITA} bei {NOM_NEGOCI} wurde abgesagt.

                Grund: {MOTIU}

                Um einen neuen Termin zu buchen, kontaktieren Sie uns bitte.

                Mit freundlichen Grüßen,
                {NOM_NEGOCI}""");

        seed("ALL", "WHATSAPP", "APPOINTMENT_CANCEL", "de", 4,
                null,
                """
                ❌ *Termin abgesagt*

                Hallo {NOM_CLIENT}, Ihr Termin am *{DATA_CITA} um {HORA_CITA}* bei {NOM_NEGOCI} wurde abgesagt.

                Grund: {MOTIU}

                Für einen neuen Termin, bitte melden Sie sich. 🙏""");

        // ── SETUP_CALENDAR_AUTH ───────────────────────────────────────────────

        seed("ALL", "EMAIL", "SETUP_CALENDAR_AUTH", "ca", 5,
                "Connecta el teu Google Calendar — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Per activar la gestió automàtica de cites, necessitem accés al teu Google Calendar. Amb aquest accés, el sistema podrà:

                • Consultar la teva disponibilitat en temps real
                • Crear cites quan un client les confirma
                • Modificar o cancel·lar cites automàticament
                • Enviar recordatoris als teus clients

                Com fer-ho (1 minut):
                1. Fes clic en aquest enllaç: {URL_OAUTH}
                2. Tria el compte de Google que fas servir per a l'agenda del negoci
                3. Accepta els permisos

                Pots revocar l'accés en qualsevol moment des de myaccount.google.com › Seguretat › Aplicacions de tercers.

                Gràcies,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_CALENDAR_AUTH", "ca", 5,
                null,
                """
                📅 Hola {NOM_CLIENT}!

                Per gestionar la teva agenda automàticament (crear cites, confirmar i cancel·lar), necessitem accés al teu Google Calendar.

                Fes clic aquí i accepta els permisos:
                {URL_OAUTH}

                En un minut queda activat 😊""");

        seed("ALL", "EMAIL", "SETUP_CALENDAR_AUTH", "es", 5,
                "Conecta tu Google Calendar — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Para activar la gestión automática de citas, necesitamos acceso a tu Google Calendar. Con este acceso, el sistema podrá:

                • Consultar tu disponibilidad en tiempo real
                • Crear citas cuando un cliente las confirma
                • Modificar o cancelar citas automáticamente
                • Enviar recordatorios a tus clientes

                Cómo hacerlo (1 minuto):
                1. Haz clic en este enlace: {URL_OAUTH}
                2. Elige la cuenta de Google que usas para la agenda del negocio
                3. Acepta los permisos

                Puedes revocar el acceso en cualquier momento desde myaccount.google.com › Seguridad › Apps de terceros.

                Gracias,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_CALENDAR_AUTH", "es", 5,
                null,
                """
                📅 ¡Hola {NOM_CLIENT}!

                Para gestionar tu agenda automáticamente (crear citas, confirmar y cancelar), necesitamos acceso a tu Google Calendar.

                Haz clic aquí y acepta los permisos:
                {URL_OAUTH}

                En un minuto queda activado 😊""");

        seed("ALL", "EMAIL", "SETUP_CALENDAR_AUTH", "en", 5,
                "Connect your Google Calendar — {NOM_NEGOCI}",
                """
                Hi {NOM_CLIENT},

                To enable automatic appointment management, we need access to your Google Calendar. With this access, the system will be able to:

                • Check your availability in real time
                • Create appointments when a client confirms
                • Modify or cancel appointments automatically
                • Send reminders to your clients

                How to do it (1 minute):
                1. Click this link: {URL_OAUTH}
                2. Choose the Google account you use for your business calendar
                3. Accept the permissions

                You can revoke access at any time from myaccount.google.com › Security › Third-party apps.

                Thanks,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_CALENDAR_AUTH", "en", 5,
                null,
                """
                📅 Hi {NOM_CLIENT}!

                To manage your appointments automatically, we need access to your Google Calendar.

                Click here and accept the permissions:
                {URL_OAUTH}

                Done in one minute 😊""");

        seed("ALL", "EMAIL", "SETUP_CALENDAR_AUTH", "de", 5,
                "Verbinden Sie Ihren Google Kalender — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT},

                Um die automatische Terminverwaltung zu aktivieren, benötigen wir Zugriff auf Ihren Google Kalender. Mit diesem Zugriff kann das System:

                • Ihre Verfügbarkeit in Echtzeit prüfen
                • Termine erstellen, wenn ein Kunde bestätigt
                • Termine automatisch ändern oder stornieren
                • Erinnerungen an Ihre Kunden senden

                So geht es (1 Minute):
                1. Klicken Sie auf diesen Link: {URL_OAUTH}
                2. Wählen Sie das Google-Konto für Ihren Geschäftskalender
                3. Akzeptieren Sie die Berechtigungen

                Mit freundlichen Grüßen,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_CALENDAR_AUTH", "de", 5,
                null,
                """
                📅 Hallo {NOM_CLIENT}!

                Für die automatische Terminverwaltung benötigen wir Zugriff auf Ihren Google Kalender.

                Klicken Sie hier und akzeptieren Sie die Berechtigungen:
                {URL_OAUTH}

                In einer Minute erledigt 😊""");

        // ── SETUP_REVIEWS_URL ─────────────────────────────────────────────────

        seed("ALL", "EMAIL", "SETUP_REVIEWS_URL", "ca", 5,
                "Comparteix l'URL de les teves ressenyes de Google — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Per enviar automàticament sol·licituds de ressenya als teus clients satisfets, necessitem l'enllaç directe a les teves ressenyes de Google.

                Com obtenir-lo:
                1. Cerca el teu negoci a Google Maps
                2. Fes clic a "Escriu una ressenya"
                3. Copia l'URL del navegador
                4. Envia'ns-la responent aquest correu

                Per a què serveix: l'agent enviarà automàticament aquest enllaç als clients que han completat un servei, augmentant les teves ressenyes positives a Google.

                Gràcies,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_REVIEWS_URL", "ca", 5,
                null,
                """
                ⭐ Hola {NOM_CLIENT}!

                Per enviar sol·licituds de ressenya automàtiques als teus clients, necessitem l'URL de les teves ressenyes de Google.

                Com obtenir-la:
                1. Busca el teu negoci a Google Maps
                2. Fes clic a "Escriu una ressenya"
                3. Copia l'URL i envia-la aquí

                Gràcies! 🙏""");

        seed("ALL", "EMAIL", "SETUP_REVIEWS_URL", "es", 5,
                "Comparte la URL de tus reseñas de Google — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Para enviar automáticamente solicitudes de reseña a tus clientes satisfechos, necesitamos el enlace directo a tus reseñas de Google.

                Cómo obtenerlo:
                1. Busca tu negocio en Google Maps
                2. Haz clic en "Escribe una reseña"
                3. Copia la URL del navegador
                4. Envíanosla respondiendo este correo

                Gracias,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_REVIEWS_URL", "es", 5,
                null,
                """
                ⭐ ¡Hola {NOM_CLIENT}!

                Para enviar solicitudes de reseña automáticas a tus clientes, necesitamos la URL de tus reseñas de Google.

                Cómo obtenerla:
                1. Busca tu negocio en Google Maps
                2. Haz clic en "Escribe una reseña"
                3. Copia la URL y envíala aquí

                ¡Gracias! 🙏""");

        seed("ALL", "EMAIL", "SETUP_REVIEWS_URL", "en", 5,
                "Share your Google Reviews URL — {NOM_NEGOCI}",
                """
                Hi {NOM_CLIENT},

                To automatically send review requests to your satisfied clients, we need the direct link to your Google Reviews.

                How to get it:
                1. Search for your business on Google Maps
                2. Click "Write a review"
                3. Copy the URL from your browser
                4. Send it back by replying to this email

                Thanks,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_REVIEWS_URL", "en", 5,
                null,
                """
                ⭐ Hi {NOM_CLIENT}!

                To send automatic review requests to your clients, we need your Google Reviews URL.

                How to get it:
                1. Search your business on Google Maps
                2. Click "Write a review"
                3. Copy the URL and send it here

                Thank you! 🙏""");

        seed("ALL", "EMAIL", "SETUP_REVIEWS_URL", "de", 5,
                "Teilen Sie Ihre Google-Bewertungs-URL — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT},

                Um automatisch Bewertungsanfragen an Ihre zufriedenen Kunden zu senden, benötigen wir den direkten Link zu Ihren Google-Bewertungen.

                So erhalten Sie ihn:
                1. Suchen Sie Ihr Unternehmen bei Google Maps
                2. Klicken Sie auf "Rezension schreiben"
                3. Kopieren Sie die URL aus dem Browser
                4. Senden Sie sie als Antwort auf diese E-Mail

                Mit freundlichen Grüßen,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_REVIEWS_URL", "de", 5,
                null,
                """
                ⭐ Hallo {NOM_CLIENT}!

                Für automatische Bewertungsanfragen benötigen wir Ihre Google-Bewertungs-URL.

                So erhalten Sie sie:
                1. Suchen Sie Ihr Unternehmen bei Google Maps
                2. Klicken Sie auf "Rezension schreiben"
                3. Kopieren Sie die URL und senden Sie sie hier

                Vielen Dank! 🙏""");

        // ── SETUP_TELEGRAM_GROUP ──────────────────────────────────────────────

        seed("ALL", "EMAIL", "SETUP_TELEGRAM_GROUP", "ca", 5,
                "Configura les alertes de Telegram — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Per rebre alertes automàtiques al teu equip (nous leads, cites del dia, incidències), necessitem connectar el sistema al vostre grup de Telegram.

                Com configurar-ho (5 minuts):
                1. Crea un grup de Telegram per al teu equip (o usa'n un d'existent)
                2. Afegeix el bot @{NOM_BOT} al grup
                3. Escriu /start al grup
                4. El bot respondrà amb l'ID del grup (un número negatiu, ex: -1001234567890)
                5. Envia'ns aquest número responent aquest correu

                Amb això configurat, el teu equip rebrà notificacions automàtiques en temps real.

                Gràcies,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_TELEGRAM_GROUP", "ca", 5,
                null,
                """
                📱 Hola {NOM_CLIENT}!

                Per activar les alertes automàtiques al teu equip per Telegram, necessitem l'ID del vostre grup.

                Com obtenir-lo:
                1. Afegeix @{NOM_BOT} al teu grup de Telegram
                2. Escriu /start al grup
                3. Copia l'ID que t'envia (número negatiu) i envia'l aquí

                Gràcies! 🙏""");

        seed("ALL", "EMAIL", "SETUP_TELEGRAM_GROUP", "es", 5,
                "Configura las alertas de Telegram — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Para recibir alertas automáticas en tu equipo (nuevos leads, citas del día, incidencias), necesitamos conectar el sistema a vuestro grupo de Telegram.

                Cómo configurarlo (5 minutos):
                1. Crea un grupo de Telegram para tu equipo (o usa uno existente)
                2. Añade el bot @{NOM_BOT} al grupo
                3. Escribe /start en el grupo
                4. El bot responderá con el ID del grupo (un número negativo, ej: -1001234567890)
                5. Envíanos ese número respondiendo este correo

                Gracias,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_TELEGRAM_GROUP", "es", 5,
                null,
                """
                📱 ¡Hola {NOM_CLIENT}!

                Para activar las alertas automáticas en tu equipo por Telegram, necesitamos el ID de vuestro grupo.

                Cómo obtenerlo:
                1. Añade @{NOM_BOT} a tu grupo de Telegram
                2. Escribe /start en el grupo
                3. Copia el ID que te envía (número negativo) y envíalo aquí

                ¡Gracias! 🙏""");

        seed("ALL", "EMAIL", "SETUP_TELEGRAM_GROUP", "en", 5,
                "Set up Telegram alerts — {NOM_NEGOCI}",
                """
                Hi {NOM_CLIENT},

                To receive automatic alerts for your team (new leads, daily appointments, incidents), we need to connect the system to your Telegram group.

                How to set it up (5 minutes):
                1. Create a Telegram group for your team (or use an existing one)
                2. Add the bot @{NOM_BOT} to the group
                3. Type /start in the group
                4. The bot will reply with the group ID (a negative number, e.g. -1001234567890)
                5. Send us that number by replying to this email

                Thanks,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_TELEGRAM_GROUP", "en", 5,
                null,
                """
                📱 Hi {NOM_CLIENT}!

                To activate automatic alerts for your team on Telegram, we need your group ID.

                How to get it:
                1. Add @{NOM_BOT} to your Telegram group
                2. Type /start in the group
                3. Copy the ID it sends (negative number) and send it here

                Thank you! 🙏""");

        seed("ALL", "EMAIL", "SETUP_TELEGRAM_GROUP", "de", 5,
                "Telegram-Benachrichtigungen einrichten — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT},

                Um automatische Benachrichtigungen für Ihr Team zu erhalten (neue Leads, Tagestermine, Vorfälle), müssen wir das System mit Ihrer Telegram-Gruppe verbinden.

                Einrichtung (5 Minuten):
                1. Erstellen Sie eine Telegram-Gruppe für Ihr Team (oder nutzen Sie eine bestehende)
                2. Fügen Sie den Bot @{NOM_BOT} zur Gruppe hinzu
                3. Schreiben Sie /start in der Gruppe
                4. Der Bot antwortet mit der Gruppen-ID (eine negative Zahl, z.B. -1001234567890)
                5. Senden Sie uns diese Zahl als Antwort auf diese E-Mail

                Mit freundlichen Grüßen,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_TELEGRAM_GROUP", "de", 5,
                null,
                """
                📱 Hallo {NOM_CLIENT}!

                Für automatische Team-Benachrichtigungen über Telegram benötigen wir Ihre Gruppen-ID.

                So erhalten Sie sie:
                1. Fügen Sie @{NOM_BOT} zu Ihrer Telegram-Gruppe hinzu
                2. Schreiben Sie /start in der Gruppe
                3. Kopieren Sie die ID (negative Zahl) und senden Sie sie hier

                Vielen Dank! 🙏""");

        // ── SETUP_SERVICE_CATALOG ─────────────────────────────────────────────

        seed("ALL", "EMAIL", "SETUP_SERVICE_CATALOG", "ca", 5,
                "Envia'ns el teu catàleg de serveis — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Per configurar l'agent de pressupostos, necessitem el teu catàleg de serveis amb els preus corresponents.

                Pots enviar-ho en qualsevol format (llista, Excel, PDF, foto d'un full...). Necessitem saber:

                • Nom de cada servei o feina
                • Preu (o rang de preus si varia)
                • Unitat (per hora, per m², per unitat, preu fix...)
                • Descripció breu (opcional)

                Exemple:
                - Pintura habitació: 150-250€ (depenent de la mida)
                - Pintura façana: des de 8€/m²
                - Preparació superfície: inclòs

                Respon aquest correu adjuntant o escrivint el teu catàleg.

                Gràcies,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_SERVICE_CATALOG", "ca", 5,
                null,
                """
                📋 Hola {NOM_CLIENT}!

                Per configurar els pressupostos automàtics, necessitem el teu llistat de serveis i preus.

                Pots enviar-lo en qualsevol format: llista, foto, PDF...

                Per a cada servei necessitem:
                • Nom del servei
                • Preu (o rang)
                • Unitat (hora, m², unitat...)

                Envia'l quan puguis 🙏""");

        seed("ALL", "EMAIL", "SETUP_SERVICE_CATALOG", "es", 5,
                "Envíanos tu catálogo de servicios — {NOM_NEGOCI}",
                """
                Hola {NOM_CLIENT},

                Para configurar el agente de presupuestos, necesitamos tu catálogo de servicios con los precios correspondientes.

                Puedes enviarlo en cualquier formato (lista, Excel, PDF, foto...). Necesitamos saber:

                • Nombre de cada servicio o trabajo
                • Precio (o rango si varía)
                • Unidad (por hora, por m², por unidad, precio fijo...)
                • Descripción breve (opcional)

                Responde este correo adjuntando o escribiendo tu catálogo.

                Gracias,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_SERVICE_CATALOG", "es", 5,
                null,
                """
                📋 ¡Hola {NOM_CLIENT}!

                Para configurar los presupuestos automáticos, necesitamos tu listado de servicios y precios.

                Puedes enviarlo en cualquier formato: lista, foto, PDF...

                Para cada servicio necesitamos:
                • Nombre del servicio
                • Precio (o rango)
                • Unidad (hora, m², unidad...)

                ¡Envíalo cuando puedas! 🙏""");

        seed("ALL", "EMAIL", "SETUP_SERVICE_CATALOG", "en", 5,
                "Send us your service catalog — {NOM_NEGOCI}",
                """
                Hi {NOM_CLIENT},

                To set up the automatic quote agent, we need your service catalog with corresponding prices.

                You can send it in any format (list, Excel, PDF, photo...). We need to know:

                • Name of each service or job
                • Price (or range if it varies)
                • Unit (per hour, per m², per unit, fixed price...)
                • Brief description (optional)

                Reply to this email with your catalog attached or written out.

                Thanks,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_SERVICE_CATALOG", "en", 5,
                null,
                """
                📋 Hi {NOM_CLIENT}!

                To set up automatic quotes, we need your service list and prices.

                Any format works: list, photo, PDF...

                For each service we need:
                • Service name
                • Price (or range)
                • Unit (hour, m², unit...)

                Send it whenever you can! 🙏""");

        seed("ALL", "EMAIL", "SETUP_SERVICE_CATALOG", "de", 5,
                "Senden Sie uns Ihren Servicekatalog — {NOM_NEGOCI}",
                """
                Hallo {NOM_CLIENT},

                Um den automatischen Angebots-Agenten einzurichten, benötigen wir Ihren Servicekatalog mit den entsprechenden Preisen.

                Sie können ihn in jedem Format senden (Liste, Excel, PDF, Foto...). Wir benötigen:

                • Name jeder Dienstleistung oder Arbeit
                • Preis (oder Preisspanne)
                • Einheit (pro Stunde, pro m², pro Stück, Festpreis...)
                • Kurze Beschreibung (optional)

                Antworten Sie auf diese E-Mail mit Ihrem Katalog.

                Mit freundlichen Grüßen,
                {NOM_AGENCIA}""");

        seed("ALL", "WHATSAPP", "SETUP_SERVICE_CATALOG", "de", 5,
                null,
                """
                📋 Hallo {NOM_CLIENT}!

                Für automatische Angebote benötigen wir Ihre Dienstleistungsliste mit Preisen.

                Jedes Format ist möglich: Liste, Foto, PDF...

                Für jeden Service benötigen wir:
                • Servicename
                • Preis (oder Spanne)
                • Einheit (Stunde, m², Stück...)

                Senden Sie es, wenn Sie können! 🙏""");

        // ── SETUP_WEB_URL ─────────────────────────────────────────────────────

        seed("ALL", "WHATSAPP", "SETUP_WEB_URL", "ca", 5,
                null,
                """
                🌐 Hola {NOM_CLIENT}!

                Per configurar el teu agent, necessitem l'adreça de la teva web actual.

                Si tens web, envia'ns l'URL (ex: www.elnegoci.com).
                Si no en tens, no passa res, t'ajudem amb una landing page.

                Gràcies! 😊""");

        seed("ALL", "WHATSAPP", "SETUP_WEB_URL", "es", 5,
                null,
                """
                🌐 ¡Hola {NOM_CLIENT}!

                Para configurar tu agente, necesitamos la dirección de tu web actual.

                Si tienes web, envíanos la URL (ej: www.tunegocio.com).
                Si no tienes, no pasa nada, te ayudamos con una landing page.

                ¡Gracias! 😊""");

        seed("ALL", "WHATSAPP", "SETUP_WEB_URL", "en", 5,
                null,
                """
                🌐 Hi {NOM_CLIENT}!

                To configure your agent, we need your current website address.

                If you have a website, send us the URL (e.g. www.yourbusiness.com).
                If you don't have one, no problem — we'll help you with a landing page.

                Thanks! 😊""");

        seed("ALL", "WHATSAPP", "SETUP_WEB_URL", "de", 5,
                null,
                """
                🌐 Hallo {NOM_CLIENT}!

                Um Ihren Agenten zu konfigurieren, benötigen wir die Adresse Ihrer aktuellen Website.

                Falls Sie eine Website haben, senden Sie uns die URL (z.B. www.ihrgeschaeft.de).
                Falls nicht, kein Problem — wir helfen Ihnen mit einer Landing Page.

                Danke! 😊""");

        log.info("CommunicationTemplateSeeder: {} templates actives", repo.count());
    }

    private void seed(String sector, String channel, String action, String language,
                      int order, String subject, String body) {
        if (repo.existsBySectorAndChannelAndActionAndLanguage(sector, channel, action, language)) return;
        repo.save(CommunicationTemplate.builder()
                .sector(sector)
                .channel(channel)
                .action(action)
                .language(language)
                .subject(subject)
                .body(body.stripIndent().strip())
                .sortOrder(order)
                .build());
        log.debug("Seeded comm template: {}/{}/{}/{}", sector, channel, action, language);
    }
}
