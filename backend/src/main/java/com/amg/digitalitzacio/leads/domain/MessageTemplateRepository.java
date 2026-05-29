package com.amg.digitalitzacio.leads.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, UUID> {

    // Llista plantilles d'un tenant ordenades per tipus i nom
    List<MessageTemplate> findByTenantIdOrderByTypeAscNameAsc(UUID tenantId);

    // Cerca una plantilla assegurant que pertany al tenant correcte
    Optional<MessageTemplate> findByIdAndTenantId(UUID id, UUID tenantId);
}
