package com.amg.digitalitzacio.hosting.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebSiteRepository extends JpaRepository<WebSite, UUID> {

    List<WebSite> findByTenantId(UUID tenantId);

    List<WebSite> findByStatus(WebsiteStatus status);
    List<WebSite> findAllByOrderByCreatedAtDesc();

    boolean existsByTenantIdAndDomain(UUID tenantId, String domain);

    // Serveix estàtics per Host (PublicSiteController): resol el site actiu d'un domini
    Optional<WebSite> findFirstByDomainAndTypeAndStatus(String domain, WebsiteType type, WebsiteStatus status);
}
