package com.amg.digitalitzacio.shared.storage.application;

import com.amg.digitalitzacio.shared.storage.StorageFile;
import com.amg.digitalitzacio.shared.storage.StorageProviderRouter;
import com.amg.digitalitzacio.shared.storage.api.dto.ProviderConfigRequest;
import com.amg.digitalitzacio.shared.storage.api.dto.ProviderConfigResponse;
import com.amg.digitalitzacio.shared.storage.api.dto.StorageStatusResponse;
import com.amg.digitalitzacio.shared.storage.domain.StorageProviderConfig;
import com.amg.digitalitzacio.shared.storage.domain.StorageProviderConfigRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantStorageService {

    private final StorageProviderConfigRepository configRepo;
    private final StorageProviderRouter router;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<ProviderConfigResponse> listConfigs(UUID tenantId) {
        return configRepo.findByTenantId(tenantId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProviderConfigResponse getConfig(UUID tenantId, String providerKey) {
        var config = configRepo.findByTenantIdAndProviderKey(tenantId, providerKey)
            .orElseThrow(() -> new java.util.NoSuchElementException("Storage config not found: " + providerKey));
        return toResponse(config);
    }

    @Transactional
    public ProviderConfigResponse saveConfig(UUID tenantId, ProviderConfigRequest req) {
        var config = configRepo.findByTenantIdAndProviderKey(tenantId, req.providerKey())
            .orElseGet(() -> StorageProviderConfig.of(tenantId, req.providerKey(), "{}"));

        try {
            config.setConfigJson(objectMapper.writeValueAsString(req.config()));
        } catch (Exception e) {
            config.setConfigJson("{}");
        }
        config.setActive(req.active());

        if (req.active()) {
            configRepo.findByTenantIdAndActiveTrue(tenantId)
                .filter(c -> !c.getProviderKey().equals(req.providerKey()))
                .ifPresent(c -> { c.setActive(false); configRepo.save(c); });
        }

        var saved = configRepo.save(config);
        router.invalidateCache(tenantId);
        return toResponse(saved);
    }

    @Transactional
    public void deleteConfig(UUID tenantId, String providerKey) {
        configRepo.findByTenantIdAndProviderKey(tenantId, providerKey)
            .ifPresent(config -> { configRepo.delete(config); router.invalidateCache(tenantId); });
    }

    @Transactional(readOnly = true)
    public StorageStatusResponse getStatus(UUID tenantId) {
        var opt = configRepo.findByTenantIdAndActiveTrue(tenantId);
        if (opt.isEmpty()) {
            return new StorageStatusResponse("local", true, "Emmagatzematge local actiu");
        }
        var config = opt.get();
        try {
            var provider = router.buildProvider(config);
            var testContent = "storage-test-" + System.currentTimeMillis();
            var result = provider.upload(
                new ByteArrayInputStream(testContent.getBytes()),
                "test-" + System.currentTimeMillis() + ".txt", "text/plain");
            provider.delete(result.fileId());
            return new StorageStatusResponse(config.getProviderKey(), true, "Connexió correcta");
        } catch (Exception e) {
            return new StorageStatusResponse(config.getProviderKey(), false, "Error: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public String testConnection(UUID tenantId, String providerKey) {
        try {
            var config = configRepo.findByTenantIdAndProviderKey(tenantId, providerKey)
                .orElseThrow(() -> new java.util.NoSuchElementException("Config not found"));
            var provider = router.buildProvider(config);
            var testContent = "connection-test-" + System.currentTimeMillis();
            var result = provider.upload(
                new ByteArrayInputStream(testContent.getBytes()),
                "test-" + System.currentTimeMillis() + ".txt", "text/plain");
            var signedUrl = provider.getSignedUrl(result.fileId(), Duration.ofMinutes(5));
            provider.delete(result.fileId());
            return signedUrl != null ? "OK" : "OK (sense signed URL)";
        } catch (Exception e) {
            throw new RuntimeException("Test de connexió fallit: " + e.getMessage());
        }
    }

    /**
     * P44: llista les últimes imatges pujades pel tenant (filtrades per extensió).
     * Retorna fins a maxKeys fitxers; si no hi ha storage configurat, retorna buit.
     */
    @Transactional(readOnly = true)
    public List<StorageFile> listTenantImages(UUID tenantId, int maxKeys) {
        return configRepo.findByTenantIdAndActiveTrue(tenantId)
            .map(config -> {
                try {
                    var provider = router.buildProvider(config);
                    return provider.listFiles("", maxKeys).stream()
                        .filter(f -> f.fileName() != null && f.fileName().toLowerCase()
                            .matches(".*\\.(jpg|jpeg|png|gif|webp)"))
                        .collect(Collectors.toList());
                } catch (Exception e) {
                    log.warn("listTenantImages fallit per {}: {}", tenantId, e.getMessage());
                    return List.<StorageFile>of();
                }
            })
            .orElse(List.of());
    }

    private ProviderConfigResponse toResponse(StorageProviderConfig c) {
        Map<String, Object> configMap;
        try {
            configMap = objectMapper.readValue(c.getConfigJson(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            configMap = Map.of();
        }
        return new ProviderConfigResponse(c.getTenantId(), c.getProviderKey(), configMap, c.isActive(), c.getUpdatedAt());
    }
}
