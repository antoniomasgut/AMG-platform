package com.amg.digitalitzacio.shared.storage;

import com.amg.digitalitzacio.shared.storage.domain.StorageProviderConfig;
import com.amg.digitalitzacio.shared.storage.domain.StorageProviderConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageProviderRouter {

    private final StorageProviderConfigRepository configRepo;
    private final LocalStorageProvider localStorageProvider;
    private final ObjectMapper objectMapper;
    private final Map<UUID, StorageProvider> providerCache = new ConcurrentHashMap<>();

    public StorageProvider getProvider(UUID tenantId) {
        return providerCache.computeIfAbsent(tenantId, this::resolveProvider);
    }

    private StorageProvider resolveProvider(UUID tenantId) {
        var opt = configRepo.findByTenantIdAndActiveTrue(tenantId);
        if (opt.isEmpty()) {
            log.debug("No active storage provider for tenant {}, using local", tenantId);
            return localStorageProvider;
        }
        var config = opt.get();
        return buildProvider(config);
    }

    public StorageProvider buildProvider(StorageProviderConfig config) {
        try {
            Map<String, Object> parsed = objectMapper.readValue(config.getConfigJson(),
                new TypeReference<Map<String, Object>>() {});
            parsed.put("providerName", config.getProviderKey());
            return switch (config.getProviderKey()) {
                case "minio", "s3" -> new S3StorageProvider(parsed);
                default -> localStorageProvider;
            };
        } catch (Exception e) {
            log.warn("Failed to build provider {}: {}", config.getProviderKey(), e.getMessage());
            return localStorageProvider;
        }
    }

    public void invalidateCache(UUID tenantId) {
        providerCache.remove(tenantId);
    }

    public void invalidateAll() {
        providerCache.clear();
    }
}
