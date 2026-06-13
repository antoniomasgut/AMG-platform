package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findTop20ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    List<Conversation> findTop30ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.customerIdentifier = :identifier AND c.channel IN :channels ORDER BY c.createdAt DESC LIMIT 40")
    List<Conversation> findRecentByTenantIdAndCustomerIdentifierAndChannels(
        @Param("tenantId") UUID tenantId,
        @Param("identifier") String identifier,
        @Param("channels") java.util.Collection<ConversationChannel> channels);

    long countByTenantIdAndCustomerIdentifierAndChannel(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    List<Conversation> findByTenantIdAndPendingApprovalTrueOrderByCreatedAtDesc(UUID tenantId);

    List<Conversation> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c WHERE c.tenantId = ?1 ORDER BY c.createdAt DESC")
    List<String> findDistinctCustomerIdentifiersByTenantId(UUID tenantId);

    List<Conversation> findByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtAsc(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    Optional<Conversation> findTop1ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    long countByTenantIdAndCustomerIdentifierAndChannelAndPendingApprovalTrue(
        UUID tenantId, String customerIdentifier, ConversationChannel channel);

    long countByTenantIdAndCreatedAtAfter(UUID tenantId, java.time.Instant since);

    long countByTenantIdAndPendingApprovalTrue(UUID tenantId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndChannel(UUID tenantId, ConversationChannel channel);

    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c " +
           "WHERE c.tenantId = :tenantId AND c.role = 'USER' " +
           "AND c.createdAt BETWEEN :from AND :to")
    List<String> findDistinctIdentifiersByTenantIdAndCreatedAtBetween(
        UUID tenantId, Instant from, Instant to);

    // Identifiers where last message is USER (no ASSISTANT reply since) and in the given window
    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c " +
           "WHERE c.tenantId = :tenantId AND c.role = 'USER' " +
           "AND c.createdAt BETWEEN :from AND :cutoff " +
           "AND NOT EXISTS (SELECT 1 FROM Conversation c2 " +
           "  WHERE c2.tenantId = c.tenantId AND c2.customerIdentifier = c.customerIdentifier " +
           "  AND c2.role = 'ASSISTANT' AND c2.createdAt > c.createdAt)")
    List<String> findIdentifiersWithUnrespondedUserMessage(
        @Param("tenantId") UUID tenantId,
        @Param("from") Instant from,
        @Param("cutoff") Instant cutoff);

    // Identifiers where assistant mentioned a quote and client hasn't replied since
    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c " +
           "WHERE c.tenantId = :tenantId AND c.role = 'ASSISTANT' " +
           "AND LOWER(c.content) LIKE '%pressupost%' " +
           "AND c.createdAt BETWEEN :from AND :cutoff " +
           "AND NOT EXISTS (SELECT 1 FROM Conversation c2 " +
           "  WHERE c2.tenantId = c.tenantId AND c2.customerIdentifier = c.customerIdentifier " +
           "  AND c2.role = 'USER' AND c2.createdAt > c.createdAt)")
    List<String> findIdentifiersWithUnansweredQuote(
        @Param("tenantId") UUID tenantId,
        @Param("from") Instant from,
        @Param("cutoff") Instant cutoff);

    // Identifiers whose last USER message falls in the given window (for reactivation)
    @Query("SELECT DISTINCT c.customerIdentifier FROM Conversation c " +
           "WHERE c.tenantId = :tenantId AND c.role = 'USER' " +
           "AND c.createdAt BETWEEN :windowStart AND :windowEnd " +
           "AND NOT EXISTS (SELECT 1 FROM Conversation c2 " +
           "  WHERE c2.tenantId = c.tenantId AND c2.customerIdentifier = c.customerIdentifier " +
           "  AND c2.role = 'USER' AND c2.createdAt > c.createdAt)")
    List<String> findIdentifiersWithLastUserMessageInWindow(
        @Param("tenantId") UUID tenantId,
        @Param("windowStart") Instant windowStart,
        @Param("windowEnd") Instant windowEnd);

    /** Missatge USER més recent anterior a :before — per trobar el missatge del client que va generar un pendent. */
    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.customerIdentifier = :identifier AND c.channel = :channel AND c.role = 'USER' AND c.createdAt < :before ORDER BY c.createdAt DESC LIMIT 1")
    Optional<Conversation> findTop1UserMessageBefore(
        @Param("tenantId") UUID tenantId,
        @Param("identifier") String identifier,
        @Param("channel") ConversationChannel channel,
        @Param("before") Instant before);

    /** Totes les converses d'un conjunt d'identifiers — per getThread sense N+1. */
    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.customerIdentifier IN :identifiers ORDER BY c.createdAt ASC")
    List<Conversation> findByTenantIdAndCustomerIdentifierInOrderByCreatedAtAsc(
        @Param("tenantId") UUID tenantId,
        @Param("identifiers") java.util.Collection<String> identifiers);

    /** Darrer missatge per cada (customerIdentifier, channel) del tenant — per buildSummary sense N+1. */
    @Query("SELECT c FROM Conversation c WHERE c.tenantId = :tenantId AND c.createdAt = (SELECT MAX(c2.createdAt) FROM Conversation c2 WHERE c2.tenantId = c.tenantId AND c2.customerIdentifier = c.customerIdentifier AND c2.channel = c.channel)")
    List<Conversation> findLastMessagePerIdentifierAndChannel(@Param("tenantId") UUID tenantId);

    /** Comptador de pendents per (customerIdentifier, channel) — per buildSummary sense N+1. */
    @Query("SELECT c.customerIdentifier, c.channel, COUNT(c) FROM Conversation c WHERE c.tenantId = :tenantId AND c.pendingApproval = true GROUP BY c.customerIdentifier, c.channel")
    List<Object[]> countPendingGroupedByIdentifierAndChannel(@Param("tenantId") UUID tenantId);

    /** Darrera data de conversa per (customerIdentifier, channel) — per findMostRecentlyUsed sense N+1. */
    @Query("SELECT c.customerIdentifier, c.channel, MAX(c.createdAt) FROM Conversation c WHERE c.tenantId = :tenantId AND c.customerIdentifier IN :identifiers GROUP BY c.customerIdentifier, c.channel")
    List<Object[]> findMaxCreatedAtPerIdentifier(
        @Param("tenantId") UUID tenantId,
        @Param("identifiers") java.util.Collection<String> identifiers);
}
