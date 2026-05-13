package com.amg.digitalitzacio.vault.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunicationRequestRepository extends JpaRepository<CommunicationRequest, UUID> {
    List<CommunicationRequest> findByTenantServiceIdOrderByCreatedAtDesc(UUID tenantServiceId);
    List<CommunicationRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, CommunicationStatus status);
}
