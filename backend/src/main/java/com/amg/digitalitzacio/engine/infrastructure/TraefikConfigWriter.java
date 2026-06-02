package com.amg.digitalitzacio.engine.infrastructure;

import com.amg.digitalitzacio.engine.domain.Landing;
import com.amg.digitalitzacio.engine.domain.LandingStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Escriu /traefik-dynamic/landings.yml quan es publica o despublica una landing.
 * Traefik (file provider) detecta el canvi i actualitza les rutes al moment.
 *
 * Si TRAEFIK_DYNAMIC_DIR no està configurat, el servei queda desactivat (útil en dev).
 */
@Slf4j
@Component
public class TraefikConfigWriter {

    @Value("${app.traefik.dynamic-config-dir:}")
    private String configDir;

    @Value("${app.landing.base-domain:webs.amgdl.com}")
    private String landingBaseDomain;

    public void regenerate(List<Landing> allLandings) {
        if (configDir == null || configDir.isBlank()) return;

        var published = allLandings.stream()
                .filter(l -> l.getStatus() == LandingStatus.PUBLISHED)
                .toList();

        var yaml = buildYaml(published);
        write(yaml);
    }

    private String buildYaml(List<Landing> landings) {
        if (landings.isEmpty()) {
            return "# cap landing publicada\n";
        }

        var sb = new StringBuilder();
        sb.append("http:\n");
        sb.append("  routers:\n");

        for (var l : landings) {
            var slug = l.getSlug();
            var host = l.getCustomDomain() != null ? l.getCustomDomain()
                    : slug + "." + landingBaseDomain;
            var key = "landing-" + slug.replaceAll("[^a-zA-Z0-9]", "-");

            sb.append("    ").append(key).append(":\n");
            sb.append("      rule: \"Host(`").append(host).append("`)\"\n");
            sb.append("      entrypoints:\n");
            sb.append("        - https\n");
            sb.append("      tls:\n");
            sb.append("        certResolver: letsencrypt\n");
            sb.append("      service: amg-landings-svc\n");

            sb.append("    ").append(key).append("-http:\n");
            sb.append("      rule: \"Host(`").append(host).append("`)\"\n");
            sb.append("      entrypoints:\n");
            sb.append("        - http\n");
            sb.append("      middlewares:\n");
            sb.append("        - https-redirect@file\n");
            sb.append("      service: amg-landings-svc\n");
        }

        sb.append("  services:\n");
        sb.append("    amg-landings-svc:\n");
        sb.append("      loadBalancer:\n");
        sb.append("        servers:\n");
        sb.append("          - url: \"http://amg-backend:8080\"\n");

        return sb.toString();
    }

    private void write(String yaml) {
        try {
            var path = Path.of(configDir, "landings.yml");
            Files.createDirectories(path.getParent());
            Files.writeString(path, yaml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Traefik landings.yml actualitzat: {} rutes", yaml.lines().filter(l -> l.contains("certResolver")).count());
        } catch (IOException e) {
            log.error("No s'ha pogut escriure landings.yml: {}", e.getMessage());
        }
    }
}
