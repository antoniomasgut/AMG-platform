package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.CatalogService;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.Phase;
import com.amg.digitalitzacio.vault.domain.PhaseRepository;
import com.amg.digitalitzacio.vault.domain.ServiceProfile;
import com.amg.digitalitzacio.vault.domain.ServiceProfileRepository;
import com.amg.digitalitzacio.vault.domain.ServiceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class CatalogSeeder implements CommandLineRunner {

    private final CatalogServiceRepository catalogServiceRepository;
    private final ServiceProfileRepository serviceProfileRepository;
    private final PhaseRepository phaseRepository;

    @Override
    public void run(String... args) {
        if (catalogServiceRepository.count() > 0) {
            log.debug("Catalog already seeded ({} services found), skipping", catalogServiceRepository.count());
            return;
        }

        log.info("Seeding catalog services and profiles...");
        seedBaseServices();
        seedProfiles();
        log.info("Catalog seeding complete: {} services, {} profiles",
                catalogServiceRepository.count(), serviceProfileRepository.count());
    }

    // =========================================================================
    // Base services — appear in the admin catalog (phaseId=null, profileId=null)
    // =========================================================================
    private void seedBaseServices() {
        base("whatsapp-business",         "WhatsApp Business",        ServiceType.CREDENTIALS, false, 100, 100);
        base("smtp-corporatiu",           "SMTP Corporatiu",          ServiceType.CREDENTIALS, false,  50,  50);
        base("landing-pro",               "Landing Pro",              ServiceType.LANDING,     false, 100, 100);
        base("landing-extra",             "Landing Extra",            ServiceType.LANDING,     true,  100, 100);
        base("agenda-online",             "Agenda Online",            ServiceType.AUTOMATION,  false,  50,  50);
        base("calc-pressupost",           "Calculadora Pressupost",   ServiceType.OTHER,       false,  50,  35);
        base("recordatori-24h",           "Recordatori 24h",          ServiceType.AUTOMATION,  false,  50,  20);
        base("reenganxament",             "Reenganxament Clients",    ServiceType.AUTOMATION,  false,  50,  25);
        base("ressenya-google",           "Ressenya Google",          ServiceType.AUTOMATION,  false,  50,  15);
        base("crm-pipeline",              "CRM + Pipeline",           ServiceType.OTHER,       false,  50,  50);
        base("cobrament-targeta",         "Cobrament Targeta",        ServiceType.BILLING,     false,  50,  20);
        base("galeria-fotos",             "Galeria Fotos",            ServiceType.OTHER,       false,  50,  20);
        base("xat-web",                   "Xat Web",                  ServiceType.CREDENTIALS, false,  50,  15);
        base("formulari-leads",           "Formulari Leads",          ServiceType.OTHER,       false,  50,  10);
        base("facturacio",                "Facturació",               ServiceType.BILLING,     false,  50,  40);
        base("google-analytics",          "Google Analytics",         ServiceType.OTHER,       false,  50,  50);
        base("bot-ia-basic",              "Bot IA Bàsic",             ServiceType.OTHER,       false, 100, 100);
        base("bot-ia-rag",                "Bot IA Avançat (RAG)",     ServiceType.OTHER,       false, 250, 250);
        base("domini-autogestionat",      "Domini Autogestionat",     ServiceType.OTHER,       false,  25,  25);
        base("domini-gestionat",          "Domini Gestionat",         ServiceType.OTHER,       false,  62,  62);
        base("questionari-salut",         "Qüestionari Salut",        ServiceType.OTHER,       true,   50,  15);
        base("historial-medic",           "Historial Mèdic",          ServiceType.OTHER,       true,   50,  15);
        base("horari-especialitat",       "Horari Especialitat",      ServiceType.OTHER,       true,   50,  10);
        base("telegram-bot",              "Bot Telegram",             ServiceType.OTHER,       false,  50,  50);

        log.debug("Seeded {} base catalog services", catalogServiceRepository.count());
    }

    private void base(String slug, String name, ServiceType type, boolean isAddon,
                      int costEuro, int salePriceEuro) {
        catalogServiceRepository.save(CatalogService.builder()
                .slug(slug)
                .name(name)
                .type(type)
                .isAddon(isAddon)
                .cost(new BigDecimal(costEuro))
                .salePrice(new BigDecimal(salePriceEuro))
                .build());
    }

    // =========================================================================
    // Profiles with phases
    // =========================================================================
    private void seedProfiles() {
        // A: Professionals amb cita prèvia
        seedProfile("professionals-cita", "Professionals amb cita prèvia",
                "Per a metges, dentistes, fisioterapeutes, podòlegs, psicòlegs i altres professionals que treballen amb cita prèvia",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1), svc("galeria-fotos", 2)),
                        new PhaseDef("Agenda online", 3,
                                svc("agenda-online", 1), svc("recordatori-24h", 2)),
                        new PhaseDef("CRM i seguiment", 4,
                                svc("crm-pipeline", 1), svc("reenganxament", 2)),
                        new PhaseDef("Fidelització", 5,
                                svc("ressenya-google", 1)),
                });

        // B: Negocis amb pressupost
        seedProfile("negocis-pressupost", "Negocis amb pressupost",
                "Per a professionals que treballen per pressupostos: arquitectes, enginyers, dissenyadors, fusteries, manyans, lampistes, etc.",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1), svc("galeria-fotos", 2)),
                        new PhaseDef("Pressupost", 3,
                                svc("calc-pressupost", 1)),
                        new PhaseDef("CRM i seguiment", 4,
                                svc("crm-pipeline", 1), svc("reenganxament", 2)),
                        new PhaseDef("Fidelització", 5,
                                svc("ressenya-google", 1)),
                });

        // C: Serveis per subscripció
        seedProfile("serveis-subscripcio", "Serveis per subscripció",
                "Per a gimnasos, centres esportius, escoles, academies, clubs i associacions",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1)),
                        new PhaseDef("Cobraments", 3,
                                svc("cobrament-targeta", 1)),
                        new PhaseDef("CRM i retenció", 4,
                                svc("crm-pipeline", 1), svc("reenganxament", 2)),
                        new PhaseDef("Fidelització", 5,
                                svc("ressenya-google", 1)),
                });

        // D: Comerços locals
        seedProfile("comercos-locals", "Comerços locals",
                "Per a botigues, supermercats, mercats, fleques, carnisseries i qualsevol comerç de proximitat",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1), svc("galeria-fotos", 2)),
                        new PhaseDef("Captació", 3,
                                svc("formulari-leads", 1), svc("xat-web", 2)),
                        new PhaseDef("Fidelització", 4,
                                svc("ressenya-google", 1), svc("reenganxament", 2)),
                });

        // E: Assessoria / Gestoria
        seedProfile("assessoria-gestoria", "Assessoria / Gestoria",
                "Per a assessories, gestories, despatxos d'advocats, consultories i empreses de serveis professionals",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1)),
                        new PhaseDef("CRM clients", 3,
                                svc("crm-pipeline", 1)),
                        new PhaseDef("Cobraments i factures", 4,
                                svc("cobrament-targeta", 1), svc("facturacio", 2)),
                        new PhaseDef("Fidelització", 5,
                                svc("reenganxament", 1), svc("ressenya-google", 2)),
                });

        // F: Immobiliària / Agent immobiliari
        seedProfile("immobiliari", "Immobiliària / Agent immobiliari",
                "Per a agències immobiliàries, agents independents i promotores",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1), svc("galeria-fotos", 2)),
                        new PhaseDef("Captació leads", 3,
                                svc("formulari-leads", 1), svc("crm-pipeline", 2)),
                        new PhaseDef("Visites", 4,
                                svc("agenda-online", 1), svc("recordatori-24h", 2)),
                        new PhaseDef("Seguiment", 5,
                                svc("reenganxament", 1), svc("ressenya-google", 2)),
                });

        // G: Taller mecànic
        seedProfile("taller-mecanic", "Taller mecànic",
                "Per a tallers mecànics, electrodomèstics, reparació en general i serveis tècnics",
                new PhaseDef[]{
                        new PhaseDef("Configuració bàsica", 1,
                                svc("whatsapp-business", 1), svc("smtp-corporatiu", 2)),
                        new PhaseDef("Landing", 2,
                                svc("landing-pro", 1)),
                        new PhaseDef("Reserva visites", 3,
                                svc("agenda-online", 1), svc("recordatori-24h", 2)),
                        new PhaseDef("CRM i historial", 4,
                                svc("crm-pipeline", 1), svc("historial-medic", 2)),
                        new PhaseDef("Fidelització", 5,
                                svc("reenganxament", 1), svc("ressenya-google", 2)),
                });

        log.debug("Seeded {} profiles with phases", serviceProfileRepository.count());
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /** Convenience: wraps base-slug + sort-order into a service definition for a phase. */
    private static ServiceRef svc(String baseSlug, int sortOrder) {
        return new ServiceRef(baseSlug, sortOrder);
    }

    private record ServiceRef(String baseSlug, int sortOrder) {}

    private record PhaseDef(String name, int sortOrder, ServiceRef... services) {}

    private void seedProfile(String slug, String name, String description, PhaseDef[] phases) {
        var profile = serviceProfileRepository.save(ServiceProfile.builder()
                .slug(slug)
                .name(name)
                .description(description)
                .isActive(true)
                .build());

        for (var phaseDef : phases) {
            var phase = phaseRepository.save(Phase.builder()
                    .profileId(profile.getId())
                    .name(phaseDef.name)
                    .sortOrder(phaseDef.sortOrder)
                    .build());

            for (var svc : phaseDef.services) {
                var base = findBaseBySlug(svc.baseSlug);
                var phaseSlug = phaseDef.sortOrder + "-" + slug + "-" + svc.baseSlug;
                catalogServiceRepository.save(CatalogService.builder()
                        .slug(phaseSlug)
                        .name(base.getName())
                        .type(base.getType())
                        .isAddon(base.getIsAddon())
                        .cost(base.getCost())
                        .salePrice(base.getSalePrice())
                        .phaseId(phase.getId())
                        .profileId(profile.getId())
                        .sortOrder(svc.sortOrder)
                        .build());
            }
        }
    }

    private CatalogService findBaseBySlug(String slug) {
        // All base services were just saved in seedBaseServices, so they exist in the DB.
        // We need to look them up because we need their name/type/cost/price values.
        return catalogServiceRepository.findAll().stream()
                .filter(s -> s.getSlug().equals(slug) && s.getPhaseId() == null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Base service not found: " + slug));
    }
}
