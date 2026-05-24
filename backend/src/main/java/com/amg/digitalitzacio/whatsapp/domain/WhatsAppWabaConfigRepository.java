package com.amg.digitalitzacio.whatsapp.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppWabaConfigRepository extends JpaRepository<WhatsAppWabaConfig, UUID> {
    Optional<WhatsAppWabaConfig> findByTenantId(UUID tenantId);
    Optional<WhatsAppWabaConfig> findByPhoneNumberId(String phoneNumberId);
}
