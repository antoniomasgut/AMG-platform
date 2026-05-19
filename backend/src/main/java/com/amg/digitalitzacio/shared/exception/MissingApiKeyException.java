package com.amg.digitalitzacio.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class MissingApiKeyException extends RuntimeException {

    private final String service;
    private final String configKey;

    public MissingApiKeyException(String service, String configKey) {
        super("API key no configurada per al servei: " + service + " (clau: " + configKey + ")");
        this.service = service;
        this.configKey = configKey;
    }

    public String getService() { return service; }
    public String getConfigKey() { return configKey; }
}
