package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class TemplateSeeder implements CommandLineRunner {

    private final LandingTemplateRepository templateRepository;
    private final TemplateSectionRepository sectionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        seedIfAbsent("restaurant",    this::seedRestaurant);
        seedIfAbsent("profesional",   this::seedProfesional);
        seedIfAbsent("comercio",      this::seedComercio);
        seedIfAbsent("evento",        this::seedEvento);
        seedIfAbsent("basica",        this::seedBasica);
        seedIfAbsent("serveis",       this::seedServeis);
        seedIfAbsent("salut",         this::seedSalut);
        seedIfAbsent("taller",        this::seedTaller);
        seedIfAbsent("immobiliaria",  this::seedImmobiliaria);
    }

    private void seedIfAbsent(String slug, Runnable seeder) {
        if (templateRepository.findBySlug(slug).isEmpty()) {
            log.info("Seeding template: {}", slug);
            seeder.run();
        }
    }

    private void addSections(LandingTemplate template, List<SectionDef> sections) {
        int order = 1;
        for (var def : sections) {
            var section = TemplateSection.builder()
                    .templateId(template.getId())
                    .blockType(BlockType.valueOf(def.blockType))
                    .sortOrder(order++)
                    .propsSchema(toJson(def.propsSchema))
                    .defaultProps(toJson(def.defaultProps))
                    .build();
            sectionRepository.save(section);
        }
    }

    private String toJson(Map<String, Object> map) {
        try { return objectMapper.writeValueAsString(map); }
        catch (Exception e) { return "{}"; }
    }

    private Map<String, Object> schema(String... keys) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keys.length; i += 2) {
            Map<String, Object> field = new LinkedHashMap<>();
            field.put("type", keys[i + 1]);
            field.put("label", keys[i]);
            map.put(keys[i], field);
        }
        return map;
    }

    private Map<String, Object> props(Object... entries) {
        var map = new LinkedHashMap<String, Object>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], entries[i + 1]);
        }
        return map;
    }

    private record SectionDef(String blockType, Map<String, Object> propsSchema, Map<String, Object> defaultProps) {}

    private List<Map<String, Object>> hours(String monFri, String sat, String sun) {
        String[] mfParts = monFri.split("-");
        List<String[]> days = List.of(
                new String[]{"Dilluns", mfParts[0], mfParts[1]},
                new String[]{"Dimarts", mfParts[0], mfParts[1]},
                new String[]{"Dimecres", mfParts[0], mfParts[1]},
                new String[]{"Dijous", mfParts[0], mfParts[1]},
                new String[]{"Divendres", mfParts[0], mfParts[1]},
                new String[]{"Dissabte", sat.isEmpty() ? "" : sat.split("-")[0], sat.isEmpty() ? "" : sat.split("-")[1]},
                new String[]{"Diumenge", sun.isEmpty() ? "" : sun.split("-")[0], sun.isEmpty() ? "" : sun.split("-")[1]}
        );
        return days.stream().map(d -> {
            var m = new LinkedHashMap<String, Object>();
            m.put("day", d[0]);
            m.put("open", d[1]);
            m.put("close", d[2]);
            m.put("closed", d[1].isEmpty());
            return (Map<String, Object>) m;
        }).toList();
    }

    // ─── RESTAURANT ──────────────────────────────────────────────────────────

    private void seedRestaurant() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Restaurant").slug("restaurant")
                .description("Restaurants i bars").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text", "links", "array", "ctaText", "text"),
                        props("logoText", "Restaurant", "links", List.of(
                                Map.of("label", "La carta", "href", "#services"),
                                Map.of("label", "Horaris", "href", "#hours"),
                                Map.of("label", "Reserves", "href", "#contact")
                        ), "ctaText", "Reserva taula", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "bgImage", "image"),
                        props("title", "Cuina mallorquina de mercat", "subtitle", "Producte local, receptes de sempre. A dos minuts del centre de Palma.", "ctaText", "Reserva taula", "ctaLink", "#contact", "badgeText", "Obert des de 1987 · Cuina de proximitat")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "35+", "label", "Anys oberts", "icon", "🏆"),
                                Map.of("number", "4.8★", "label", "Valoració Google", "icon", "⭐"),
                                Map.of("number", "200+", "label", "Plats a la carta", "icon", "🍽️")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "La nostra carta", "items", List.of(
                                Map.of("name", "Cuina mallorquina", "desc", "Arròs brut, tumbet, porcella i les receptes de tota la vida fetes amb producte de mercat.", "icon", "🥘"),
                                Map.of("name", "Menú del dia", "desc", "Primer, segon i postres per 13,50€. De dilluns a divendres al migdia.", "icon", "🍱"),
                                Map.of("name", "Carta de temporada", "desc", "Plats que canvien cada mes segons el mercat local i el millor producte de l'illa.", "icon", "🌿"),
                                Map.of("name", "Reserves per a grups", "desc", "Celebracions i dinars d'empresa fins a 50 persones. Menú personalitzat.", "icon", "🎉")
                        ))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "El que diuen els nostres clients", "items", List.of(
                                Map.of("name", "Maria Bauçà", "role", "Client habitual", "text", "El millor arròs brut de Palma, sense dubte. Hi anam cada setmana amb la família. El tracte és excel·lent i el preu molt raonable.", "rating", 5),
                                Map.of("name", "Joan Riera", "role", "Dinar d'empresa", "text", "Vam fer el dinar de Nadal aquí i tot va ser perfecte. El menú personalitzat, el servei atent i l'ambient càlid. Molt recomanable.", "rating", 5),
                                Map.of("name", "Ana Martínez", "role", "Turista", "text", "Cercàvem cuina mallorquina autèntica i la vam trobar. El tumbet i la porcella, inoblidades. Tornarem segur.", "rating", 5)
                        ))),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "El nostre local i els nostres plats", "images", List.of())),
                new SectionDef("REVIEWS", schema("title", "text", "items", "array"),
                        props("title", "El que diuen a Google", "googleMapsUrl", "", "items", List.of(
                                Map.of("name", "Catalina Moll", "rating", 5, "text", "Anàrem per primera vegada i tornarem moltes vegades. La cuina mallorquina autèntica que feia temps que cercàvem. El servei, impecable.", "date", "2026-03-10"),
                                Map.of("name", "Ralf Bauer", "rating", 5, "text", "Best Mallorcan food we had during our stay. Authentic, delicious and at a great price. The staff was very welcoming.", "date", "2026-02-15"),
                                Map.of("name", "Pere Antoni Nadal", "rating", 5, "text", "Hi anam cada setmana amb els de la feina. Mai ens ha fallat: el menú del dia és una ganga i la qualitat, sempre constant.", "date", "2026-01-20")
                        ))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris", "hours", hours("13:00-16:00", "13:00-16:00", "13:00-16:00"))),
                new SectionDef("CHAT_CTA", schema("title", "text"),
                        props("title", "Reserva la teva taula ara", "subtitle", "El nostre assistent respon en menys d'1 minut", "buttonText", "Parla amb nosaltres")),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "Reserva la teva taula avui", "subtitle", "Places limitades. Reserva ara i assegura el teu lloc.", "ctaText", "Reservar ara", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Fes la teva reserva", "subtitle", "Respondrem en menys de 2 hores", "email", "", "phone", "")),
                new SectionDef("MAP", schema("address", "text", "lat", "text", "lng", "text"),
                        props("title", "On trobar-nos", "address", "Palma de Mallorca", "lat", "39.5696", "lng", "2.6502")),
                new SectionDef("FOOTER", schema("businessName", "text", "address", "text"),
                        props("businessName", "Restaurant", "tagline", "Cuina mallorquina de mercat des de 1987", "links", List.of(
                                Map.of("label", "La carta", "href", "#services"),
                                Map.of("label", "Horaris", "href", "#hours"),
                                Map.of("label", "Reserves", "href", "#contact")
                        ), "copyright", "© 2026 · Tots els drets reservats"))
        ));
    }

    // ─── PROFESSIONAL ─────────────────────────────────────────────────────────

    private void seedProfesional() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Professional").slug("profesional")
                .description("Advocats, metges, assessors").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "Nom Professional", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Experiència", "href", "#about"),
                                Map.of("label", "Consulta", "href", "#contact")
                        ), "ctaText", "Primera consulta gratuïta", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "La defensa legal que el teu negoci mereix", "subtitle", "Assessorament jurídic especialitzat per a empreses i autònoms a Mallorca. Més de 20 anys resolent el que els altres no poden.", "ctaText", "Primera consulta gratuïta", "ctaLink", "#contact", "badgeText", "Col·legiat nº 1234 · 20 anys d'experiència")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "500+", "label", "Casos resolts", "icon", "⚖️"),
                                Map.of("number", "20", "label", "Anys d'experiència", "icon", "🏆"),
                                Map.of("number", "98%", "label", "Clients satisfets", "icon", "✓")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Àrees d'especialització", "items", List.of(
                                Map.of("name", "Dret mercantil", "desc", "Constitució d'empreses, contractes mercantils i assessorament a societats.", "icon", "🏢"),
                                Map.of("name", "Dret laboral", "desc", "Acomiadaments, ERTOs, inspeccions de treball i relacions laborals.", "icon", "👔"),
                                Map.of("name", "Dret fiscal", "desc", "Planificació fiscal, recursos tributaris i representació davant Hisenda.", "icon", "📋"),
                                Map.of("name", "Propietat intel·lectual", "desc", "Registre de marques, patents i protecció del teu actiu intangible.", "icon", "💡")
                        ))),
                new SectionDef("TRUST_BAR", schema("title", "text", "items", "array"),
                        props("title", "Col·legiats i certificats", "items", List.of(
                                Map.of("name", "Il·lustre Col·legi d'Advocats", "icon", "⚖️"),
                                Map.of("name", "Protecció de dades RGPD", "icon", "🔒"),
                                Map.of("name", "+500 casos resolts", "icon", "✅"),
                                Map.of("name", "Membre ICAIB", "icon", "🏛")
                        ))),
                new SectionDef("STEPS", schema("title", "text", "items", "array"),
                        props("title", "Com treballem", "items", List.of(
                                Map.of("number", "1", "title", "Primera consulta", "description", "Anàlisi gratuïta del teu cas. Sense compromisos, sense sorpreses."),
                                Map.of("number", "2", "title", "Estratègia personalitzada", "description", "Dissenyem un pla d'acció adaptat als teus objectius i pressupost."),
                                Map.of("number", "3", "title", "Execució i seguiment", "description", "Gestionem tot el procés i t'informem en cada pas fins a la resolució.")
                        ))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients que confien en nosaltres", "items", List.of(
                                Map.of("name", "Pere Vidal", "role", "Empresari", "text", "Gràcies al seu assessorament vam evitar una sanció d'Hisenda de 40.000€. Professionals de primera, accessibles i directes.", "rating", 5),
                                Map.of("name", "Cristina Torres", "role", "Autònoma", "text", "Em van ajudar en una disputa laboral complexa i el resultat va ser millor del que esperava. 100% recomanables.", "rating", 5),
                                Map.of("name", "Marc Ferrer", "role", "Director comercial", "text", "La nostra empresa porta 8 anys amb ells. Sempre disponibles i amb solucions clares. Indispensables.", "rating", 5)
                        ))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "La primera consulta és gratuïta?", "answer", "Sí, la primera consulta de fins a 30 minuts és completament gratuïta i sense compromís."),
                                Map.of("question", "Quant tarda a resoldre's un cas?", "answer", "Depèn de la complexitat. Un contracte mercantil pot estar en 3-5 dies; un litigi pot trigar mesos. T'informarem des del principi."),
                                Map.of("question", "Treballeu amb empreses petites?", "answer", "Sí, la majoria dels nostres clients són pimes i autònoms. Tenim tarifes adaptades a cada situació.")
                        ))),
                new SectionDef("PRICING", schema("title", "text", "items", "array"),
                        props("title", "Tarifes transparents", "items", List.of(
                                Map.of("name", "Consulta puntual", "description", "Per a autònoms i particulars", "price", "150", "period", "hora", "features", List.of("Consulta d'1h en profunditat", "Informe escrit amb recomanacions", "Seguiment per email 30 dies"), "highlighted", false),
                                Map.of("name", "Assessoria mensual", "description", "Per a empreses i pimes", "price", "250", "period", "mes", "features", List.of("Assessorament il·limitat", "Revisió de contractes", "Representació en gestions", "Resposta en 24h garantida"), "highlighted", true),
                                Map.of("name", "Pack defensa", "description", "Per a litigis i disputes", "price", "Pressupost", "period", "cas", "features", List.of("Anàlisi inicial gratuïta", "Estratègia personalitzada", "Representació completa", "Sense sorpreses"), "highlighted", false)
                        ))),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "Resol el teu problema legal avui", "subtitle", "Primera consulta gratuïta · Resposta en 24h", "ctaText", "Parla amb un expert", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Sol·licita la teva consulta gratuïta", "subtitle", "T'atenem en menys de 24h · Confidencialitat garantida")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Bufet d'Advocats", "tagline", "Assessorament jurídic per a empreses i autònoms", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Sobre nosaltres", "href", "#about"),
                                Map.of("label", "Contacte", "href", "#contact")
                        )))
        ));
    }

    // ─── COMERÇ ───────────────────────────────────────────────────────────────

    private void seedComercio() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Comerç").slug("comercio")
                .description("Botigues i comerços locals").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "La nostra botiga", "links", List.of(
                                Map.of("label", "Productes", "href", "#services"),
                                Map.of("label", "Horaris", "href", "#hours"),
                                Map.of("label", "Contacte", "href", "#contact")
                        ), "ctaText", "Visita'ns", "ctaLink", "#hours")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "Productes locals que marquen la diferència", "subtitle", "La botiga de referència del barri des de fa 20 anys. Producte artesanal, tracte personal i qualitat garantida.", "ctaText", "Descobreix la nostra oferta", "ctaLink", "#services", "badgeText", "Botiga local · Producte artesanal")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "La nostra oferta", "items", List.of(
                                Map.of("name", "Producte fresc", "desc", "Selecció diària del millor producte local i de temporada directe del productor.", "icon", "🛒"),
                                Map.of("name", "Producte artesanal", "desc", "Elaboració pròpia i productes artesans de productors de l'illa.", "icon", "🏺"),
                                Map.of("name", "Assessorament personalitzat", "desc", "Et recomanem el millor producte per a la teva necessitat. Sense pressa.", "icon", "💬"),
                                Map.of("name", "Lliurament a domicili", "desc", "Portem la teva compra a casa. Servei de repartiment al municipi.", "icon", "🚚")
                        ))),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "El nostre local", "images", List.of())),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "El que diuen els nostres clients", "items", List.of(
                                Map.of("name", "Antònia Mas", "role", "Client de tota la vida", "text", "Hi vaig des de fa 15 anys i mai m'han decebut. El producte sempre fresc i la Marta sempre amb un somriure. Imprescindible.", "rating", 5),
                                Map.of("name", "Bernat Sureda", "role", "Client nou", "text", "Vaig trobar productes que no trobo als supermercats grans. El tracte és molt proper i els preus raonables.", "rating", 5)
                        ))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris d'atenció", "hours", hours("09:30-13:30", "09:30-13:30", ""))),
                new SectionDef("MAP", schema("address", "text"),
                        props("title", "On som", "address", "Carrer Principal, Mallorca", "lat", "39.5696", "lng", "2.6502")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Contacta amb nosaltres", "subtitle", "Per a comandes o consultes especials")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "La nostra botiga", "tagline", "Producte local amb tracte personal", "links", List.of(
                                Map.of("label", "Productes", "href", "#services"),
                                Map.of("label", "Horaris", "href", "#hours"),
                                Map.of("label", "Contacte", "href", "#contact")
                        )))
        ));
    }

    // ─── EVENTO ───────────────────────────────────────────────────────────────

    private void seedEvento() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Esdeveniment").slug("evento")
                .description("Celebracions i events").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "Una experiència que no oblidaràs", "subtitle", "Organitzem l'event perfecte per a cada moment especial. Gestió integral, zero preocupacions.", "ctaText", "Demana pressupost", "ctaLink", "#contact", "badgeText", "Més de 200 events celebrats")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Tipus d'events", "items", List.of(
                                Map.of("name", "Casaments", "desc", "Des del primer sí fins al darrer ball. Organitzem cada detall del vostre dia especial.", "icon", "💍"),
                                Map.of("name", "Events d'empresa", "desc", "Dinars, presentacions, team buildings i convencions. Espais únics a tota Mallorca.", "icon", "🏢"),
                                Map.of("name", "Celebracions privades", "desc", "Aniversaris, comunions i tot tipus de celebracions familiars personalitzades.", "icon", "🎉")
                        ))),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "Els nostres events", "images", List.of())),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "El teu event perfecte t'espera", "subtitle", "Pressupost gratuït · Resposta en 24h", "ctaText", "Demana pressupost ara", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Parla'ns del teu event", "subtitle", "T'enviem pressupost sense compromís en 24h")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Events & Celebracions", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Contacte", "href", "#contact")
                        )))
        ));
    }

    // ─── BÀSICA ───────────────────────────────────────────────────────────────

    private void seedBasica() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Bàsica").slug("basica")
                .description("Landing mínima").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "Benvinguts al nostre negoci", "subtitle", "Qualitat i professionalitat al teu servei. Contacta'ns per saber com podem ajudar-te.", "ctaText", "Contacta'ns", "ctaLink", "#contact")),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Qui som", "body", "<p>Som un negoci local amb anys d'experiència al sector. La nostra missió és oferir el millor servei amb la màxima qualitat i atenció personalitzada.</p><p>Treballem cada dia per superar les expectatives dels nostres clients i mantenir la confiança que han dipositat en nosaltres.</p>")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Contacta amb nosaltres")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "El Negoci", "copyright", "© 2026 · Tots els drets reservats"))
        ));
    }

    // ─── SERVEIS ─────────────────────────────────────────────────────────────

    private void seedServeis() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Serveis").slug("serveis")
                .description("Artesans i serveis a domicili").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "El teu professional", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Garantia", "href", "#about"),
                                Map.of("label", "Pressupost", "href", "#contact")
                        ), "ctaText", "Pressupost gratuït", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "El professional que el teu negoci necessita", "subtitle", "Servei ràpid, garantia total i pressupost transparent. Cobrim tots els municipis de Mallorca.", "ctaText", "Pressupost gratuït", "ctaLink", "#contact", "badgeText", "Assegurat · Garantia 2 anys · Pressupost en 24h")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "1.200+", "label", "Treballs realitzats", "icon", "🔧"),
                                Map.of("number", "10", "label", "Anys d'experiència", "icon", "🏆"),
                                Map.of("number", "24h", "label", "Pressupost garantit", "icon", "⚡")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("name", "Instal·lació", "desc", "Instal·lació professional amb tots els permisos i normatives vigents.", "icon", "🔌"),
                                Map.of("name", "Reparació urgent", "desc", "Atenem emergències en menys de 2 hores. Disponibles tots els dies.", "icon", "🚨"),
                                Map.of("name", "Manteniment preventiu", "desc", "Revisions periòdiques per evitar avaries i garantir el bon funcionament.", "icon", "🔍"),
                                Map.of("name", "Pressupost gratuït", "desc", "Visita de diagnòstic i pressupost detallat sense cap cost ni compromís.", "icon", "📋")
                        ))),
                new SectionDef("TRUST_BAR", schema("title", "text", "items", "array"),
                        props("title", "Per què triar-nos", "items", List.of(
                                Map.of("name", "Assegurat", "icon", "🛡"),
                                Map.of("name", "Garantia 2 anys", "icon", "✅"),
                                Map.of("name", "Pressupost gratuït", "icon", "📋"),
                                Map.of("name", "Disponibles 7 dies", "icon", "📞")
                        ))),
                new SectionDef("STEPS", schema("title", "text", "items", "array"),
                        props("title", "Com treballem", "items", List.of(
                                Map.of("number", "1", "title", "Truques o escrius", "description", "Ens expliques el que necessites. Et respondrem en menys de 2 hores."),
                                Map.of("number", "2", "title", "Visita gratuïta", "description", "Anem a veure el treball, donem el pressupost detallat i sense sorpreses."),
                                Map.of("number", "3", "title", "Feina feta", "description", "Executem el treball amb garantia de 2 anys. Tu no et preocupes de res.")
                        ))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients satisfets", "items", List.of(
                                Map.of("name", "Rafel Llull", "role", "Propietari, Sa Pobla", "text", "Vaig tenir una avaria el dissabte a la nit i van venir en menys d'una hora. El problema resolt i el preu, molt just. Molt recomanables.", "rating", 5),
                                Map.of("name", "Margalida Font", "role", "Restaurant, Pollença", "text", "Fan el manteniment del nostre local des de fa 5 anys. Sempre puntuals, sense sorpreses al pressupost i amb garantia de tot el que fan.", "rating", 5),
                                Map.of("name", "Sebastià Mir", "role", "Particular, Felanitx", "text", "Pressupost clar des del principi, feina neta i ben acabada. Molt diferent d'altres empreses que he provat.", "rating", 5)
                        ))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "En quina zona treballeu?", "answer", "Cobrim tots els municipis de Mallorca, incloent la Part Forana. El desplaçament és gratuït."),
                                Map.of("question", "Quant tarda el pressupost?", "answer", "En menys de 24 hores tindreu el pressupost detallat per escrit. En urgències, en 2 hores."),
                                Map.of("question", "Doneu garantia dels treballs?", "answer", "Sí, tots els treballs tenen garantia de 2 anys en mà d'obra i respectem les garanties del fabricant en materials.")
                        ))),
                new SectionDef("CHAT_CTA", schema("title", "text"),
                        props("title", "Demana el teu pressupost en 2 minuts", "subtitle", "Explica'ns el que necessites i et responem de seguida", "buttonText", "Xateja ara")),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "Pressupost gratuït en menys de 24h", "subtitle", "Sense compromís · Resposta garantida", "ctaText", "Demanar pressupost", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Demana el teu pressupost gratuït", "subtitle", "Respondrem en menys de 2 hores")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Serveis Professionals", "tagline", "Qualitat garantida · Pressupost transparent", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Com treballem", "href", "#steps"),
                                Map.of("label", "Pressupost", "href", "#contact")
                        )))
        ));
    }

    // ─── SALUT ────────────────────────────────────────────────────────────────

    private void seedSalut() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Salut i Bellesa").slug("salut")
                .description("Professionals de la salut i bellesa").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "Centre de Salut", "links", List.of(
                                Map.of("label", "Tractaments", "href", "#services"),
                                Map.of("label", "L'equip", "href", "#team"),
                                Map.of("label", "Cita prèvia", "href", "#contact")
                        ), "ctaText", "Reserva cita", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "El teu benestar, la nostra prioritat", "subtitle", "Tractaments personalitzats per a cada persona. Centre especialitzat amb professionals col·legiats a Mallorca.", "ctaText", "Reserva la teva cita", "ctaLink", "#contact", "badgeText", "Professionals col·legiats · Atenció personalitzada")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "3.000+", "label", "Pacients atesos", "icon", "❤️"),
                                Map.of("number", "12", "label", "Anys d'experiència", "icon", "🏆"),
                                Map.of("number", "4.9★", "label", "Valoració Google", "icon", "⭐")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres tractaments", "items", List.of(
                                Map.of("name", "Fisioteràpia", "desc", "Tractament individualitzat del dolor i la lesió. Torna a la teva activitat habitual.", "icon", "🦴"),
                                Map.of("name", "Nutrició i dietètica", "desc", "Plans nutricionals personalitzats amb seguiment. Resultats visibles en 4 setmanes.", "icon", "🥗"),
                                Map.of("name", "Psicologia", "desc", "Atenció psicològica individual i de parella. Eines per al teu benestar emocional.", "icon", "🧠"),
                                Map.of("name", "Estètica avançada", "desc", "Tractaments facials i corporals amb tecnologia de darrera generació.", "icon", "✨")
                        ))),
                new SectionDef("TRUST_BAR", schema("title", "text", "items", "array"),
                        props("title", "Professionalitat i confiança", "items", List.of(
                                Map.of("name", "Professionals col·legiats", "icon", "🏛"),
                                Map.of("name", "3.000+ pacients atesos", "icon", "❤️"),
                                Map.of("name", "Assegurança responsabilitat civil", "icon", "🛡"),
                                Map.of("name", "Cita en 24h garantida", "icon", "⚡")
                        ))),
                new SectionDef("TEAM", schema("title", "text", "items", "array"),
                        props("title", "El nostre equip", "items", List.of(
                                Map.of("name", "Dra. Marta Valls", "role", "Fisioterapeuta · Col. 1234", "bio", "Especialista en teràpia manual i rehabilitació esportiva. 15 anys d'experiència clínica.", "photo", ""),
                                Map.of("name", "Núria Ferrer", "role", "Nutricionista · Col. 5678", "bio", "Experta en nutrició clínica i esportiva. Màster en obesitat i trastorns alimentaris.", "photo", "")
                        ))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Experiències dels nostres pacients", "items", List.of(
                                Map.of("name", "Joana Perelló", "role", "Pacient de fisioteràpia", "text", "Tenia dolor lumbar crònic des de feia 2 anys. Després de 8 sessions, he recuperat la mobilitat i puc fer vida normal. Un canvi increïble.", "rating", 5),
                                Map.of("name", "Miquel Tomàs", "role", "Pacient de nutrició", "text", "Vaig perdre 12 kg en 4 mesos amb el pla de la Núria. Sense passar gana i aprenent a menjar bé per sempre. Molt recomanable.", "rating", 5),
                                Map.of("name", "Elena Coll", "role", "Pacient de psicologia", "text", "La Dra. Valls m'ha ajudat a gestionar l'ansietat que tenia des de feia anys. Ambiente segur, professional i sense jutjar.", "rating", 5)
                        ))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "Cal demanar cita prèvia?", "answer", "Sí, totes les visites són amb cita prèvia per garantir l'atenció personalitzada. Pots reservar per telèfon, WhatsApp o el formulari."),
                                Map.of("question", "Acceptau assegurances mèdiques?", "answer", "Treballem amb les principals assegurances. Consulta si la teva cobreix els nostres tractaments."),
                                Map.of("question", "Quant dura la primera visita?", "answer", "La primera visita dura entre 45 i 60 minuts. Fem una valoració completa i dissenyem el pla de tractament.")
                        ))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris d'atenció", "hours", hours("09:00-20:00", "09:00-14:00", ""))),
                new SectionDef("CHAT_CTA", schema("title", "text"),
                        props("title", "Reserva la teva cita en 1 minut", "subtitle", "El nostre assistent t'ajudarà a trobar el millor horari", "buttonText", "Xateja amb nosaltres")),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "Comença el teu camí cap al benestar", "subtitle", "Primera visita sense compromís · Places limitades", "ctaText", "Reservar cita ara", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Reserva la teva cita", "subtitle", "Respondrem en menys de 2 hores")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Centre de Salut", "tagline", "Cuidem el teu benestar amb la màxima professionalitat", "links", List.of(
                                Map.of("label", "Tractaments", "href", "#services"),
                                Map.of("label", "L'equip", "href", "#team"),
                                Map.of("label", "Cita prèvia", "href", "#contact")
                        )))
        ));
    }

    // ─── TALLER ───────────────────────────────────────────────────────────────

    private void seedTaller() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Taller Mecànic").slug("taller")
                .description("Tallers i serveis d'automoció").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "Taller Oficial", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Preguntes", "href", "#faq"),
                                Map.of("label", "Cita prèvia", "href", "#contact")
                        ), "ctaText", "Demana cita", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "El taller de confiança del teu vehicle", "subtitle", "Mecànics certificats, pressupost transparent i garantia en tots els treballs. Sense sorpreses a la factura.", "ctaText", "Demana cita prèvia", "ctaLink", "#contact", "badgeText", "Taller autoritzat · Garantia 2 anys · Pressupost gratis")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "2.500+", "label", "Vehicles reparats", "icon", "🚗"),
                                Map.of("number", "20", "label", "Anys d'experiència", "icon", "🔧"),
                                Map.of("number", "0€", "label", "Diagnòstic inicial", "icon", "✓")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("name", "Revisió i ITV", "desc", "Preparació completa per a la ITV. Revisió de tots els sistemes de seguretat.", "icon", "🔍"),
                                Map.of("name", "Canvi d'oli i filtres", "desc", "Manteniment preventiu amb olis originals. Millora el rendiment del motor.", "icon", "🛢️"),
                                Map.of("name", "Frenada i suspensió", "desc", "Revisió i substitució de frens i amortidors. Seguretat màxima al volant.", "icon", "⚙️"),
                                Map.of("name", "Pneumàtics", "desc", "Venda, muntatge, equilibrat i alineació. Totes les marques al millor preu.", "icon", "🏎️")
                        ))),
                new SectionDef("BEFORE_AFTER", schema("title", "text"),
                        props("title", "Resultats reals dels nostres treballs", "beforeLabel", "Abans de la reparació", "afterLabel", "Resultat final", "beforeImage", "", "afterImage", "")),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients satisfets", "items", List.of(
                                Map.of("name", "Toni Moll", "role", "Client des de 2018", "text", "El pressupost sempre clar i just. Mai m'han facturat res que no s'hagi acordat abans. Hi porto tots els meus vehicles.", "rating", 5),
                                Map.of("name", "Bàrbara Salas", "role", "Primera visita", "text", "Vaig venir per una revisió pre-ITV i em van explicar tot el que calia fer amb fotos i tot. Vaig aprovar a la primera. Molt professionals.", "rating", 5),
                                Map.of("name", "Miquel Riutord", "role", "Particular", "text", "Ràpids, honests i amb un preu molt competitiu. Cada any hi duc el cotxe per la revisió anual. Millor taller de la zona.", "rating", 5)
                        ))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "El diagnòstic és gratuït?", "answer", "Sí, la primera revisió de diagnòstic és completament gratuïta i sense compromís."),
                                Map.of("question", "Quant tarda una reparació?", "answer", "Reparacions bàsiques en el dia. Treballs complexos en 24-48h. T'avisem sempre."),
                                Map.of("question", "Doneu garantia dels treballs?", "answer", "Sí, 2 anys de garantia en mà d'obra i respectem la garantia del fabricant en peces.")
                        ))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris del taller", "hours", hours("08:00-18:00", "09:00-13:00", ""))),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "El teu vehicle mereix els millors professionals", "subtitle", "Cita prèvia en menys de 2h · Diagnòstic gratuït", "ctaText", "Demanar cita ara", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Demana cita o fes-nos una consulta", "subtitle", "Respondrem en menys de 2 hores")),
                new SectionDef("MAP", schema("address", "text"),
                        props("title", "On trobar-nos", "address", "Polígon Industrial, Mallorca", "lat", "39.5696", "lng", "2.6502")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Taller Oficial", "tagline", "Mecànica de confiança des de 2004", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "Horaris", "href", "#hours"),
                                Map.of("label", "Cita prèvia", "href", "#contact")
                        )))
        ));
    }

    // ─── IMMOBILIÀRIA ────────────────────────────────────────────────────────

    private void seedImmobiliaria() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Immobiliària").slug("immobiliaria")
                .description("Agències immobiliàries i gestors de propietats").build());
        addSections(t, List.of(
                new SectionDef("HEADER", schema("logoText", "text"),
                        props("logoText", "Immobiliària", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "L'agència", "href", "#about"),
                                Map.of("label", "Contacte", "href", "#contact")
                        ), "ctaText", "Consulta gratuïta", "ctaLink", "#contact")),
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text"),
                        props("title", "Troba la propietat que has somiat", "subtitle", "Especialistes en el mercat immobiliari de Mallorca des de fa 25 anys. Compra, venda i lloguer amb assessorament integral.", "ctaText", "Consulta gratuïta", "ctaLink", "#contact", "badgeText", "25 anys al mercat · +1.000 operacions tancades")),
                new SectionDef("STATS", schema("items", "array"),
                        props("items", List.of(
                                Map.of("number", "1.200+", "label", "Propietats venudes", "icon", "🏠"),
                                Map.of("number", "25", "label", "Anys d'experiència", "icon", "🏆"),
                                Map.of("number", "98%", "label", "Clients repeteixen", "icon", "⭐")
                        ))),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("name", "Compra assessorada", "desc", "T'acompanyem en tot el procés: cerca, negociació, finançament i firma. Sense sorpreses.", "icon", "🔑"),
                                Map.of("name", "Venda ràpida", "desc", "Valoració gratuïta i pla de venda personalitzat. Aconseguim el millor preu en el mínim temps.", "icon", "💶"),
                                Map.of("name", "Gestió de lloguers", "desc", "Gestió integral per a propietaris: selecció d'inquilins, contractes i manteniment.", "icon", "📋"),
                                Map.of("name", "Propietats de luxe", "desc", "Cartera exclusiva de propietats premium a Mallorca per a compradors nacionals i internacionals.", "icon", "🌟")
                        ))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients que han confiat en nosaltres", "items", List.of(
                                Map.of("name", "Laura Vidal", "role", "Compradora", "text", "Ens van trobar exactament el que buscàvem en menys de 3 setmanes. La gestió del finançament i la firma, impecable. Sense cap estrès.", "rating", 5),
                                Map.of("name", "Robert Müller", "role", "Comprador alemany", "text", "We bought our dream property in Mallorca with their help. Perfect communication in German, English and Spanish. Absolutely recommend.", "rating", 5),
                                Map.of("name", "Francisca Morro", "role", "Propietària", "text", "Vaig vendre el meu pis en 45 dies al preu que demanàvem. Gestió professional, comunicació constant i zero problemes.", "rating", 5)
                        ))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "Quina és la vostra comissió de venda?", "answer", "La nostra comissió és un percentatge sobre el preu de venda, inclòs en el preu final. Et detallarem tot a la consulta inicial gratuïta."),
                                Map.of("question", "Quant tarda a vendre's una propietat?", "answer", "En mercat estàndard, entre 2 i 6 mesos. Amb el nostre pla de màrqueting actiu, accelerem el procés considerablement."),
                                Map.of("question", "Treballeu amb compradors internacionals?", "answer", "Sí, el 40% dels nostres clients provenen d'Alemanya, Regne Unit i Escandinávia. Tenim equip multilingüe.")
                        ))),
                new SectionDef("REVIEWS", schema("title", "text", "items", "array"),
                        props("title", "El que diuen els nostres clients a Google", "googleMapsUrl", "", "items", List.of(
                                Map.of("name", "Susanne Hoffmann", "rating", 5, "text", "We found our dream home in Mallorca through them. Professional, multilingual service and they guided us through every step. Highly recommended!", "date", "2026-04-05"),
                                Map.of("name", "Jaume Cerdà", "rating", 5, "text", "Vaig vendre el meu apartament en 5 setmanes i per sobre del preu que esperava. Gestió impecable i comunicació constant.", "date", "2026-03-18"),
                                Map.of("name", "Ana López Ruiz", "rating", 5, "text", "Primera experiència comprant a Mallorca i no podríem estar més satisfets. Ens van assessorar fins i tot amb el finançament. Molt professionals.", "date", "2026-02-22")
                        ))),
                new SectionDef("CTA", schema("title", "text", "ctaText", "text"),
                        props("title", "La teva propietat perfecta t'espera", "subtitle", "Consulta gratuïta · Sense compromís · Experiència local", "ctaText", "Parla amb un expert", "ctaLink", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text"),
                        props("title", "Consulta gratuïta sense compromís", "subtitle", "T'atenem en menys de 24h")),
                new SectionDef("FOOTER", schema("businessName", "text"),
                        props("businessName", "Immobiliària", "tagline", "Especialistes en propietats a Mallorca des de 1999", "links", List.of(
                                Map.of("label", "Serveis", "href", "#services"),
                                Map.of("label", "L'agència", "href", "#about"),
                                Map.of("label", "Contacte", "href", "#contact")
                        )))
        ));
    }
}
