package com.amg.digitalitzacio.metaads.application;

import com.amg.digitalitzacio.metaads.api.dto.*;
import com.amg.digitalitzacio.metaads.domain.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaAdsCampaignService {

    private final AdCampaignRepository campaignRepository;
    private final AdSetRepository adSetRepository;
    private final AdRepository adRepository;
    private final AdCreativeRepository creativeRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public CampaignResponse createCampaign(UUID tenantId, CreateCampaignRequest req) {
        var campaign = new AdCampaign();
        campaign.setTenantId(tenantId);
        campaign.setName(req.name());
        campaign.setObjective(CampaignObjective.valueOf(req.objective()));
        campaign.setDailyBudget(req.dailyBudget());
        campaign.setLifetimeBudget(req.lifetimeBudget());
        campaign.setStartTime(req.startTime());
        campaign.setStopTime(req.stopTime());
        campaign.setStatus(CampaignStatus.DRAFT);
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> listCampaigns(UUID tenantId) {
        return campaignRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID tenantId, UUID id) {
        return campaignRepository.findByIdAndTenantId(id, tenantId)
                .map(this::toResponseFull)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));
    }

    @Transactional
    public CampaignResponse updateCampaign(UUID tenantId, UUID id, CreateCampaignRequest req) {
        var campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));
        if (campaign.getStatus() == CampaignStatus.ACTIVE || campaign.getStatus() == CampaignStatus.PENDING_REVIEW) {
            throw new IllegalStateException("No es pot editar una campanya activa o en revisió");
        }
        if (req.name() != null) campaign.setName(req.name());
        if (req.objective() != null) campaign.setObjective(CampaignObjective.valueOf(req.objective()));
        if (req.dailyBudget() != null) campaign.setDailyBudget(req.dailyBudget());
        if (req.lifetimeBudget() != null) campaign.setLifetimeBudget(req.lifetimeBudget());
        if (req.startTime() != null) campaign.setStartTime(req.startTime());
        if (req.stopTime() != null) campaign.setStopTime(req.stopTime());
        return toResponse(campaignRepository.save(campaign));
    }

    @Transactional
    public void deleteCampaign(UUID tenantId, UUID id) {
        var campaign = campaignRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));
        campaign.setStatus(CampaignStatus.ARCHIVED);
        campaignRepository.save(campaign);
    }

    @Transactional
    public CampaignResponse duplicateCampaign(UUID tenantId, UUID id) {
        var src = campaignRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));

        var copy = new AdCampaign();
        copy.setTenantId(tenantId);
        copy.setName(src.getName() + " (còpia)");
        copy.setObjective(src.getObjective());
        copy.setDailyBudget(src.getDailyBudget());
        copy.setLifetimeBudget(src.getLifetimeBudget());
        copy.setStartTime(src.getStartTime());
        copy.setStopTime(src.getStopTime());
        copy.setStatus(CampaignStatus.DRAFT);
        copy = campaignRepository.save(copy);

        for (AdSet srcSet : adSetRepository.findByCampaignIdOrderByCreatedAtAsc(src.getId())) {
            var setClone = new AdSet();
            setClone.setTenantId(tenantId);
            setClone.setCampaignId(copy.getId());
            setClone.setName(srcSet.getName());
            setClone.setDailyBudget(srcSet.getDailyBudget());
            setClone.setOptimizationGoal(srcSet.getOptimizationGoal());
            setClone.setBillingEvent(srcSet.getBillingEvent());
            setClone.setAgeMin(srcSet.getAgeMin());
            setClone.setAgeMax(srcSet.getAgeMax());
            setClone.setGenders(srcSet.getGenders());
            setClone.setGeoLocationsJson(srcSet.getGeoLocationsJson());
            setClone.setInterestsJson(srcSet.getInterestsJson());
            setClone.setPublisherPlatforms(srcSet.getPublisherPlatforms());
            setClone.setStatus(CampaignStatus.DRAFT);
            setClone = adSetRepository.save(setClone);

            for (Ad srcAd : adRepository.findByAdSetIdOrderByCreatedAtAsc(srcSet.getId())) {
                var adClone = new Ad();
                adClone.setTenantId(tenantId);
                adClone.setAdSetId(setClone.getId());
                adClone.setCreativeId(srcAd.getCreativeId());
                adClone.setName(srcAd.getName());
                adClone.setStatus(CampaignStatus.DRAFT);
                adRepository.save(adClone);
            }
        }
        return toResponseFull(copy);
    }

    // ---- Ad Sets ----

    @Transactional
    public AdSetResponse createAdSet(UUID tenantId, UUID campaignId, CreateAdSetRequest req) {
        campaignRepository.findByIdAndTenantId(campaignId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Campanya no trobada"));

        var adSet = new AdSet();
        adSet.setTenantId(tenantId);
        adSet.setCampaignId(campaignId);
        adSet.setName(req.name());
        adSet.setDailyBudget(req.dailyBudget());
        adSet.setOptimizationGoal(req.optimizationGoal() != null ? req.optimizationGoal() : "LEAD_GENERATION");
        adSet.setBillingEvent(req.billingEvent() != null ? req.billingEvent() : "IMPRESSIONS");
        adSet.setBidAmount(req.bidAmount());
        adSet.setAgeMin(req.ageMin() != null ? req.ageMin() : 18);
        adSet.setAgeMax(req.ageMax() != null ? req.ageMax() : 65);
        adSet.setGenders(req.genders() != null ? req.genders() : "ALL");
        adSet.setPublisherPlatforms(req.publisherPlatforms() != null ? req.publisherPlatforms() : "facebook,instagram");
        adSet.setStartTime(req.startTime());
        adSet.setStopTime(req.stopTime());
        adSet.setStatus(CampaignStatus.DRAFT);

        try {
            if (req.geoLocations() != null)
                adSet.setGeoLocationsJson(objectMapper.writeValueAsString(req.geoLocations()));
            if (req.interests() != null)
                adSet.setInterestsJson(objectMapper.writeValueAsString(req.interests()));
        } catch (Exception e) {
            log.warn("[MetaAds] Error serialitzant targeting JSON: {}", e.getMessage());
        }

        return toAdSetResponse(adSetRepository.save(adSet));
    }

    @Transactional
    public AdSetResponse updateAdSet(UUID tenantId, UUID adSetId, CreateAdSetRequest req) {
        var adSet = adSetRepository.findByIdAndTenantId(adSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ad Set no trobat"));
        if (req.name() != null) adSet.setName(req.name());
        if (req.dailyBudget() != null) adSet.setDailyBudget(req.dailyBudget());
        if (req.ageMin() != null) adSet.setAgeMin(req.ageMin());
        if (req.ageMax() != null) adSet.setAgeMax(req.ageMax());
        if (req.genders() != null) adSet.setGenders(req.genders());
        if (req.publisherPlatforms() != null) adSet.setPublisherPlatforms(req.publisherPlatforms());
        try {
            if (req.geoLocations() != null) adSet.setGeoLocationsJson(objectMapper.writeValueAsString(req.geoLocations()));
            if (req.interests() != null) adSet.setInterestsJson(objectMapper.writeValueAsString(req.interests()));
        } catch (Exception e) {
            log.warn("[MetaAds] Error serialitzant targeting JSON: {}", e.getMessage());
        }
        return toAdSetResponse(adSetRepository.save(adSet));
    }

    @Transactional
    public void deleteAdSet(UUID tenantId, UUID adSetId) {
        var adSet = adSetRepository.findByIdAndTenantId(adSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ad Set no trobat"));
        adSet.setStatus(CampaignStatus.ARCHIVED);
        adSetRepository.save(adSet);
    }

    // ---- Ads ----

    @Transactional
    public AdResponse createAd(UUID tenantId, UUID adSetId, CreateAdRequest req) {
        adSetRepository.findByIdAndTenantId(adSetId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Ad Set no trobat"));

        AdCreative creative;
        if (req.creativeId() != null) {
            creative = creativeRepository.findByIdAndTenantId(req.creativeId(), tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Creatiu no trobat"));
        } else {
            creative = new AdCreative();
            creative.setTenantId(tenantId);
            creative.setHeadline(req.headline());
            creative.setBody(req.body());
            creative.setDescription(req.description());
            creative.setCallToAction(req.callToAction());
            creative.setLinkUrl(req.linkUrl());
            creative.setMetaImageHash(req.metaImageHash());
            creative.setImageAssetId(req.imageAssetId());
            creative.setName(req.name());
            creative = creativeRepository.save(creative);
        }

        var ad = new Ad();
        ad.setTenantId(tenantId);
        ad.setAdSetId(adSetId);
        ad.setCreativeId(creative.getId());
        ad.setName(req.name());
        ad.setStatus(CampaignStatus.DRAFT);
        return toAdResponse(adRepository.save(ad), creative);
    }

    @Transactional
    public void deleteAd(UUID tenantId, UUID adId) {
        var ad = adRepository.findByIdAndTenantId(adId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Anunci no trobat"));
        ad.setStatus(CampaignStatus.ARCHIVED);
        adRepository.save(ad);
    }

    // ---- Mappers ----

    public CampaignResponse toResponse(AdCampaign c) {
        return new CampaignResponse(c.getId(), c.getMetaCampaignId(), c.getName(),
                c.getObjective().name(), c.getStatus().name(),
                c.getDailyBudget(), c.getLifetimeBudget(),
                c.getStartTime(), c.getStopTime(), c.getMetaError(),
                List.of(), c.getCreatedAt(), c.getUpdatedAt());
    }

    public CampaignResponse toResponseFull(AdCampaign c) {
        List<AdSetResponse> adSets = adSetRepository.findByCampaignIdOrderByCreatedAtAsc(c.getId())
                .stream().map(this::toAdSetResponseFull).toList();
        return new CampaignResponse(c.getId(), c.getMetaCampaignId(), c.getName(),
                c.getObjective().name(), c.getStatus().name(),
                c.getDailyBudget(), c.getLifetimeBudget(),
                c.getStartTime(), c.getStopTime(), c.getMetaError(),
                adSets, c.getCreatedAt(), c.getUpdatedAt());
    }

    public AdSetResponse toAdSetResponse(AdSet s) {
        return new AdSetResponse(s.getId(), s.getCampaignId(), s.getMetaAdSetId(), s.getName(),
                s.getStatus().name(), s.getDailyBudget(), s.getOptimizationGoal(),
                s.getAgeMin(), s.getAgeMax(), s.getGenders(), s.getPublisherPlatforms(),
                s.getMetaError(), List.of(), s.getCreatedAt(), s.getUpdatedAt());
    }

    public AdSetResponse toAdSetResponseFull(AdSet s) {
        List<AdResponse> ads = adRepository.findByAdSetIdOrderByCreatedAtAsc(s.getId())
                .stream().map(ad -> {
                    AdCreative c = ad.getCreativeId() != null
                            ? creativeRepository.findById(ad.getCreativeId()).orElse(null) : null;
                    return toAdResponse(ad, c);
                }).toList();
        return new AdSetResponse(s.getId(), s.getCampaignId(), s.getMetaAdSetId(), s.getName(),
                s.getStatus().name(), s.getDailyBudget(), s.getOptimizationGoal(),
                s.getAgeMin(), s.getAgeMax(), s.getGenders(), s.getPublisherPlatforms(),
                s.getMetaError(), ads, s.getCreatedAt(), s.getUpdatedAt());
    }

    public AdResponse toAdResponse(Ad ad, AdCreative c) {
        return new AdResponse(ad.getId(), ad.getAdSetId(), ad.getMetaAdId(), ad.getName(),
                ad.getStatus().name(), ad.getCreativeId(),
                c != null ? c.getHeadline() : null,
                c != null ? c.getBody() : null,
                c != null ? c.getCallToAction() : null,
                c != null ? c.getLinkUrl() : null,
                c != null ? c.getMetaImageHash() : null,
                ad.getMetaError(), ad.getCreatedAt());
    }

    // Per a ús intern des de MetaAdsPublishService
    public List<AdSet> getAdSetsForCampaign(UUID campaignId) {
        return adSetRepository.findByCampaignIdOrderByCreatedAtAsc(campaignId);
    }

    public List<Ad> getAdsForAdSet(UUID adSetId) {
        return adRepository.findByAdSetIdOrderByCreatedAtAsc(adSetId);
    }

    public AdCreative getCreative(UUID creativeId) {
        return creativeRepository.findById(creativeId).orElse(null);
    }

    public void saveAdSet(AdSet adSet) { adSetRepository.save(adSet); }
    public void saveAd(Ad ad) { adRepository.save(ad); }
    public void saveCreative(AdCreative creative) { creativeRepository.save(creative); }
    public void saveCampaign(AdCampaign campaign) { campaignRepository.save(campaign); }
}
