package com.amg.digitalitzacio.infraops.api.dto;

/**
 * Estat d'un contenidor reportat per l'agent del host (docker ps).
 * El backend no toca Docker: l'agent li envia aquesta informació.
 */
public record ContainerStatus(String name, String state, String status) {

    /** Sa = en execució i no marcat com a unhealthy. */
    public boolean healthy() {
        boolean running = state != null && state.equalsIgnoreCase("running");
        boolean unhealthy = status != null && status.toLowerCase().contains("unhealthy");
        return running && !unhealthy;
    }
}
