package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactIdentifierRepository extends JpaRepository<ContactIdentifier, UUID> {

    Optional<ContactIdentifier> findByTenantIdAndChannelAndIdentifier(
        UUID tenantId, ConversationChannel channel, String identifier);

    List<ContactIdentifier> findByContactId(UUID contactId);
}
