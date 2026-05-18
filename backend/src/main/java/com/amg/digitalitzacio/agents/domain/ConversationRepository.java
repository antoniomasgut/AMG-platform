package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findTop20ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
        UUID tenantId,
        String customerIdentifier,
        ConversationChannel channel
    );

    List<Conversation> findByTenantIdAndPendingApprovalTrueOrderByCreatedAtDesc(UUID tenantId);

    List<Conversation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c WHERE c.tenantId = ?1 ORDER BY c.createdAt DESC")
    List<String> findDistinctCustomerIdentifiersByTenantId(UUID tenantId);
}
