package com.amg.digitalitzacio.bootstrap;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerTenantSeeder {

    private final TenantRepository tenantRepository;
    private final SystemConfigService systemConfigService;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        Tenant owner;
        if (tenantRepository.existsByIsOwnerTrue()) {
            owner = tenantRepository.findByIsOwnerTrue().orElseThrow();
        } else {
            log.info("Seeders: creant tenant propietari AMG Digitalització...");
            owner = tenantRepository.save(Tenant.builder()
                    .name("AMG Digitalització")
                    .slug("amg-digitalitzacio")
                    .email("info@amgdigitalitzacio.com")
                    .isActive(true)
                    .isFree(true)
                    .isOwner(true)
                    .build());
            log.info("Seeders: tenant propietari creat");
        }

        // Sincronitza PLATFORM_TENANT_ID automàticament amb el tenant propietari
        String current = systemConfigService.get("PLATFORM_TENANT_ID");
        if (current == null || current.isBlank()) {
            systemConfigService.setInternal("PLATFORM_TENANT_ID", owner.getId().toString());
            log.info("Seeders: PLATFORM_TENANT_ID assignat automàticament → {}", owner.getId());
        }
    }
}
