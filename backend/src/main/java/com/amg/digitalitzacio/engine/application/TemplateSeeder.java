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
        String[] satParts = sat.isEmpty() ? new String[]{} : sat.split("-");
        String[] sunParts = sun.isEmpty() ? new String[]{} : sun.split("-");
        List<String[]> days = List.of(
                new String[]{"Dilluns", mfParts[0], mfParts[1]},
                new String[]{"Dimarts", mfParts[0], mfParts[1]},
                new String[]{"Dimecres", mfParts[0], mfParts[1]},
                new String[]{"Dijous", mfParts[0], mfParts[1]},
                new String[]{"Divendres", mfParts[0], mfParts[1]},
                new String[]{"Dissabte", sat.isEmpty() ? "" : satParts[0], sat.isEmpty() ? "" : satParts[1]},
                new String[]{"Diumenge", sun.isEmpty() ? "" : sunParts[0], sun.isEmpty() ? "" : sunParts[1]}
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

    // --- Restaurant ---

    private void seedRestaurant() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Restaurant").slug("restaurant")
                .description("Restaurants i bars").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Títol principal", "subtitle", "Subtítol", "ctaText", "Contacta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Sobre nosaltres", "body", "<p>Text de contingut</p>")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(Map.of("name", "Servei 1", "desc", "Descripció")))),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "Galeria", "images", List.of())),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Què diuen els clients", "items", List.of(Map.of("name", "Client", "text", "Testimoni", "rating", 5)))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris", "hours", hours("13:00-16:00", "13:00-16:00", "13:00-16:00"))),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Contacta amb nosaltres", "email", "", "phone", "")),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }

    // --- Professional ---

    private void seedProfesional() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Professional").slug("profesional")
                .description("Advocats, metges, assessors").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Títol principal", "subtitle", "Subtítol", "ctaText", "Contacta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(Map.of("name", "Servei 1", "desc", "Descripció")))),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Què diuen els clients", "items", List.of(Map.of("name", "Client", "text", "Testimoni", "rating", 5)))),
                new SectionDef("CTA", schema("text", "text", "buttonText", "text", "buttonUrl", "url"),
                        props("text", "Crida a l'acció", "buttonText", "Contacta", "buttonUrl", "#contact")),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Contacta amb nosaltres", "email", "", "phone", ""))
        ));
    }

    // --- Comerç ---

    private void seedComercio() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Comerç").slug("comercio")
                .description("Botigues i comerços locals").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Títol principal", "subtitle", "Subtítol", "ctaText", "Contacta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(Map.of("name", "Servei 1", "desc", "Descripció")))),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "Galeria", "images", List.of())),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Contacta amb nosaltres", "email", "", "phone", "")),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris d'atenció", "hours", hours("09:30-20:30", "10:00-20:00", ""))),
                new SectionDef("MAP", schema("address", "text", "lat", "text", "lng", "text"),
                        props("address", "Carrer, Ciutat", "lat", 39.5696, "lng", 2.6502)),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }

    // --- Esdeveniment ---

    private void seedEvento() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Esdeveniment").slug("evento")
                .description("Celebracions i events").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Títol principal", "subtitle", "Subtítol", "ctaText", "Contacta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Sobre l'esdeveniment", "body", "<p>Text de contingut</p>")),
                new SectionDef("GALLERY", schema("title", "text", "images", "array"),
                        props("title", "Galeria", "images", List.of())),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Contacta amb nosaltres", "email", "", "phone", ""))
        ));
    }

    // --- Bàsica ---

    private void seedBasica() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Bàsica").slug("basica")
                .description("Landing mínima").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Títol principal", "subtitle", "Subtítol", "ctaText", "Contacta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Sobre nosaltres", "body", "<p>Text de contingut</p>")),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Contacta amb nosaltres", "email", "", "phone", ""))
        ));
    }

    // --- Serveis artesans (pintor, electricista, fontaner, jardiner, neteja) ---

    private void seedServeis() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Serveis").slug("serveis")
                .description("Artesans i serveis a domicili").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Professionals de confiança", "subtitle", "Servei ràpid i de qualitat a casa teva", "ctaText", "Demana pressupost", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("title", "Servei 1", "description", "Descripció del servei"),
                                Map.of("title", "Servei 2", "description", "Descripció del servei"),
                                Map.of("title", "Servei 3", "description", "Descripció del servei")
                        ))),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Per què elegirnos?", "body", "<p>Anys d'experiència al sector. Garantia en tots els treballs. Pressupost sense compromís.</p>")),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Què diuen els clients", "items", List.of(Map.of("name", "Client", "text", "Molt content amb el servei, ràpid i professional.", "rating", 5)))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "En quina zona treballeu?", "answer", "Cobrim tota la zona i voltants. Contacta per confirmar disponibilitat."),
                                Map.of("question", "Donau pressupost gratuït?", "answer", "Sí, el pressupost és gratuït i sense compromís.")
                        ))),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Demana pressupost gratuït", "email", "", "phone", "")),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }

    // --- Salut i bellesa (fisio, psicòleg, nutricionista, perruqueria, estètica) --- (NEW)

    private void seedSalut() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Salut i Bellesa").slug("salut")
                .description("Professionals de la salut i bellesa").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Cuida el teu benestar", "subtitle", "Professionals especialitzats al teu servei", "ctaText", "Reserva cita", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres tractaments", "items", List.of(
                                Map.of("title", "Tractament 1", "description", "Descripció del tractament"),
                                Map.of("title", "Tractament 2", "description", "Descripció del tractament"),
                                Map.of("title", "Tractament 3", "description", "Descripció del tractament")
                        ))),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "La nostra filosofia", "body", "<p>Apostem per un enfocament personalitzat i holístic. Cada persona és única i mereix una atenció adaptada a les seves necessitats.</p>")),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Experiències dels nostres clients", "items", List.of(Map.of("name", "Client", "text", "Una atenció excel·lent. Em sento molt millor.", "rating", 5)))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "Cal demanar cita prèvia?", "answer", "Sí, recomanem demanar cita per garantir l'atenció personalitzada."),
                                Map.of("question", "Acceptau assegurances?", "answer", "Poseu-vos en contacte amb nosaltres per consultar la compatibilitat amb la vostra assegurança.")
                        ))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris d'atenció", "hours", hours("09:00-19:00", "10:00-14:00", ""))),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Reserva la teva cita", "email", "", "phone", "")),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }

    // --- Taller mecànic ---

    private void seedTaller() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Taller Mecànic").slug("taller")
                .description("Tallers i serveis d'automoció").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "El teu taller de confiança", "subtitle", "Reparació i manteniment professional del teu vehicle", "ctaText", "Demana cita", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("title", "Revisió ITV", "description", "Preparació i revisió completa per a la ITV"),
                                Map.of("title", "Canvi d'oli i filtres", "description", "Manteniment preventiu del motor"),
                                Map.of("title", "Pneumàtics", "description", "Venda, muntatge i equilibrat de pneumàtics"),
                                Map.of("title", "Frenada i suspensió", "description", "Revisió i reparació del sistema de frenada")
                        ))),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "Per què confiar en nosaltres?", "body", "<p>Taller autoritzat amb més de X anys d'experiència. Mecànics certificats. Pressupost transparent sense sorpreses.</p>")),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients satisfets", "items", List.of(Map.of("name", "Client", "text", "Molt professionals i honests amb el pressupost. Hi tornaré.", "rating", 5)))),
                new SectionDef("OPENING_HOURS", schema("title", "text", "hours", "array"),
                        props("title", "Horaris del taller", "hours", hours("08:00-18:00", "09:00-13:00", ""))),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Demana cita o consulta", "email", "", "phone", "")),
                new SectionDef("MAP", schema("address", "text", "lat", "text", "lng", "text"),
                        props("address", "Carrer, Ciutat", "lat", 39.5696, "lng", 2.6502)),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }

    // --- Immobiliària ---

    private void seedImmobiliaria() {
        var t = templateRepository.save(LandingTemplate.builder()
                .name("Immobiliària").slug("immobiliaria")
                .description("Agències immobiliàries i gestors de propietats").build());
        addSections(t, List.of(
                new SectionDef("HERO", schema("title", "text", "subtitle", "text", "ctaText", "text", "ctaLink", "url", "bgImage", "image"),
                        props("title", "Troba la teva propietat ideal", "subtitle", "Experts en el mercat immobiliari local", "ctaText", "Consulta gratuïta", "ctaLink", "#contact", "bgImage", "")),
                new SectionDef("SERVICES", schema("title", "text", "items", "array"),
                        props("title", "Els nostres serveis", "items", List.of(
                                Map.of("title", "Compra", "description", "T'acompanyem en tot el procés de compra de la teva nova llar"),
                                Map.of("title", "Venda", "description", "Valorem i comercialitzem la teva propietat al millor preu"),
                                Map.of("title", "Lloguer", "description", "Gestió integral de lloguers per a propietaris i llogataris")
                        ))),
                new SectionDef("TEXT", schema("title", "text", "body", "richtext"),
                        props("title", "La nostra experiència al teu servei", "body", "<p>Anys d'experiència al mercat local. Coneixem cada racó de la zona. La nostra missió és trobar la propietat perfecta per a cada client.</p>")),
                new SectionDef("TESTIMONIALS", schema("title", "text", "items", "array"),
                        props("title", "Clients que confien en nosaltres", "items", List.of(Map.of("name", "Client", "text", "Ens va ajudar a trobar la casa dels nostres somnis. Molt professional.", "rating", 5)))),
                new SectionDef("FAQ", schema("title", "text", "items", "array"),
                        props("title", "Preguntes freqüents", "items", List.of(
                                Map.of("question", "Quina és la vostra comissió?", "answer", "Poseu-vos en contacte amb nosaltres per conèixer les nostres tarifes, adaptades a cada operació."),
                                Map.of("question", "Quant tarda en vendre's una propietat?", "answer", "Depèn del tipus de propietat i el mercat actual. Us informarem en la consulta inicial.")
                        ))),
                new SectionDef("CONTACT_FORM", schema("title", "text", "email", "text", "phone", "text"),
                        props("title", "Consulta gratuïta sense compromís", "email", "", "phone", "")),
                new SectionDef("FOOTER", schema("copyright", "text"),
                        props("copyright", "© 2026 Tots els drets reservats"))
        ));
    }
}
