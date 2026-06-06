package com.amg.digitalitzacio.shared.storage.api.dto;

import java.util.Map;

public record ProviderConfigRequest(
    String providerKey,
    Map<String, Object> config,
    boolean active
) {}
