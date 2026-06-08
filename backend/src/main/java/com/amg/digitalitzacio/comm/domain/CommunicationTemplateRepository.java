package com.amg.digitalitzacio.comm.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunicationTemplateRepository extends JpaRepository<CommunicationTemplate, UUID> {

    List<CommunicationTemplate> findAllByOrderBySortOrderAscActionAscChannelAsc();

    Optional<CommunicationTemplate> findBySectorAndChannelAndActionAndLanguageAndIsActiveTrue(
            String sector, String channel, String action, String language);

    boolean existsBySectorAndChannelAndActionAndLanguage(
            String sector, String channel, String action, String language);
}
