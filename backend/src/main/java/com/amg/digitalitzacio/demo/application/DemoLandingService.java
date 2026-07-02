package com.amg.digitalitzacio.demo.application;

import com.amg.digitalitzacio.agents.domain.SectorTemplateRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.chat.domain.LandingChatContext;
import com.amg.digitalitzacio.chat.domain.LandingChatContextRepository;
import com.amg.digitalitzacio.engine.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoLandingService {

    private final TenantRepository tenantRepository;
    private final LandingRepository landingRepository;
    private final LandingVersionRepository landingVersionRepository;
    private final LandingChatContextRepository landingChatContextRepository;
    private final SectorTemplateRepository sectorTemplateRepository;
    private final ObjectMapper objectMapper;

    // ── Data model ───────────────────────────────────────────────────────────

    private record SectorConfig(String emoji, String primaryColor, String headline,
                                 String subheadline, String servicesTitle,
                                 String ctaHeroText, String ctaSectionTitle, String ctaSectionSubtitle, String ctaSectionBtn,
                                 String contactTitle,
                                 List<ServiceItem> services) {}

    private record ServiceItem(String title, String description) {}

    // ── Catalan content ──────────────────────────────────────────────────────

    private static final SectorConfig DEFAULT_CA = new SectorConfig(
        "🏢", "#4f46e5",
        "El millor servei per al teu negoci", "Parla amb el nostre agent i descobreix com t'ajudem",
        "Els nostres serveis", "💬 Parla amb el nostre agent",
        "Reserva la teva cita o consulta", "El nostre agent respon en menys d'1 minut", "Inicia el xat ara",
        "Contacte directe",
        List.of(new ServiceItem("Servei premium", "Atenció personalitzada al client"),
                new ServiceItem("Respostes ràpides", "Disponible 24 hores al dia"),
                new ServiceItem("Confiança", "Anys d'experiència al sector")));

    private static final Map<String, SectorConfig> SECTORS_CA = Map.ofEntries(
        Map.entry("PERRUQUERIA", new SectorConfig("✂️", "#7c3aed",
            "El teu estil, la nostra passió", "Reserva la teva cita en 30 segons via xat",
            "Els nostres serveis", "💬 Parla amb el nostre agent",
            "Reserva la teva cita ara", "El nostre agent respon en menys d'1 minut", "Inicia el xat ara",
            "Contacte directe",
            List.of(new ServiceItem("Tall i estil", "Disseny personalitzat per a cada client"),
                    new ServiceItem("Coloració", "Tècniques modernes amb productes premium"),
                    new ServiceItem("Tractaments", "Hidratació, queratina i nutrició capilar")))),
        Map.entry("ESTETICA", new SectorConfig("💅", "#db2777",
            "Bellesa que s'expressa", "Demana cita ara i descobreix els nostres tractaments",
            "Els nostres serveis", "💬 Parla amb el nostre agent",
            "Demana la teva cita avui", "El nostre agent respon en menys d'1 minut", "Inicia el xat ara",
            "Contacte directe",
            List.of(new ServiceItem("Tractament facial", "Neteja profunda i hidratació personalitzada"),
                    new ServiceItem("Depilació", "Làser, cera i fil per a una pell perfecta"),
                    new ServiceItem("Massatge corporal", "Relaxació i drenatge limfàtic")))),
        Map.entry("FISIOTERAPEUTA", new SectorConfig("🏃", "#0891b2",
            "Recupera el teu moviment", "Parla amb el nostre especialista al xat",
            "Els nostres serveis", "💬 Parla amb el nostre especialista",
            "Consulta el teu cas", "El nostre fisioterapeuta et respon aviat", "Inicia el xat ara",
            "Contacte directe",
            List.of(new ServiceItem("Recuperació de lesions", "Protocols personalitzats post-lesió"),
                    new ServiceItem("Fisioteràpia esportiva", "Per a atletes i persones actives"),
                    new ServiceItem("Pilates terapèutic", "Enfortiment i prevenció")))),
        Map.entry("PSICOLEG", new SectorConfig("🧠", "#4f46e5",
            "El benestar comença aquí", "Consulta confidencial disponible al xat",
            "Els nostres serveis", "💬 Parla amb nosaltres",
            "Dona el primer pas", "Consulta confidencial i sense compromís", "Inicia el xat ara",
            "Contacte directe",
            List.of(new ServiceItem("Teràpia individual", "Un espai segur per créixer i millorar"),
                    new ServiceItem("Teràpia de parella", "Comunicació i vincle afectiu"),
                    new ServiceItem("Psicologia infantil", "Suport en etapes clau del creixement")))),
        Map.entry("NUTRICIONISTA", new SectorConfig("🥗", "#16a34a",
            "Menja bé, viu millor", "Consulta el teu pla nutricional via xat",
            "Els nostres serveis", "💬 Parla amb el nostre nutricionista",
            "Comença el teu canvi avui", "El teu pla personalitzat t'espera", "Inicia el xat ara",
            "Contacte directe",
            List.of(new ServiceItem("Dieta personalitzada", "Plans adaptats als teus objectius"),
                    new ServiceItem("Seguiment mensual", "Acompanyament i ajustos constants"),
                    new ServiceItem("Nutrició esportiva", "Optimitza el teu rendiment")))),
        Map.entry("TALLER_MECANIC", new SectorConfig("🔧", "#b45309",
            "El teu cotxe en bones mans", "Demana pressupost gratuït al xat",
            "Els nostres serveis", "💬 Demana pressupost gratuït",
            "Pressupost sense compromís", "Et responem en menys d'1 minut", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Revisió completa", "Diagnosi electrònica i mecànica"),
                    new ServiceItem("Reparació", "Tots els marques i models"),
                    new ServiceItem("Pneumàtics i frens", "Muntatge i equilibrat")))),
        Map.entry("VETERINARI", new SectorConfig("🐾", "#0f766e",
            "La salut de la teva mascota, primer", "Reserva visita al xat en un clic",
            "Els nostres serveis", "💬 Reserva visita ara",
            "La teva mascota et necessita", "El nostre veterinari et respon aviat", "Reservar visita",
            "Contacte directe",
            List.of(new ServiceItem("Consultes generals", "Revisions i diagnòstics"),
                    new ServiceItem("Vacunació", "Calendari vacunal complet"),
                    new ServiceItem("Cirurgia i urgències", "Atenció especialitzada 24h")))),
        Map.entry("RESTAURANTE", new SectorConfig("🍽️", "#ea580c",
            "Sabors que et faran tornar", "Reserva taula ara al xat",
            "La nostra carta", "💬 Reserva taula ara",
            "Reserva la teva taula", "Et confirmen la reserva en minuts", "Reservar taula",
            "Contacte directe",
            List.of(new ServiceItem("Menú del dia", "Cuina de mercat fresca cada dia"),
                    new ServiceItem("Esdeveniments", "Sopars d'empresa i celebracions"),
                    new ServiceItem("Menjar per emportar", "Recull o lliurament a domicili")))),
        Map.entry("ELECTRICISTA", new SectorConfig("⚡", "#ca8a04",
            "Solucions elèctriques professionals", "Pressupost gratuït en minuts via xat",
            "Els nostres serveis", "💬 Demana pressupost gratuït",
            "Pressupost sense compromís", "Resposta en menys de 5 minuts", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Instal·lacions", "Noves instal·lacions i reformes"),
                    new ServiceItem("Reparació d'avaries", "Resposta ràpida i garantia"),
                    new ServiceItem("Energia solar", "Plaques fotovoltaiques i estalvi")))),
        Map.entry("FONTANER", new SectorConfig("🚰", "#0284c7",
            "Urgències i instal·lacions 24h", "Contacta ara al xat per a qualsevol avaria",
            "Els nostres serveis", "💬 Urgències 24h — contacta ara",
            "Avaria? Et resolem avui", "Servei d'urgències disponible ara", "Contactar ara",
            "Contacte directe",
            List.of(new ServiceItem("Reparació d'avaries", "Servei d'urgències tot l'any"),
                    new ServiceItem("Instal·lació", "Banyeres, dutxes i sanitaris"),
                    new ServiceItem("Obres i reformes", "Projectes integrals de fontaneria")))),
        Map.entry("JARDINER", new SectorConfig("🌿", "#15803d",
            "Espais verds que inspiren", "Demana el teu pressupost al xat",
            "Els nostres serveis", "💬 Demana pressupost gratuït",
            "El jardí dels teus somnis", "Et fem un pressupost sense compromís", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Disseny de jardins", "Projectes personalitzats"),
                    new ServiceItem("Manteniment", "Serveis periòdics adaptats"),
                    new ServiceItem("Reg automàtic", "Instal·lació i programació")))),
        Map.entry("NETEJA", new SectorConfig("🧹", "#64748b",
            "Espais impecables, clients feliços", "Demana pressupost sense compromís al xat",
            "Els nostres serveis", "💬 Demana pressupost ara",
            "Pressupost gratuït i sense compromís", "Et responem en menys d'1 minut", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Neteja de llar", "Servei regular o puntual"),
                    new ServiceItem("Oficines i locals", "Contractes de manteniment"),
                    new ServiceItem("Neteja profunda", "Moquetes, cristalls i finestres")))),
        Map.entry("GESTORIA", new SectorConfig("📊", "#1d4ed8",
            "La teva empresa en ordre", "Consulta gratuïta al xat",
            "Els nostres serveis", "💬 Consulta gratuïta ara",
            "Consulta gratuïta sense compromís", "Un gestor et respon en minuts", "Iniciar consulta",
            "Contacte directe",
            List.of(new ServiceItem("Comptabilitat", "Comptes clars per a créixer"),
                    new ServiceItem("Assessoria fiscal", "IRPF, IVA i impost de societats"),
                    new ServiceItem("Laboral i RRHH", "Nòmines, contractes i baixes")))),
        Map.entry("ACADEMIA", new SectorConfig("📚", "#7c3aed",
            "Aprèn amb els millors", "Demana informació del curs al xat",
            "Els nostres cursos", "💬 Demana informació ara",
            "Comença el teu aprenentatge", "Et guiem cap al curs ideal", "Demanar informació",
            "Contacte directe",
            List.of(new ServiceItem("Idiomes", "Anglès, alemany, francès i més"),
                    new ServiceItem("Oposicions", "Preparació intensiva per a funcionaris"),
                    new ServiceItem("Reforç escolar", "Primària, ESO, Batxillerat i selectivitat")))),
        Map.entry("PERRUQUERIA_CANINA", new SectorConfig("🐕", "#d97706",
            "El teu millor amic mereix el millor", "Reserva la banyera al xat",
            "Els nostres serveis", "💬 Reserva cita ara",
            "Reserva la cita del teu gos", "Et confirmem disponibilitat al moment", "Reservar cita",
            "Contacte directe",
            List.of(new ServiceItem("Bany i assecat", "Productes hipoal·lèrgics i segurs"),
                    new ServiceItem("Tall i estilisme", "Per a totes les races i mides"),
                    new ServiceItem("Estètica completa", "Ungles, orelles i dents")))),
        Map.entry("INMOBILIARIA", new SectorConfig("🏠", "#0f172a",
            "La teva llar ideal t'espera", "Consulta el nostre catàleg al xat",
            "Els nostres serveis", "💬 Parla amb un agent immobiliari",
            "Troba la teva propietat", "Un agent et respon ara", "Parlar amb un agent",
            "Contacte directe",
            List.of(new ServiceItem("Compravenda", "T'acompanyem en tot el procés"),
                    new ServiceItem("Lloguer", "Pisos, locals i garatges"),
                    new ServiceItem("Valoració gratuïta", "Coneix el valor real del teu immoble")))),
        Map.entry("AGENCIA_IA", new SectorConfig("🤖", "#6d28d9",
            "Automatitza el teu negoci amb IA", "Parla amb el nostre agent i descobreix les possibilitats",
            "Els nostres serveis", "💬 Parla amb el nostre agent IA",
            "Descobreix el potencial de la IA", "Et mostrem com automatitzar el teu negoci", "Parlar amb l'agent",
            "Contacte directe",
            List.of(new ServiceItem("Automatitzacions", "Workflows que treballen per tu"),
                    new ServiceItem("Agents IA", "Atenció al client 24/7 amb IA"),
                    new ServiceItem("Integracions", "CRM, WhatsApp, Google i molt més")))),
        Map.entry("PINTOR", new SectorConfig("🎨", "#1e40af",
            "Pintura i lacats professionals a Mallorca", "Pressupost gratuït en menys de 24h — respon el nostre agent",
            "Els nostres serveis", "💬 Demana pressupost gratuït",
            "Pressupost sense compromís", "Et responem avui mateix", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Pintura interior i exterior", "Habitatges, locals i façanes amb acabats professionals"),
                    new ServiceItem("Lacats i esmalts", "Mobles, portes i fusteria amb cabines pressuritzades"),
                    new ServiceItem("Protecció de fusta", "Tractaments i vernissos per a llarga durada")))),
        Map.entry("PINTORS", new SectorConfig("🎨", "#1e40af",
            "Pintura i lacats professionals a Mallorca", "Pressupost gratuït en menys de 24h — respon el nostre agent",
            "Els nostres serveis", "💬 Demana pressupost gratuït",
            "Pressupost sense compromís", "Et responem avui mateix", "Demanar pressupost",
            "Contacte directe",
            List.of(new ServiceItem("Pintura interior i exterior", "Habitatges, locals i façanes amb acabats professionals"),
                    new ServiceItem("Lacats i esmalts", "Mobles, portes i fusteria amb cabines pressuritzades"),
                    new ServiceItem("Protecció de fusta", "Tractaments i vernissos per a llarga durada"))))
    );

    // ── Spanish content ──────────────────────────────────────────────────────

    private static final SectorConfig DEFAULT_ES = new SectorConfig(
        "🏢", "#4f46e5",
        "El mejor servicio para tu negocio", "Habla con nuestro agente y descubre cómo te ayudamos",
        "Nuestros servicios", "💬 Habla con nuestro agente",
        "Reserva tu cita o consulta", "Nuestro agente responde en menos de 1 minuto", "Iniciar chat",
        "Contacto directo",
        List.of(new ServiceItem("Servicio premium", "Atención personalizada al cliente"),
                new ServiceItem("Respuestas rápidas", "Disponible 24 horas al día"),
                new ServiceItem("Confianza", "Años de experiencia en el sector")));

    private static final Map<String, SectorConfig> SECTORS_ES = Map.ofEntries(
        Map.entry("PERRUQUERIA", new SectorConfig("✂️", "#7c3aed",
            "Tu estilo, nuestra pasión", "Reserva tu cita en 30 segundos vía chat",
            "Nuestros servicios", "💬 Habla con nuestro agente",
            "Reserva tu cita ahora", "Nuestro agente responde en menos de 1 minuto", "Iniciar chat",
            "Contacto directo",
            List.of(new ServiceItem("Corte y estilo", "Diseño personalizado para cada cliente"),
                    new ServiceItem("Coloración", "Técnicas modernas con productos premium"),
                    new ServiceItem("Tratamientos", "Hidratación, queratina y nutrición capilar")))),
        Map.entry("ESTETICA", new SectorConfig("💅", "#db2777",
            "Belleza que se expresa", "Pide cita ahora y descubre nuestros tratamientos",
            "Nuestros servicios", "💬 Habla con nuestro agente",
            "Pide tu cita hoy", "Nuestro agente responde en menos de 1 minuto", "Iniciar chat",
            "Contacto directo",
            List.of(new ServiceItem("Tratamiento facial", "Limpieza profunda e hidratación personalizada"),
                    new ServiceItem("Depilación", "Láser, cera e hilo para una piel perfecta"),
                    new ServiceItem("Masaje corporal", "Relajación y drenaje linfático")))),
        Map.entry("FISIOTERAPEUTA", new SectorConfig("🏃", "#0891b2",
            "Recupera tu movimiento", "Habla con nuestro especialista en el chat",
            "Nuestros servicios", "💬 Habla con nuestro especialista",
            "Consulta tu caso", "Nuestro fisioterapeuta te responde pronto", "Iniciar chat",
            "Contacto directo",
            List.of(new ServiceItem("Recuperación de lesiones", "Protocolos personalizados post-lesión"),
                    new ServiceItem("Fisioterapia deportiva", "Para atletas y personas activas"),
                    new ServiceItem("Pilates terapéutico", "Fortalecimiento y prevención")))),
        Map.entry("PSICOLEG", new SectorConfig("🧠", "#4f46e5",
            "El bienestar empieza aquí", "Consulta confidencial disponible en el chat",
            "Nuestros servicios", "💬 Habla con nosotros",
            "Da el primer paso", "Consulta confidencial y sin compromiso", "Iniciar chat",
            "Contacto directo",
            List.of(new ServiceItem("Terapia individual", "Un espacio seguro para crecer y mejorar"),
                    new ServiceItem("Terapia de pareja", "Comunicación y vínculo afectivo"),
                    new ServiceItem("Psicología infantil", "Apoyo en etapas clave del crecimiento")))),
        Map.entry("NUTRICIONISTA", new SectorConfig("🥗", "#16a34a",
            "Come bien, vive mejor", "Consulta tu plan nutricional vía chat",
            "Nuestros servicios", "💬 Habla con nuestro nutricionista",
            "Empieza tu cambio hoy", "Tu plan personalizado te espera", "Iniciar chat",
            "Contacto directo",
            List.of(new ServiceItem("Dieta personalizada", "Planes adaptados a tus objetivos"),
                    new ServiceItem("Seguimiento mensual", "Acompañamiento y ajustes constantes"),
                    new ServiceItem("Nutrición deportiva", "Optimiza tu rendimiento")))),
        Map.entry("TALLER_MECANIC", new SectorConfig("🔧", "#b45309",
            "Tu coche en buenas manos", "Pide presupuesto gratuito en el chat",
            "Nuestros servicios", "💬 Pide presupuesto gratuito",
            "Presupuesto sin compromiso", "Te respondemos en menos de 1 minuto", "Pedir presupuesto",
            "Contacto directo",
            List.of(new ServiceItem("Revisión completa", "Diagnóstico electrónico y mecánico"),
                    new ServiceItem("Reparación", "Todas las marcas y modelos"),
                    new ServiceItem("Neumáticos y frenos", "Montaje y equilibrado")))),
        Map.entry("VETERINARI", new SectorConfig("🐾", "#0f766e",
            "La salud de tu mascota, primero", "Reserva visita en el chat con un clic",
            "Nuestros servicios", "💬 Reserva visita ahora",
            "Tu mascota te necesita", "Nuestro veterinario te responde pronto", "Reservar visita",
            "Contacto directo",
            List.of(new ServiceItem("Consultas generales", "Revisiones y diagnósticos"),
                    new ServiceItem("Vacunación", "Calendario vacunal completo"),
                    new ServiceItem("Cirugía y urgencias", "Atención especializada 24h")))),
        Map.entry("RESTAURANTE", new SectorConfig("🍽️", "#ea580c",
            "Sabores que te harán volver", "Reserva mesa ahora en el chat",
            "Nuestra carta", "💬 Reserva mesa ahora",
            "Reserva tu mesa", "Te confirmamos la reserva en minutos", "Reservar mesa",
            "Contacto directo",
            List.of(new ServiceItem("Menú del día", "Cocina de mercado fresca cada día"),
                    new ServiceItem("Eventos", "Cenas de empresa y celebraciones"),
                    new ServiceItem("Comida para llevar", "Recoge o entrega a domicilio")))),
        Map.entry("ELECTRICISTA", new SectorConfig("⚡", "#ca8a04",
            "Soluciones eléctricas profesionales", "Presupuesto gratuito en minutos vía chat",
            "Nuestros servicios", "💬 Pide presupuesto gratuito",
            "Presupuesto sin compromiso", "Respuesta en menos de 5 minutos", "Pedir presupuesto",
            "Contacto directo",
            List.of(new ServiceItem("Instalaciones", "Nuevas instalaciones y reformas"),
                    new ServiceItem("Reparación de averías", "Respuesta rápida y garantía"),
                    new ServiceItem("Energía solar", "Placas fotovoltaicas y ahorro")))),
        Map.entry("FONTANER", new SectorConfig("🚰", "#0284c7",
            "Urgencias e instalaciones 24h", "Contáctanos ahora en el chat para cualquier avería",
            "Nuestros servicios", "💬 Urgencias 24h — contacta ahora",
            "¿Avería? Te lo resolvemos hoy", "Servicio de urgencias disponible ahora", "Contactar ahora",
            "Contacto directo",
            List.of(new ServiceItem("Reparación de averías", "Servicio de urgencias todo el año"),
                    new ServiceItem("Instalación", "Bañeras, duchas y sanitarios"),
                    new ServiceItem("Obras y reformas", "Proyectos integrales de fontanería")))),
        Map.entry("JARDINER", new SectorConfig("🌿", "#15803d",
            "Espacios verdes que inspiran", "Pide tu presupuesto en el chat",
            "Nuestros servicios", "💬 Pide presupuesto gratuito",
            "El jardín de tus sueños", "Te hacemos un presupuesto sin compromiso", "Pedir presupuesto",
            "Contacto directo",
            List.of(new ServiceItem("Diseño de jardines", "Proyectos personalizados"),
                    new ServiceItem("Mantenimiento", "Servicios periódicos adaptados"),
                    new ServiceItem("Riego automático", "Instalación y programación")))),
        Map.entry("NETEJA", new SectorConfig("🧹", "#64748b",
            "Espacios impecables, clientes felices", "Pide presupuesto sin compromiso en el chat",
            "Nuestros servicios", "💬 Pide presupuesto ahora",
            "Presupuesto gratuito y sin compromiso", "Te respondemos en menos de 1 minuto", "Pedir presupuesto",
            "Contacto directo",
            List.of(new ServiceItem("Limpieza del hogar", "Servicio regular o puntual"),
                    new ServiceItem("Oficinas y locales", "Contratos de mantenimiento"),
                    new ServiceItem("Limpieza profunda", "Moquetas, cristales y ventanas")))),
        Map.entry("GESTORIA", new SectorConfig("📊", "#1d4ed8",
            "Tu empresa en orden", "Consulta gratuita en el chat",
            "Nuestros servicios", "💬 Consulta gratuita ahora",
            "Consulta gratuita sin compromiso", "Un gestor te responde en minutos", "Iniciar consulta",
            "Contacto directo",
            List.of(new ServiceItem("Contabilidad", "Cuentas claras para crecer"),
                    new ServiceItem("Asesoría fiscal", "IRPF, IVA e impuesto de sociedades"),
                    new ServiceItem("Laboral y RRHH", "Nóminas, contratos y bajas")))),
        Map.entry("ACADEMIA", new SectorConfig("📚", "#7c3aed",
            "Aprende con los mejores", "Pide información del curso en el chat",
            "Nuestros cursos", "💬 Pide información ahora",
            "Empieza tu aprendizaje", "Te guiamos hacia el curso ideal", "Pedir información",
            "Contacto directo",
            List.of(new ServiceItem("Idiomas", "Inglés, alemán, francés y más"),
                    new ServiceItem("Oposiciones", "Preparación intensiva para funcionarios"),
                    new ServiceItem("Refuerzo escolar", "Primaria, ESO, Bachillerato y selectividad")))),
        Map.entry("PERRUQUERIA_CANINA", new SectorConfig("🐕", "#d97706",
            "Tu mejor amigo merece lo mejor", "Reserva la bañera en el chat",
            "Nuestros servicios", "💬 Reserva cita ahora",
            "Reserva la cita de tu perro", "Te confirmamos disponibilidad al momento", "Reservar cita",
            "Contacto directo",
            List.of(new ServiceItem("Baño y secado", "Productos hipoalergénicos y seguros"),
                    new ServiceItem("Corte y estilismo", "Para todas las razas y tamaños"),
                    new ServiceItem("Estética completa", "Uñas, orejas y dientes")))),
        Map.entry("INMOBILIARIA", new SectorConfig("🏠", "#0f172a",
            "Tu hogar ideal te espera", "Consulta nuestro catálogo en el chat",
            "Nuestros servicios", "💬 Habla con un agente inmobiliario",
            "Encuentra tu propiedad", "Un agente te responde ahora", "Hablar con un agente",
            "Contacto directo",
            List.of(new ServiceItem("Compraventa", "Te acompañamos en todo el proceso"),
                    new ServiceItem("Alquiler", "Pisos, locales y garajes"),
                    new ServiceItem("Valoración gratuita", "Conoce el valor real de tu inmueble")))),
        Map.entry("AGENCIA_IA", new SectorConfig("🤖", "#6d28d9",
            "Automatiza tu negocio con IA", "Habla con nuestro agente y descubre las posibilidades",
            "Nuestros servicios", "💬 Habla con nuestro agente IA",
            "Descubre el potencial de la IA", "Te mostramos cómo automatizar tu negocio", "Hablar con el agente",
            "Contacto directo",
            List.of(new ServiceItem("Automatizaciones", "Workflows que trabajan por ti"),
                    new ServiceItem("Agentes IA", "Atención al cliente 24/7 con IA"),
                    new ServiceItem("Integraciones", "CRM, WhatsApp, Google y mucho más")))),
        Map.entry("PINTOR", new SectorConfig("🎨", "#1e40af",
            "Pintores profesionales en Mallorca", "Presupuesto gratuito en menos de 24h — responde nuestro agente",
            "Nuestros servicios", "💬 Pide presupuesto gratuito",
            "Presupuesto sin compromiso", "Te respondemos hoy mismo", "Pedir presupuesto",
            "Contacto directo",
            List.of(new ServiceItem("Pintura interior y exterior", "Viviendas, locales y fachadas con acabados profesionales"),
                    new ServiceItem("Lacados y esmaltes", "Muebles, puertas y carpintería con cabinas presurizadas"),
                    new ServiceItem("Protección de madera", "Tratamientos y barnices para larga duración"))))
    );

    // ── English content ──────────────────────────────────────────────────────

    private static final SectorConfig DEFAULT_EN = new SectorConfig(
        "🏢", "#4f46e5",
        "The best service for your business", "Talk to our agent and discover how we help you",
        "Our services", "💬 Talk to our agent",
        "Book your appointment or inquiry", "Our agent replies in under 1 minute", "Start chat",
        "Contact us",
        List.of(new ServiceItem("Premium service", "Personalised customer care"),
                new ServiceItem("Fast responses", "Available 24 hours a day"),
                new ServiceItem("Trust", "Years of experience in the sector")));

    private static final Map<String, SectorConfig> SECTORS_EN = Map.ofEntries(
        Map.entry("PERRUQUERIA", new SectorConfig("✂️", "#7c3aed",
            "Your style, our passion", "Book your appointment in 30 seconds via chat",
            "Our services", "💬 Talk to our agent",
            "Book your appointment now", "Our agent replies in under 1 minute", "Start chat",
            "Contact us",
            List.of(new ServiceItem("Cut & style", "Personalised design for every client"),
                    new ServiceItem("Colouring", "Modern techniques with premium products"),
                    new ServiceItem("Treatments", "Hydration, keratin and hair nourishment")))),
        Map.entry("RESTAURANTE", new SectorConfig("🍽️", "#ea580c",
            "Flavours that will bring you back", "Reserve your table now via chat",
            "Our menu", "💬 Reserve your table now",
            "Reserve your table", "We confirm your booking in minutes", "Reserve table",
            "Contact us",
            List.of(new ServiceItem("Daily menu", "Fresh market cuisine every day"),
                    new ServiceItem("Events", "Corporate dinners and celebrations"),
                    new ServiceItem("Takeaway", "Collect or home delivery")))),
        Map.entry("FISIOTERAPEUTA", new SectorConfig("🏃", "#0891b2",
            "Recover your movement", "Talk to our specialist via chat",
            "Our services", "💬 Talk to our specialist",
            "Consult your case", "Our physiotherapist replies soon", "Start chat",
            "Contact us",
            List.of(new ServiceItem("Injury recovery", "Personalised post-injury protocols"),
                    new ServiceItem("Sports physiotherapy", "For athletes and active people"),
                    new ServiceItem("Therapeutic Pilates", "Strengthening and prevention")))),
        Map.entry("AGENCIA_IA", new SectorConfig("🤖", "#6d28d9",
            "Automate your business with AI", "Talk to our agent and discover the possibilities",
            "Our services", "💬 Talk to our AI agent",
            "Discover AI potential", "We show you how to automate your business", "Talk to agent",
            "Contact us",
            List.of(new ServiceItem("Automations", "Workflows that work for you"),
                    new ServiceItem("AI Agents", "Customer support 24/7 with AI"),
                    new ServiceItem("Integrations", "CRM, WhatsApp, Google and much more"))))
    );

    // ── German content ───────────────────────────────────────────────────────

    private static final SectorConfig DEFAULT_DE = new SectorConfig(
        "🏢", "#4f46e5",
        "Der beste Service für Ihr Unternehmen", "Sprechen Sie mit unserem Agenten",
        "Unsere Leistungen", "💬 Mit unserem Agenten sprechen",
        "Termin oder Anfrage", "Unser Agent antwortet in unter 1 Minute", "Chat starten",
        "Kontakt",
        List.of(new ServiceItem("Premium-Service", "Persönliche Kundenbetreuung"),
                new ServiceItem("Schnelle Antworten", "Rund um die Uhr verfügbar"),
                new ServiceItem("Vertrauen", "Jahrelange Erfahrung in der Branche")));

    private static final Map<String, SectorConfig> SECTORS_DE = Map.ofEntries(
        Map.entry("PERRUQUERIA", new SectorConfig("✂️", "#7c3aed",
            "Ihr Stil, unsere Leidenschaft", "Buchen Sie Ihren Termin in 30 Sekunden per Chat",
            "Unsere Leistungen", "💬 Mit unserem Agenten sprechen",
            "Termin jetzt buchen", "Unser Agent antwortet in unter 1 Minute", "Chat starten",
            "Kontakt",
            List.of(new ServiceItem("Schnitt & Styling", "Persönliches Design für jeden Kunden"),
                    new ServiceItem("Färben", "Moderne Techniken mit Premium-Produkten"),
                    new ServiceItem("Behandlungen", "Feuchtigkeitspflege und Keratin")))),
        Map.entry("RESTAURANTE", new SectorConfig("🍽️", "#ea580c",
            "Aromen, die Sie zurückbringen", "Tisch jetzt per Chat reservieren",
            "Unsere Speisekarte", "💬 Tisch jetzt reservieren",
            "Ihren Tisch reservieren", "Wir bestätigen Ihre Reservierung in Minuten", "Tisch reservieren",
            "Kontakt",
            List.of(new ServiceItem("Tagesmenü", "Frische Marktküche täglich"),
                    new ServiceItem("Veranstaltungen", "Firmendinner und Feiern"),
                    new ServiceItem("Zum Mitnehmen", "Abholen oder Heimlieferung")))),
        Map.entry("AGENCIA_IA", new SectorConfig("🤖", "#6d28d9",
            "Automatisieren Sie Ihr Unternehmen mit KI", "Sprechen Sie mit unserem Agenten",
            "Unsere Leistungen", "💬 Mit unserem KI-Agenten sprechen",
            "KI-Potenzial entdecken", "Wir zeigen Ihnen die Automatisierung", "Mit Agent sprechen",
            "Kontakt",
            List.of(new ServiceItem("Automatisierungen", "Workflows die für Sie arbeiten"),
                    new ServiceItem("KI-Agenten", "Kundensupport 24/7 mit KI"),
                    new ServiceItem("Integrationen", "CRM, WhatsApp, Google und mehr"))))
    );

    // ── Locale resolution ────────────────────────────────────────────────────

    private SectorConfig getSectorConfig(String sector, String locale) {
        String key = sector != null ? sector.toUpperCase() : "";
        String loc = locale != null ? locale.toLowerCase() : "ca";
        return switch (loc) {
            case "es" -> SECTORS_ES.getOrDefault(key, DEFAULT_ES);
            case "en" -> SECTORS_EN.getOrDefault(key, SECTORS_ES.getOrDefault(key, DEFAULT_EN));
            case "de" -> SECTORS_DE.getOrDefault(key, SECTORS_EN.getOrDefault(key, DEFAULT_DE));
            default   -> SECTORS_CA.getOrDefault(key, DEFAULT_CA);
        };
    }

    private String langName(String locale) {
        return switch (locale != null ? locale.toLowerCase() : "ca") {
            case "es" -> "español";
            case "en" -> "English";
            case "de" -> "Deutsch";
            default   -> "català";
        };
    }

    // ── Public API ───────────────────────────────────────────────────────────

    @Transactional
    public Tenant getOrCreateDemoTenant(String sector) {
        String slug = "demo_" + (sector != null ? sector.toLowerCase() : "general");
        return tenantRepository.findBySlug(slug).orElseGet(() -> {
            var t = Tenant.builder()
                    .name("Demo " + formatSectorLabel(sector))
                    .slug(slug)
                    .isFree(true)
                    .isActive(true)
                    .build();
            log.info("Creating demo tenant for sector {}: slug={}", sector, slug);
            return tenantRepository.save(t);
        });
    }

    @Transactional
    public String createAndPublishDemoLanding(UUID tenantId, UUID token, String sector,
                                              String companyName, String locale) {
        String loc = locale != null && !locale.isBlank() ? locale : "ca";
        String slug = "demo-" + token;
        var config = getSectorConfig(sector, loc);
        String displayName = (companyName != null && !companyName.isBlank())
                ? companyName : formatSectorLabel(sector);

        String contentJson = buildContentJson(config, displayName, loc);
        String stylesJson  = buildStylesJson(config, displayName, loc);

        var landing = Landing.builder()
                .tenantId(tenantId)
                .serviceId(null)
                .title(displayName + " — Demo AMG")
                .slug(slug)
                .metaDescription(config.subheadline())
                .landingType(LandingType.PRO)
                .status(LandingStatus.DRAFT)
                .build();
        landing = landingRepository.save(landing);

        var version = LandingVersion.builder()
                .landingId(landing.getId())
                .versionNumber(1)
                .locale(loc)
                .status(VersionStatus.DRAFT)
                .content(contentJson)
                .styles(stylesJson)
                .build();
        version = landingVersionRepository.save(version);

        version.setStatus(VersionStatus.PUBLISHED);
        version.setPublishedAt(Instant.now());
        landingVersionRepository.save(version);
        landing.setStatus(LandingStatus.PUBLISHED);
        landing.setPublishedVersionId(version.getId());
        landingRepository.save(landing);

        createChatContext(landing.getId(), sector, displayName, config, contentJson, loc);
        log.info("Demo landing created: slug={}, locale={}, tenant={}", slug, loc, tenantId);
        return slug;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private void createChatContext(UUID landingId, String sector, String businessName,
                                   SectorConfig config, String contentJson, String locale) {
        String systemPrompt = buildPromptFromLandingContent(businessName, sector, config, contentJson, locale);
        var ctx = LandingChatContext.builder()
                .landingId(landingId)
                .businessName(businessName)
                .sector(sector)
                .systemPrompt(systemPrompt)
                .profanityAction("CLOSE")
                .build();
        landingChatContextRepository.save(ctx);
    }

    @SuppressWarnings("unchecked")
    private String buildPromptFromLandingContent(String businessName, String sector,
                                                  SectorConfig config, String contentJson,
                                                  String locale) {
        String heroTitle    = config.headline();
        String heroSubtitle = config.subheadline();
        var serviceLines    = new StringBuilder();

        try {
            var root   = objectMapper.readValue(contentJson, java.util.Map.class);
            var blocks = (java.util.List<?>) root.get("blocks");
            if (blocks != null) {
                for (var b : blocks) {
                    var block = (Map<String, Object>) b;
                    var props = (Map<String, Object>) block.getOrDefault("props", Map.of());
                    if ("hero".equals(block.get("type"))) {
                        heroTitle    = str(props, "title",    heroTitle);
                        heroSubtitle = str(props, "subtitle", heroSubtitle);
                    } else if ("services".equals(block.get("type"))) {
                        var items = (java.util.List<?>) props.getOrDefault("items", List.of());
                        for (var item : items) {
                            var im = (Map<String, Object>) item;
                            serviceLines.append("  - ")
                                    .append(str(im, "title", ""))
                                    .append(": ")
                                    .append(str(im, "description", ""))
                                    .append("\n");
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse landing content for prompt: {}", e.getMessage());
            config.services().forEach(s ->
                serviceLines.append("  - ").append(s.title()).append(": ").append(s.description()).append("\n"));
        }

        String lang = langName(locale);

        return "You are the virtual assistant of " + businessName + ".\n\n" +
               "THE LANDING PAGE PRESENTS:\n" +
               "Headline: \"" + heroTitle + "\"\n" +
               "Subtitle: \"" + heroSubtitle + "\"\n\n" +
               "SERVICES OFFERED:\n" + serviceLines +
               "\nROLE:\n" +
               "- Answer questions about the above services using the same words as the landing page\n" +
               "- Motivate the visitor to book an appointment or make contact\n" +
               "- If asked about prices, say it depends on the service and invite them to contact you\n" +
               "- If you don't know the answer, invite them to call or write via the contact form\n\n" +
               "LANGUAGE RULE (CRITICAL): Detect the language of EACH message the visitor sends and " +
               "ALWAYS respond in that SAME language. The landing is in " + lang + ", but if the visitor " +
               "writes in Spanish, respond in Spanish. If they write in English, respond in English. " +
               "If they write in German, respond in German. If they write in Catalan, respond in Catalan. " +
               "NEVER switch languages mid-conversation unless the visitor does.\n\n" +
               "STYLE: Short responses (max 3 sentences), friendly, coherent with the landing content.";
    }

    private String buildContentJson(SectorConfig config, String displayName, String locale) {
        try {
            var blocks = new ArrayList<Map<String, Object>>();

            var hero = new LinkedHashMap<String, Object>();
            hero.put("id", "blk_" + shortId());
            hero.put("type", "hero");
            var heroProps = new LinkedHashMap<String, Object>();
            heroProps.put("title", config.headline());
            heroProps.put("subtitle", config.subheadline());
            heroProps.put("ctaText", config.ctaHeroText());
            heroProps.put("ctaAction", "chat");
            hero.put("props", heroProps);
            blocks.add(hero);

            var services = new LinkedHashMap<String, Object>();
            services.put("id", "blk_" + shortId());
            services.put("type", "services");
            var svcProps = new LinkedHashMap<String, Object>();
            svcProps.put("title", config.servicesTitle());
            var items = config.services().stream()
                    .map(s -> { var m = new LinkedHashMap<String, Object>();
                                m.put("title", s.title()); m.put("description", s.description()); return m; })
                    .toList();
            svcProps.put("items", items);
            services.put("props", svcProps);
            blocks.add(services);

            var chatCta = new LinkedHashMap<String, Object>();
            chatCta.put("id", "blk_" + shortId());
            chatCta.put("type", "chat-cta");
            var ctaProps = new LinkedHashMap<String, Object>();
            ctaProps.put("title", config.ctaSectionTitle());
            ctaProps.put("subtitle", config.ctaSectionSubtitle());
            ctaProps.put("buttonText", config.ctaSectionBtn());
            ctaProps.put("accentColor", config.primaryColor());
            chatCta.put("props", ctaProps);
            blocks.add(chatCta);

            var contact = new LinkedHashMap<String, Object>();
            contact.put("id", "blk_" + shortId());
            contact.put("type", "contact-form");
            contact.put("props", Map.of("title", config.contactTitle()));
            blocks.add(contact);

            var root = new LinkedHashMap<String, Object>();
            root.put("blocks", blocks);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            log.error("Error building demo landing content JSON", e);
            return "{\"blocks\":[]}";
        }
    }

    private String buildStylesJson(SectorConfig config, String displayName, String locale) {
        try {
            var styles = new LinkedHashMap<String, Object>();
            styles.put("primaryColor", config.primaryColor());
            styles.put("accentColor", config.primaryColor());
            styles.put("fontHeading", "Montserrat, sans-serif");
            styles.put("fontBody", "'Open Sans', sans-serif");
            styles.put("chatEnabled", true);
            styles.put("chatBusinessName", displayName);
            styles.put("language", locale != null ? locale : "ca");
            return objectMapper.writeValueAsString(styles);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String str(Map<String, Object> map, String key, String fallback) {
        var v = map.get(key);
        return (v != null && !v.toString().isBlank()) ? v.toString() : fallback;
    }

    private String formatSectorLabel(String sector) {
        if (sector == null || sector.isBlank()) return "Negoci";
        return switch (sector.toUpperCase()) {
            case "PERRUQUERIA"        -> "Perruqueria";
            case "ESTETICA"           -> "Estètica";
            case "FISIOTERAPEUTA"     -> "Fisioteràpia";
            case "PSICOLEG"           -> "Psicologia";
            case "NUTRICIONISTA"      -> "Nutrició";
            case "TALLER_MECANIC"     -> "Taller Mecànic";
            case "VETERINARI"         -> "Veterinari";
            case "RESTAURANTE"        -> "Restaurant";
            case "ELECTRICISTA"       -> "Electricista";
            case "FONTANER"           -> "Fontaneria";
            case "JARDINER"           -> "Jardineria";
            case "NETEJA"             -> "Neteja";
            case "GESTORIA"           -> "Gestoria";
            case "ACADEMIA"           -> "Acadèmia";
            case "PERRUQUERIA_CANINA" -> "Perruqueria Canina";
            case "INMOBILIARIA"       -> "Immobiliària";
            case "AGENCIA_IA"         -> "Agència IA";
            default -> sector.substring(0, 1).toUpperCase() +
                       sector.substring(1).toLowerCase().replace("_", " ");
        };
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
