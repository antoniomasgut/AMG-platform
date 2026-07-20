package com.amg.digitalitzacio.engine.application;

import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.api.dto.LandingStatsResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EngineService {
    LandingResponse createLanding(UUID tenantId, CreateLandingRequest request);
    LandingResponse createLandingFromTemplate(UUID tenantId, CreateLandingFromTemplateRequest request);
    List<LandingSummary> listLandings(UUID tenantId, int page, int size, String search);
    LandingResponse getLanding(UUID tenantId, UUID id);
    LandingResponse updateLanding(UUID tenantId, UUID id, CreateLandingRequest request);
    void deleteLanding(UUID tenantId, UUID id);
    VersionResponse createVersion(UUID landingId, CreateVersionRequest request, UUID principalTenantId);
    VersionResponse updateVersion(UUID landingId, UUID versionId, CreateVersionRequest request, UUID principalTenantId);
    PublishResponse publish(UUID landingId, String locale);
    void unpublish(UUID landingId);
    DomainConfigResponse configureDomain(UUID landingId, DomainConfigRequest request, UUID principalId, String principalRole);
    DomainConfigResponse verifyDomain(UUID landingId);
    DomainConfigResponse updateDomainStatus(UUID landingId, UpdateDomainStatusRequest request);
    void removeDomain(UUID landingId);
    String renderLanding(String slug, String host, String locale);

    /** Variant amb utm_source per a atribució de trànsit (P2 social). */
    default String renderLanding(String slug, String host, String locale, String utmSource) {
        return renderLanding(slug, host, locale);
    }
    String renderSitemap(String slug);
    ContactResponse submitContact(String slug, ContactRequest request);
    LandingStatsResponse getLandingStats(UUID tenantId, UUID landingId);
    Map<String, Long> getGlobalLandingsSummary();
    List<VersionResponse> autoTranslate(UUID tenantId, UUID landingId, String sourceLocale, List<String> targetLocales);
    LandingDefaultsResponse getLandingDefaults(UUID tenantId);
    Map<String, String> generatePreviewToken(UUID tenantId, UUID landingId);
    String renderLandingByPreviewToken(String token, String locale);
    LandingResponse duplicateLanding(UUID tenantId, UUID landingId);
}
