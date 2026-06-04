package com.amg.digitalitzacio.agents.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantTeamMemberRepository extends JpaRepository<TenantTeamMember, UUID> {
    Optional<TenantTeamMember> findByTenantIdAndTelegramUserId(UUID tenantId, Long telegramUserId);
    List<TenantTeamMember> findByTenantId(UUID tenantId);
    long countByTenantId(UUID tenantId);
}
