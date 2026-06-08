package com.amg.digitalitzacio.bootstrap;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        if (tenantRepository.existsByIsOwnerTrue()) {
            return;
        }
        log.info("Seeders: creant tenant propietari AMG Digitalització...");
        tenantRepository.save(Tenant.builder()
                .name("AMG Digitalització")
                .slug("amg-digitalitzacio")
                .email("info@amgdigitalitzacio.com")
                .isActive(true)
                .isFree(true)
                .isOwner(true)
                .build());
        log.info("Seeders: tenant propietari creat");
    }
}
