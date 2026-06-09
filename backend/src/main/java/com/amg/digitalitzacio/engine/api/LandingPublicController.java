package com.amg.digitalitzacio.engine.api;

import com.amg.digitalitzacio.engine.api.dto.ContactRequest;
import com.amg.digitalitzacio.engine.api.dto.ContactResponse;
import com.amg.digitalitzacio.engine.application.EngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Serveix landings publicades quan el navegador accedeix directament al domini:
 *   GET https://estetica-mireia.webs.amgdl.com/        → renderitza slug "estetica-mireia"
 *   GET https://estetica-mireia.webs.amgdl.com/sitemap.xml
 *
 * Funciona tant per subdominis de landingBaseDomain com per dominis personalitzats.
 */
@RestController
@RequiredArgsConstructor
public class LandingPublicController {

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    private final EngineService engineService;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String root(@RequestHeader(value = "Host", required = false) String host,
                       @RequestParam(defaultValue = "ca") String lang) {
        return engineService.renderLanding(extractSlug(host), host, lang);
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap(@RequestHeader(value = "Host", required = false) String host) {
        return engineService.renderSitemap(extractSlug(host));
    }

    @PostMapping("/contact")
    public ContactResponse contact(
            @RequestHeader(value = "Host", required = false) String host,
            @RequestBody ContactRequest req) {
        return engineService.submitContact(extractSlug(host), req);
    }

    private String extractSlug(String host) {
        if (host == null) return "";
        // Elimina port si n'hi ha (host:port)
        String h = host.split(":")[0];
        // Subdomini de landingBaseDomain: "estetica-mireia.webs.amgdl.com" → "estetica-mireia"
        String suffix = "." + landingBaseDomain;
        if (h.endsWith(suffix)) {
            return h.substring(0, h.length() - suffix.length());
        }
        // Domini personalitzat: el host sencer és el custom domain → cerca per host
        return h;
    }
}
