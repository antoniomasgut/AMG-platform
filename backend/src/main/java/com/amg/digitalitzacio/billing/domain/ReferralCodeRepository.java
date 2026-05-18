package com.amg.digitalitzacio.billing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, UUID> {
    Optional<ReferralCode> findByCode(String code);
    Optional<ReferralCode> findByOwnerTenantId(UUID tenantId);
    List<ReferralCode> findByCreditAppliedFalseAndUsedByTenantIdIsNotNull();
    List<ReferralCode> findAllByOrderByCreatedAtDesc();
}
