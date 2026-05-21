package com.amg.digitalitzacio.bootstrap;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.security.Role;
import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceProfileRepository profileRepository;
    private final PhaseRepository phaseRepository;
    private final ServiceRepository serviceRepository;
    private final TenantProfileRepository tenantProfileRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (profileRepository.count() > 0 || serviceRepository.count() > 0) {
            log.info("Seeders: ja hi ha dades al sistema, ometent seeder");
            return;
        }

        log.info("Seeders: iniciant càrrega de dades inicials...");

        // ── Tenents (empreses) ──
        var barEspla = saveTenant("Bar Es Pla", "bar-es-pla", "bar@espla.com", "971 123 456", "C/ Major 12, Inca");
        var clinicaDental = saveTenant("Clínica Dental Mallorca", "clinica-dental-mallorca", "info@clinicadentalmallorca.com", "971 234 567", "Av. Joan Miró 45, Palma");
        var talleresMartinez = saveTenant("Talleres Martínez", "talleres-martinez", "info@talleresmartinez.com", "971 345 678", "C/ Indústria 8, Manacor");
        var hotelPosada = saveTenant("Hotel Sa Posada", "hotel-sa-posada", "reserves@saposada.com", "971 456 789", "C/ Lluna 3, Sóller");
        var fornDesPla = saveTenant("Forn des Pla", "forn-des-pla", "info@forndespla.com", "971 567 890", "Pl. Major 1, Pollença");

        // ── Usuaris CLIENT per a cada tenent ──
        saveClientUser("miquel@espla.com", "Miquel", barEspla.getId());
        saveClientUser("dra.pons@clinicadentalmallorca.com", "Dra. Pons", clinicaDental.getId());
        saveClientUser("jordi@talleresmartinez.com", "Jordi Martínez", talleresMartinez.getId());
        saveClientUser("caterina@saposada.com", "Caterina", hotelPosada.getId());
        saveClientUser("pere@forndespla.com", "Pere", fornDesPla.getId());

        // ── Perfil 1: Presència Web Bàsica ──
        var webBasicaId = createProfile(
                "Presència Web Bàsica", "web-basica",
                "Per a negocis que necessiten una pàgina web senzilla amb informació de contacte, horaris i ubicació. " +
                "Ideal per a comerços locals, bars i restaurants que volen tenir presència digital sense complicacions " +
                "tècniques. Inclou domini personalitzat i SSL."
        );

        var dissenyContingut = addPhase(webBasicaId, "Disseny i contingut",
                "Maquetació de la pàgina amb la informació bàsica del negoci: presentació, serveis, horaris i contacte.");
        addService(dissenyContingut, "Micro-landing", "micro-landing", ServiceType.LANDING, false,
                BigDecimal.valueOf(15), BigDecimal.valueOf(30), 1, "Landing page senzilla d'una pàgina");

        var publicacio = addPhase(webBasicaId, "Publicació",
                "Registre de domini, configuració DNS i certificat SSL per a navegació segura.");
        addService(publicacio, "Domini + DNS + SSL", "domini-dns-ssl", ServiceType.OTHER, false,
                BigDecimal.valueOf(30), BigDecimal.valueOf(60), 1, "Domini .com/.es, DNS i certificat SSL");

        assignProfile(barEspla.getId(), webBasicaId);
        assignProfile(fornDesPla.getId(), webBasicaId);

        // ── Perfil 2: Landing Premium ──
        var landingPremiumId = createProfile(
                "Landing Premium", "landing-premium",
                "Web premium amb disseny exclusiu, animacions de scroll i formulari de contacte avançat. " +
                "Per a negocis que volen destacar i oferir una experiència visual impactant als seus clients. " +
                "Inclou galeria d'imatges i optimització SEO bàsica."
        );

        var dissenyUi = addPhase(landingPremiumId, "Disseny UI/UX",
                "Disseny d'interfície i experiència d'usuari: estructura, paleta de colors, tipografia i wireframes.");
        addService(dissenyUi, "Landing Pro", "landing-pro", ServiceType.LANDING, false,
                BigDecimal.valueOf(40), BigDecimal.valueOf(80), 1, "Landing page premium amb disseny exclusiu");

        var desenvolupament = addPhase(landingPremiumId, "Desenvolupament",
                "Desenvolupament frontend amb animacions, formulari de contacte i galeria fotogràfica.");
        addService(desenvolupament, "Animacions scroll", "animacions-scroll", ServiceType.LANDING, false,
                BigDecimal.valueOf(25), BigDecimal.valueOf(50), 1, "Efectes d'animació amb scroll");
        addService(desenvolupament, "Formulari de contacte", "formulari-contacte", ServiceType.LANDING, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), 2, "Formulari avançat amb validacions");

        var desplegament = addPhase(landingPremiumId, "Desplegament",
                "Configuració de domini, SSL i posada en producció.");
        addService(desplegament, "Domini + DNS + SSL", "domini-dns-ssl-lp", ServiceType.OTHER, false,
                BigDecimal.valueOf(30), BigDecimal.valueOf(60), 1, "Domini .com/.es, DNS i certificat SSL");

        assignProfile(clinicaDental.getId(), landingPremiumId);
        assignProfile(hotelPosada.getId(), landingPremiumId);

        // ── Perfil 3: Automatització Intel·ligent ──
        var automatitzacioId = createProfile(
                "Automatització Intel·ligent", "automatitzacio",
                "Automatització de processos empresarials amb n8n: integració de formularis, CRM, facturació i més. " +
                "Per a negocis que volen estalviar temps, reduir errors manuals i escalar sense contratemps."
        );

        var diagnosi = addPhase(automatitzacioId, "Diagnòstic i disseny",
                "Anàlisi dels processos actuals i disseny del flux d'automatització òptim per al negoci.");
        addService(diagnosi, "Automatització bàsica", "autom-basica", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), 1, "Flux d'automatització senzill (1-2 accions)");

        var implementacio = addPhase(automatitzacioId, "Implementació",
                "Desenvolupament de les integracions i connexions amb els sistemes existents.");
        addService(implementacio, "Integració CRM", "integracio-crm", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(15), BigDecimal.valueOf(30), 1, "Connexió amb CRM (HubSpot, Salesforce, etc.)");
        addService(implementacio, "Connexions API", "connexions-api", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(15), BigDecimal.valueOf(25), 2, "Integració amb APIs de tercers");

        var posadaMarxa = addPhase(automatitzacioId, "Test i posada en marxa",
                "Proves integrals, validació de fluxos i formació bàsica per a l'equip.");
        addService(posadaMarxa, "Test i validació", "test-validacio", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), 1, "Proves i formació d'usuari");

        assignProfile(talleresMartinez.getId(), automatitzacioId);

        // ── Perfil 4: Assistant IA ──
        var assistantIaId = createProfile(
                "Assistant IA", "assistant-ia",
                "Chatbot intel·ligent amb IA per a atenció al client 24/7. Resol dubtes freqüents, " +
                "gestiona reserves i qualifica leads automàticament. Amb integració a WhatsApp Business."
        );

        var configBasica = addPhase(assistantIaId, "Configuració bàsica",
                "Configuració inicial del bot amb respostes predefinides i fluxos de conversa bàsics.");
        addService(configBasica, "Bot IA bàsic", "bot-ia-basic", ServiceType.OTHER, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40), 1, "Xatbot amb respostes automàtiques bàsiques");

        var coneixement = addPhase(assistantIaId, "Coneixement avançat",
                "Entrenament del bot amb documentació del negoci per a respostes contextuals i precises (RAG).");
        addService(coneixement, "Bot IA avançat RAG", "bot-ia-rag", ServiceType.OTHER, false,
                BigDecimal.valueOf(50), BigDecimal.valueOf(100), 1, "Xatbot amb Retrieval Augmented Generation");

        var canals = addPhase(assistantIaId, "Integració de canals",
                "Connexió del bot amb WhatsApp Business i Telegram per a comunicació directa amb clients.");
        addService(canals, "WhatsApp Business", "whatsapp-business", ServiceType.CREDENTIALS, false,
                BigDecimal.valueOf(25), BigDecimal.valueOf(50), 1, "Integració amb WhatsApp Business API");
        addService(canals, "Telegram Business", "telegram-business", ServiceType.CREDENTIALS, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40), 2, "Bot de Telegram per a comunicació automatitzada");

        assignProfile(clinicaDental.getId(), assistantIaId);
        assignProfile(hotelPosada.getId(), assistantIaId);

        // ── Perfil 5: Transformació Digital Completa ──
        var transformacioId = createProfile(
                "Transformació Digital Completa", "transformacio-digital",
                "Pac complet que inclou landing premium, automatització de processos i chatbot IA. " +
                "Per a negocis que volen digitalitzar-se per complet i oferir una experiència omnicanal " +
                "als seus clients. Inclou formació de l'equip."
        );

        var faseLanding = addPhase(transformacioId, "Landing Premium",
                "Web corporativa premium amb disseny exclusiu i domini personalitzat.");
        addService(faseLanding, "Landing Pro", "landing-pro-td", ServiceType.LANDING, false,
                BigDecimal.valueOf(40), BigDecimal.valueOf(80), 1, "Landing page premium amb disseny exclusiu");
        addService(faseLanding, "Domini + DNS + SSL", "domini-dns-ssl-td", ServiceType.OTHER, false,
                BigDecimal.valueOf(30), BigDecimal.valueOf(60), 2, "Domini .com/.es, DNS i certificat SSL");

        var faseAutom = addPhase(transformacioId, "Automatització",
                "Automatització de processos clau del negoci amb integració CRM.");
        addService(faseAutom, "Automatització bàsica", "autom-basica-td", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20), 1, "Flux d'automatització senzill");
        addService(faseAutom, "Integració CRM", "integracio-crm-td", ServiceType.AUTOMATION, false,
                BigDecimal.valueOf(15), BigDecimal.valueOf(30), 2, "Connexió amb CRM existent");

        var faseIa = addPhase(transformacioId, "IA i Comunicació",
                "Chatbot IA i missatgeria per a atenció al client automatitzada.");
        addService(faseIa, "Bot IA bàsic", "bot-ia-basic-td", ServiceType.OTHER, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40), 1, "Xatbot amb respostes automàtiques");
        addService(faseIa, "WhatsApp Business", "whatsapp-business-td", ServiceType.CREDENTIALS, false,
                BigDecimal.valueOf(25), BigDecimal.valueOf(50), 2, "Integració amb WhatsApp Business API");
        addService(faseIa, "Telegram Business", "telegram-business-td", ServiceType.CREDENTIALS, false,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40), 3, "Bot de Telegram per a comunicació automatitzada");

        assignProfile(hotelPosada.getId(), transformacioId);

        // ── Addons globals ──
        createAddon("WhatsApp Business", "whatsapp-business-addon", ServiceType.CREDENTIALS,
                BigDecimal.valueOf(25), BigDecimal.valueOf(50),
                "Canal de comunicació amb WhatsApp Business API");
        createAddon("Telegram Business", "telegram-business-addon", ServiceType.CREDENTIALS,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40),
                "Bot de Telegram per a comunicació automatitzada amb clients");
        createAddon("Blog integrat", "blog-integrat", ServiceType.LANDING,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40),
                "Blog amb articles i gestor de continguts");
        createAddon("Galeria fotogràfica", "galeria-fotos", ServiceType.LANDING,
                BigDecimal.valueOf(15), BigDecimal.valueOf(30),
                "Galeria d'imatges amb lightbox i optimització");
        createAddon("Formulari avançat", "formulari-avancat", ServiceType.LANDING,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20),
                "Formulari amb camps condicionals i notificacions");
        createAddon("Analytics + Reports", "analytics-reports", ServiceType.OTHER,
                BigDecimal.valueOf(10), BigDecimal.valueOf(20),
                "Dashboard d'analítica amb informes mensuals");

        log.info("Seeders: dades inicials carregades correctament");
    }

    // ── Helpers ──

    private Tenant saveTenant(String name, String slug, String email, String phone, String address) {
        if (tenantRepository.existsBySlug(slug)) {
            return tenantRepository.findBySlug(slug).orElseThrow();
        }
        return tenantRepository.save(Tenant.builder()
                .name(name).slug(slug).email(email).phone(phone).address(address).isActive(true).build());
    }

    private void saveClientUser(String email, String name, UUID tenantId) {
        if (userRepository.existsByEmail(email)) return;
        userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("changeme123"))
                .name(name)
                .role(Role.CLIENT)
                .tenantId(tenantId)
                .isActive(true)
                .isBlocked(false)
                .failedAttempts(0)
                .build());
    }

    private UUID createProfile(String name, String slug, String description) {
        var profile = ServiceProfile.builder()
                .name(name).slug(slug).description(description).isActive(true).build();
        return profileRepository.save(profile).getId();
    }

    private UUID addPhase(UUID profileId, String name, String description) {
        var phases = phaseRepository.findByProfileIdOrderBySortOrder(profileId);
        var maxOrder = phases.stream().mapToInt(Phase::getSortOrder).max().orElse(0);
        var phase = Phase.builder()
                .profileId(profileId).name(name).description(description).sortOrder(maxOrder + 1).build();
        return phaseRepository.save(phase).getId();
    }

    private void addService(UUID phaseId, String name, String slug, ServiceType type, boolean isAddon,
                            BigDecimal cost, BigDecimal salePrice, int sortOrder, String description) {
        if (salePrice.compareTo(cost) <= 0) {
            throw new IllegalArgumentException("El preu de venda ha de ser major que el cost per a: " + name);
        }
        serviceRepository.save(CatalogService.builder()
                .phaseId(phaseId).name(name).slug(slug).description(description)
                .type(type).isAddon(isAddon)
                .cost(cost).salePrice(salePrice).sortOrder(sortOrder).build());
    }

    private void createAddon(String name, String slug, ServiceType type,
                             BigDecimal cost, BigDecimal salePrice, String description) {
        if (serviceRepository.findAll().stream().anyMatch(s -> s.getSlug().equals(slug))) return;
        serviceRepository.save(CatalogService.builder()
                .name(name).slug(slug).description(description)
                .type(type).isAddon(true)
                .cost(cost).salePrice(salePrice).build());
    }

    private void assignProfile(UUID tenantId, UUID profileId) {
        if (tenantProfileRepository.findByTenantIdAndProfileId(tenantId, profileId).isPresent()) return;
        tenantProfileRepository.save(TenantProfile.builder()
                .tenantId(tenantId).profileId(profileId)
                .isActive(true).startedAt(java.time.Instant.now()).build());
    }
}
