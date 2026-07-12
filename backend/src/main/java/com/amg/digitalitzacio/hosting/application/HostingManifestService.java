package com.amg.digitalitzacio.hosting.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Escriu manifestos d'"estat desitjat" per als sites que necessiten un contenidor
 * (imports de tipus CONTAINER). El backend NO toca mai Docker: escriu un fitxer JSON
 * a {dataPath}/_desired/ i l'agent del host (hosting-reconciler-agent.sh, cron) el
 * reconcilia (docker run/rm) a la xarxa `coolify`, amb labels perquè coolify-proxy
 * l'encamini. Així cap servei que processa input no confiable té accés al socket.
 *
 * Els sites STATIC i les landings del motor NO passen per aquí: els serveix
 * directament el backend (PublicSiteController), sense contenidor.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HostingManifestService {

    private static final String MEMORY_LIMIT = "32m";

    private final ObjectMapper objectMapper;

    @Value("${app.hosting.data-path:/data/websites}")
    private String dataPath;

    /** Directori de manifestos, relatiu al volum websites_data (compartit host↔backend). */
    private Path desiredDir() {
        return Path.of(dataPath, "_desired");
    }

    /**
     * Demana el desplegament d'un proxy nginx cap a un contenidor upstream del client.
     * Escriu la conf nginx al volum i el manifest que l'agent reconciliarà.
     *
     * @param confSubPath ruta de la conf nginx RELATIVA al volum (p. ex. "{tenant}/proxy/nginx-proxy.conf")
     */
    public void requestContainerProxy(String proxyName, String domain, String upstreamContainer,
                                      int upstreamPort, String widgetScriptUrl,
                                      String confSubPath, String confAbsolutePath) {
        try {
            Path confFile = Path.of(confAbsolutePath);
            Files.createDirectories(confFile.getParent());
            Files.writeString(confFile, buildNginxProxyConf(upstreamContainer, upstreamPort, widgetScriptUrl),
                    StandardCharsets.UTF_8);

            Map<String, Object> manifest = new LinkedHashMap<>();
            manifest.put("action", "deploy");
            manifest.put("kind", "container-proxy");
            manifest.put("containerName", proxyName);
            manifest.put("domain", domain);
            manifest.put("memory", MEMORY_LIMIT);
            manifest.put("confSubPath", confSubPath);

            writeManifest(proxyName, manifest);
        } catch (IOException e) {
            throw new RuntimeException("Error escrivint manifest de hosting per " + proxyName + ": " + e.getMessage(), e);
        }
    }

    /** Demana l'eliminació d'un contenidor gestionat. */
    public void requestRemoval(String containerName) {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("action", "remove");
        manifest.put("containerName", containerName);
        try {
            writeManifest(containerName, manifest);
        } catch (IOException e) {
            log.warn("[Hosting] No s'ha pogut escriure el manifest d'eliminació de {}: {}", containerName, e.getMessage());
        }
    }

    private void writeManifest(String containerName, Map<String, Object> manifest) throws IOException {
        Files.createDirectories(desiredDir());
        // Nom de fitxer sanititzat (l'agent només itera *.json d'aquest dir)
        String safe = containerName.replaceAll("[^a-zA-Z0-9._-]", "-");
        Path file = desiredDir().resolve(safe + ".json");
        Files.writeString(file, objectMapper.writeValueAsString(manifest), StandardCharsets.UTF_8);
        log.info("[Hosting] Manifest {} escrit ({})", file.getFileName(), manifest.get("action"));
    }

    private String buildNginxProxyConf(String upstream, int port, String widgetScriptUrl) {
        return """
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://%s:%d;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";

        # Desactivar compressió upstream perquè sub_filter funcioni
        proxy_set_header Accept-Encoding "";

        sub_filter '</body>' '<script src="%s" defer></script></body>';
        sub_filter_once on;
    }
}
""".formatted(upstream, port, widgetScriptUrl);
    }
}
