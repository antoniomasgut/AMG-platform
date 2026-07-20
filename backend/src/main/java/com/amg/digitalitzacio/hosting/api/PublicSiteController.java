package com.amg.digitalitzacio.hosting.api;

import com.amg.digitalitzacio.engine.application.EngineService;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.engine.domain.LandingStatus;
import com.amg.digitalitzacio.hosting.domain.WebSite;
import com.amg.digitalitzacio.hosting.domain.WebSiteRepository;
import com.amg.digitalitzacio.hosting.domain.WebsiteStatus;
import com.amg.digitalitzacio.hosting.domain.WebsiteType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Servei públic de sites per subdomini/domini, resolent pel Host.
 * coolify-proxy encamina *.webs.amgdl.com cap aquí amb el prefix /api/v1/sites/serve
 * (middleware addPrefix), preservant Host i sub-ruta.
 *
 * Dos casos, cap amb contenidor ni socket:
 *  - Landing del motor (cas A): subdomini = slug d'una landing publicada → render HTML.
 *  - Import estàtic (cas B): fitxers pujats pel client, servits des del volum websites_data.
 *
 * Els imports de tipus CONTAINER NO passen per aquí: tenen el seu propi contenidor
 * (creat per l'agent del host) que coolify-proxy encamina pel seu Host directament.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PublicSiteController {

    private static final String PREFIX = "/api/v1/sites/serve";

    private final EngineService engineService;
    private final LandingRepository landingRepository;
    private final WebSiteRepository webSiteRepository;

    @Value("${app.hosting.data-path:/data/websites}")
    private String dataPath;

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String baseDomain;

    @GetMapping(PREFIX + "/**")
    public ResponseEntity<?> serve(HttpServletRequest request,
                                   @RequestHeader(value = "Host", required = false) String host,
                                   @RequestParam(value = "lang", required = false) String lang,
                                   @RequestParam(value = "utm_source", required = false) String utmSource) {
        if (host == null || host.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        String hostname = host.split(":")[0].toLowerCase();
        String subPath = extractSubPath(request);
        boolean root = subPath.isEmpty() || subPath.equals("/") || subPath.equals("/index.html");

        // Cas A: landing del motor pel subdomini (només l'arrel; els assets són URLs absolutes)
        String subdomain = stripBaseDomain(hostname);
        if (root && subdomain != null) {
            Optional<com.amg.digitalitzacio.engine.domain.Landing> landing = landingRepository.findBySlug(subdomain);
            if (landing.isPresent() && landing.get().getStatus() == LandingStatus.PUBLISHED) {
                String locale = (lang != null && !lang.isBlank()) ? lang : "ca";
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(engineService.renderLanding(subdomain, hostname, locale, utmSource));
            }
        }

        // Cas B: import estàtic servit des del volum
        Optional<WebSite> site = webSiteRepository.findFirstByDomainAndTypeAndStatus(
                hostname, WebsiteType.STATIC, WebsiteStatus.ACTIVE);
        if (site.isPresent()) {
            return serveStaticFile(site.get(), subPath);
        }

        return ResponseEntity.notFound().build();
    }

    /** Extreu la sub-ruta després del prefix, decodificada i sense query. */
    private String extractSubPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.length() <= PREFIX.length()) return "";
        return uri.substring(PREFIX.length());
    }

    /** Retorna el subdomini si el host és *.baseDomain (p. ex. client.webs.amgdl.com → client). */
    private String stripBaseDomain(String hostname) {
        String suffix = "." + baseDomain.toLowerCase();
        if (hostname.endsWith(suffix)) {
            String sub = hostname.substring(0, hostname.length() - suffix.length());
            // només un nivell de subdomini (client), no client.extra
            return sub.contains(".") ? null : sub;
        }
        return null;
    }

    private ResponseEntity<?> serveStaticFile(WebSite site, String subPath) {
        Path htmlDir = Path.of(dataPath, site.getTenantId().toString(), "html").normalize();
        String rel = (subPath.isEmpty() || subPath.equals("/")) ? "index.html" : subPath.replaceFirst("^/+", "");
        Path target = htmlDir.resolve(rel).normalize();

        // Anti path-traversal: el fitxer resolt ha d'estar dins del directori html del tenant
        if (!target.startsWith(htmlDir)) {
            log.warn("[Hosting] Intent de path-traversal servint {}: {}", site.getDomain(), subPath);
            return ResponseEntity.notFound().build();
        }
        if (Files.isDirectory(target)) {
            target = target.resolve("index.html").normalize();
        }
        if (!Files.exists(target) || !Files.isReadable(target)) {
            // SPA/estàtics amb rutes: fallback a index.html
            Path index = htmlDir.resolve("index.html");
            if (Files.exists(index)) {
                target = index;
            } else {
                return ResponseEntity.notFound().build();
            }
        }

        Resource resource = new FileSystemResource(target);
        MediaType mediaType = MediaTypeFactory.getMediaType(target.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic())
                .body(resource);
    }
}
