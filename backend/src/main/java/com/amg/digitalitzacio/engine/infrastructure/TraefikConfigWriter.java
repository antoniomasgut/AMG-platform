package com.amg.digitalitzacio.engine.infrastructure;

import com.amg.digitalitzacio.engine.domain.Landing;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.engine.domain.LandingStatus;
import com.amg.digitalitzacio.hosting.application.HostingManifestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Genera les rutes de coolify-proxy per a les landings publicades, de forma SEGURA:
 * el backend NO escriu mai la config del proxy. Escriu manifests d'estat desitjat
 * (via HostingManifestService, al volum websites_data/_desired/) i l'agent del host
 * (hosting-reconciler-agent.py) valida el domini i genera el router concret amb cert
 * HTTP-01. Es crida en publicar/despublicar i a l'arrencada (reconciliació completa).
 *
 * Declaratiu: per a cada landing PUBLISHED escriu una ruta deploy; per a la resta, remove.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TraefikConfigWriter {

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    private final LandingRepository landingRepository;
    private final HostingManifestService manifestService;

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        regenerate(landingRepository.findAll());
    }

    public void regenerate(List<Landing> allLandings) {
        for (var l : allLandings) {
            String domain = domainOf(l);
            if (l.getStatus() == LandingStatus.PUBLISHED) {
                manifestService.requestSiteRoute(domain, "landing");
            } else {
                manifestService.removeSiteRoute(domain);
            }
        }
    }

    private String domainOf(Landing l) {
        return l.getCustomDomain() != null ? l.getCustomDomain()
                : l.getSlug() + "." + landingBaseDomain;
    }
}
