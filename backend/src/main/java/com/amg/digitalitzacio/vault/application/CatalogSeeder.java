package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.CatalogService;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CatalogSeeder implements CommandLineRunner {

    private final CatalogServiceRepository catalogServiceRepository;

    @Override
    public void run(String... args) {
        if (catalogServiceRepository.count() > 0) {
            log.debug("Catalog already seeded ({} services found), skipping", catalogServiceRepository.count());
            return;
        }

        log.info("Seeding catalog services from serveis-cataleg v2.0...");
        seed();
        log.info("Catalog seeding complete: {} services", catalogServiceRepository.count());
    }

    // Catàleg v2.0 — 10 serveis, tots a 10€/mes
    // cost = cost intern (eng × 50€/h + despeses directes)
    // salePrice = preu de setup al client
    private void seed() {
        // 2.1 Landing
        svc("landing",                  "Landing",                      ServiceType.LANDING,      false, 100,  100,  10);

        // 2.2 Domini
        svc("domini-autogestionat",     "Domini Autogestionat",         ServiceType.OTHER,        false,  25,   25,  10);
        svc("domini-gestionat",         "Domini Gestionat",             ServiceType.OTHER,        false,  62,   62,  10);

        // 2.3 WhatsApp
        svc("whatsapp-business",        "WhatsApp Business API",        ServiceType.CREDENTIALS,  false, 100,  100,  10);

        // 2.4 Automatitzacions
        svc("automatitzacio-basica",    "Automatització Bàsica",        ServiceType.AUTOMATION,   false,  50,   50,  10);
        svc("automatitzacio-avancada",  "Automatització Avançada",      ServiceType.AUTOMATION,   false, 150,  150,  10);

        // 2.5 Intel·ligència Artificial
        svc("bot-ia-basic",             "Bot IA Bàsic",                 ServiceType.OTHER,        false, 100,  100,  10);
        svc("bot-ia-rag",               "Bot IA Avançat (RAG)",         ServiceType.OTHER,        false, 250,  250,  10);

        // 2.6 Altres
        svc("smtp-corporatiu",          "SMTP Corporatiu",              ServiceType.CREDENTIALS,  false,  50,   50,  10);
        svc("google-analytics",         "Google Analytics",             ServiceType.OTHER,        false,  50,   50,  10);
    }

    private void svc(String slug, String name, ServiceType type, boolean isAddon,
                     int costEuro, int salePriceEuro, int monthlyEuro) {
        catalogServiceRepository.save(CatalogService.builder()
                .slug(slug)
                .name(name)
                .type(type)
                .isAddon(isAddon)
                .cost(new BigDecimal(costEuro))
                .salePrice(new BigDecimal(salePriceEuro))
                .monthlyPrice(new BigDecimal(monthlyEuro))
                .build());
    }
}
