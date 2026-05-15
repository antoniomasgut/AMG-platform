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
        if (templateRepository.count() > 0) {
            log.debug("Templates already seeded (count={}), skipping", templateRepository.count());
            return;
        }

        log.info("Seeding 5 landing templates...");
        seedRestaurant();
        seedProfesional();
        seedComercio();
        seedEvento();
        seedBasica();
        log.info("Templates seeded successfully");
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
}
