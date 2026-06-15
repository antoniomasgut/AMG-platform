package com.amg.digitalitzacio.comm.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationTemplateRepository extends JpaRepository<CommunicationTemplate, UUID> {

    List<CommunicationTemplate> findAllByOrderBySortOrderAscActionAscChannelAsc();

    Optional<CommunicationTemplate> findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
            String sector, String channel, String action, String language);

    /** Override per tenant específic */
    Optional<CommunicationTemplate> findByTenantIdAndChannelAndActionAndLanguageAndIsActiveTrue(
            UUID tenantId, String channel, String action, String language);

    /** Fallback sense idioma específic (cerca en 'ca') */
    Optional<CommunicationTemplate> findByTenantIdAndChannelAndActionAndIsActiveTrue(
            UUID tenantId, String channel, String action);

    boolean existsBySectorAndChannelAndActionAndLanguage(
            String sector, String channel, String action, String language);
}
