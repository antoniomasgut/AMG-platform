package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Profile("!test")
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AgenciaIAProfileSeeder implements CommandLineRunner {

    private final ServiceProfileRepository profileRepository;
    private final PhaseRepository phaseRepository;
    private final CatalogServiceRepository catalogServiceRepository;

    private static final String PROFILE_SLUG = "agencia-ia";

    @Override
    @Transactional
    public void run(String... args) {
        if (profileRepository.existsBySlug(PROFILE_SLUG)) {
            log.debug("Perfil '{}' ja existeix, ometent seeder", PROFILE_SLUG);
            return;
        }

        log.info("Creant perfil Agència IA...");

        // Assegurem que els serveis base existeixin
        ensureService("bot-ia-basic",       "Bot IA Bàsic",              ServiceType.OTHER,        false, 100, 100, 10);
        ensureService("bot-ia-rag",         "Bot IA Avançat (RAG)",      ServiceType.OTHER,        false, 250, 250, 10);
        ensureService("bot-telegram",       "Bot Telegram",              ServiceType.OTHER,        false,  50,  50, 10);
        ensureService("whatsapp-business",  "WhatsApp Business",         ServiceType.CREDENTIALS,  false, 100, 100, 10);
        ensureService("smtp-corporatiu",    "SMTP Corporatiu",           ServiceType.CREDENTIALS,  false,  50,  50, 10);
        ensureService("crm-pipeline",       "CRM + Pipeline",            ServiceType.OTHER,        false,  50,  50, 10);
        ensureService("reenganxament",      "Reenganxament Clients",     ServiceType.AUTOMATION,   false,  25,  25, 10);
        // Nous serveis específics d'agència IA
        ensureService("alertes-portal",     "Alertes Portal",            ServiceType.AUTOMATION,   false,  50,  50, 10);
        ensureService("leads-meta",         "Leads Meta Ads",            ServiceType.AUTOMATION,   false,  75,  75, 10);
        ensureService("leads-tiktok",       "Leads TikTok Ads",          ServiceType.AUTOMATION,   false,  75,  75, 10);

        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Agència IA")
                .slug(PROFILE_SLUG)
                .description("Per a agències digitals i negocis que volen implementar IA i captació de leads des de xarxes socials")
                .build());

        addPhase(profile.getId(), 1, "Bot IA bàsic",
                "Comunicació 24/7 amb IA + WhatsApp",
                new String[]{"bot-ia-basic", "whatsapp-business", "smtp-corporatiu"});

        addPhase(profile.getId(), 2, "Bot avançat",
                "Base de coneixement pròpia (RAG) + canal Telegram",
                new String[]{"bot-ia-rag", "bot-telegram"});

        addPhase(profile.getId(), 3, "Alertes portal",
                "Notificació automàtica per WhatsApp de problemes i events del portal",
                new String[]{"alertes-portal"});

        addPhase(profile.getId(), 4, "Leads xarxes socials",
                "Captura leads de Facebook/Instagram i TikTok Ads directament al CRM",
                new String[]{"leads-meta", "leads-tiktok"});

        addPhase(profile.getId(), 5, "CRM i fidelització",
                "Pipeline de clients + reenganxament automàtic",
                new String[]{"crm-pipeline", "reenganxament"});

        log.info("Perfil Agència IA creat amb 5 fases i 10 serveis");
    }

    private void addPhase(UUID profileId, int order, String name, String description, String[] serviceSlugs) {
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profileId)
                .name(name)
                .description(description)
                .sortOrder(order)
                .build());

        for (int i = 0; i < serviceSlugs.length; i++) {
            String slug = serviceSlugs[i];
            String uniqueSlug = order + "-" + PROFILE_SLUG + "-" + slug;
            final int sortOrder = i + 1;
            catalogServiceRepository.findBySlug(slug).ifPresent(base ->
                catalogServiceRepository.save(CatalogService.builder()
                        .slug(uniqueSlug)
                        .name(base.getName())
                        .type(base.getType())
                        .isAddon(false)
                        .cost(base.getCost())
                        .salePrice(base.getSalePrice())
                        .monthlyPrice(base.getMonthlyPrice())
                        .phaseId(phase.getId())
                        .profileId(profileId)
                        .sortOrder(sortOrder)
                        .build())
            );
        }
    }

    private void ensureService(String slug, String name, ServiceType type, boolean isAddon,
                                int cost, int salePrice, int monthly) {
        if (!catalogServiceRepository.existsBySlug(slug)) {
            catalogServiceRepository.save(CatalogService.builder()
                    .slug(slug).name(name).type(type).isAddon(isAddon)
                    .cost(new BigDecimal(cost))
                    .salePrice(new BigDecimal(salePrice))
                    .monthlyPrice(new BigDecimal(monthly))
                    .build());
        }
    }
}
