package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.NexeServiceConfig;
import com.amg.digitalitzacio.agents.domain.NexeServiceConfigRepository;
import com.amg.digitalitzacio.shared.events.AgendaConfigSavedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NexeServiceConfigService {

    private final NexeServiceConfigRepository repo;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<NexeServiceConfig> getAll(UUID tenantId) {
        return repo.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getAllAsMap(UUID tenantId) {
        return repo.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(NexeServiceConfig::getServiceKey, NexeServiceConfig::getConfigJson));
    }

    @Transactional(readOnly = true)
    public Optional<NexeServiceConfig> get(UUID tenantId, String serviceKey) {
        return repo.findByTenantIdAndServiceKey(tenantId, serviceKey);
    }

    @Transactional
    public NexeServiceConfig save(UUID tenantId, String serviceKey, String configJson) {
        var existing = repo.findByTenantIdAndServiceKey(tenantId, serviceKey);
        boolean isNew = existing.isEmpty();
        NexeServiceConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setConfigJson(configJson);
        } else {
            config = NexeServiceConfig.of(tenantId, serviceKey, configJson);
        }
        var saved = repo.save(config);
        if (isNew && "AGENDA".equals(serviceKey)) {
            eventPublisher.publishEvent(new AgendaConfigSavedEvent(tenantId));
        }
        return saved;
    }
}
