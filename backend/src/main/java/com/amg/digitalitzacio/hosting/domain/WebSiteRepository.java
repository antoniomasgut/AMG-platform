package com.amg.digitalitzacio.hosting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebSiteRepository extends JpaRepository<WebSite, UUID> {

    List<WebSite> findByTenantId(UUID tenantId);

    List<WebSite> findByStatus(WebsiteStatus status);

    boolean existsByTenantIdAndDomain(UUID tenantId, String domain);
}
