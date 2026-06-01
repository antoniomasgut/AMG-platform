package com.amg.digitalitzacio.hosting.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
