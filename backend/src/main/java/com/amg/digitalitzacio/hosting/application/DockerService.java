package com.amg.digitalitzacio.hosting.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DockerService {

    private static final String NETWORK = "amg_net";
    private static final String NGINX_IMAGE = "nginx:alpine";
    private static final String MEMORY_LIMIT = "32m";

    public void createStaticContainer(String containerName, String domain, Path htmlPath) {
        // Sanititza el nom del router Traefik (sense punts)
        String routerName = domain.replace(".", "-").replace("_", "-");

        List<String> cmd = List.of(
                "docker", "run", "-d",
                "--name", containerName,
                "--network", NETWORK,
                "--memory", MEMORY_LIMIT,
                "--restart", "unless-stopped",
                "-v", htmlPath.toAbsolutePath() + ":/usr/share/nginx/html:ro",
                "--label", "traefik.enable=true",
                "--label", "traefik.http.routers." + routerName + ".rule=Host(`" + domain + "`)",
                "--label", "traefik.http.routers." + routerName + ".tls=true",
                "--label", "traefik.http.routers." + routerName + ".tls.certresolver=letsencrypt",
                "--label", "traefik.http.services." + routerName + ".loadbalancer.server.port=80",
                "--label", "amg.type=static-site",
                NGINX_IMAGE
        );

        runDockerCommand(cmd, "createStaticContainer[" + containerName + "]");
    }

    public void createNginxProxyContainer(String proxyName, String domain, Path confDir,
                                           String upstreamContainer, int upstreamPort,
                                           String widgetScriptUrl) throws IOException {
        String routerName = domain.replace(".", "-").replace("_", "-");
        String nginxConf = buildNginxProxyConf(upstreamContainer, upstreamPort, widgetScriptUrl);
        Path confFile = confDir.resolve("nginx-proxy.conf");
        Files.createDirectories(confDir);
        Files.writeString(confFile, nginxConf, StandardCharsets.UTF_8);

        List<String> cmd = List.of(
                "docker", "run", "-d",
                "--name", proxyName,
                "--network", NETWORK,
                "--memory", MEMORY_LIMIT,
                "--restart", "unless-stopped",
                "-v", confFile.toAbsolutePath() + ":/etc/nginx/conf.d/default.conf:ro",
                "--label", "traefik.enable=true",
                "--label", "traefik.http.routers." + routerName + ".rule=Host(`" + domain + "`)",
                "--label", "traefik.http.routers." + routerName + ".tls=true",
                "--label", "traefik.http.routers." + routerName + ".tls.certresolver=letsencrypt",
                "--label", "traefik.http.services." + routerName + ".loadbalancer.server.port=80",
                "--label", "amg.type=container-site-proxy",
                NGINX_IMAGE
        );

        runDockerCommand(cmd, "createNginxProxyContainer[" + proxyName + "]");
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

    public void stopAndRemoveContainer(String containerName) {
        runDockerCommand(List.of("docker", "stop", containerName), "stop[" + containerName + "]");
        runDockerCommand(List.of("docker", "rm", containerName), "rm[" + containerName + "]");
    }

    public boolean containerExists(String containerName) {
        try {
            Process p = new ProcessBuilder("docker", "inspect", "--format", "{{.State.Running}}", containerName)
                    .redirectErrorStream(true)
                    .start();
            p.waitFor(10, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void runDockerCommand(List<String> cmd, String label) {
        try {
            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished || p.exitValue() != 0) {
                String output = new String(p.getInputStream().readAllBytes());
                log.error("[Docker] {} fallat (exit={}): {}", label, p.exitValue(), output);
                throw new RuntimeException("Docker command failed [" + label + "]: " + output);
            }
            log.info("[Docker] {} executat correctament", label);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Docker command error [" + label + "]: " + e.getMessage(), e);
        }
    }
}
