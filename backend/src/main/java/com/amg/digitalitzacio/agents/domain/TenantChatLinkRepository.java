package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantChatLinkRepository extends JpaRepository<TenantChatLink, UUID> {
    Optional<TenantChatLink> findByTenantId(UUID tenantId);
    Optional<TenantChatLink> findByLinkCode(String linkCode);
    Optional<TenantChatLink> findByTelegramChatId(Long telegramChatId);
    Optional<TenantChatLink> findByWhatsappMetaPhoneNumberId(String phoneNumberId);
    void deleteByTenantId(UUID tenantId);
}
