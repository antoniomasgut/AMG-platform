package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.api.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProspectingService {
    CampaignResponse createCampaign(CreateCampaignRequest request, UUID createdBy);
    CampaignResponse getCampaign(UUID campaignId);
    Page<CampaignResponse> listCampaigns(String sector, String location, String status, int page, int size);
    CampaignResponse updateCampaign(UUID campaignId, UpdateCampaignRequest request);
    void deleteCampaign(UUID campaignId);
    CampaignRunResponse runCampaign(UUID campaignId);
    List<ProspectResponse> listProspects(UUID campaignId, String status);
    ProspectResponse updateProspect(UUID prospectId, UpdateProspectRequest request);
    ProspectResponse enrichProspect(UUID prospectId);
    LeadExportResponse exportProspect(UUID prospectId);
    int exportAllProspects(UUID campaignId);
    Map<String, Integer> exportContactableProspects(UUID campaignId);
    CampaignResponse cloneCampaign(UUID campaignId);
    int enrichAllProspects(UUID campaignId);
    List<ProspectResponse> scoreProspects(UUID campaignId);
    int qualifyTop(UUID campaignId, int topN);
    int qualifyByMinScore(UUID campaignId, int minScore);
    Map<String, Integer> exportQualifiedProspects(UUID campaignId);
    CampaignResponse scheduleCampaign(UUID campaignId, Instant nextRun, int repeatDays);
    CampaignResponse unscheduleCampaign(UUID campaignId);
    String generateOutreach(UUID prospectId, String channel);

    // Spec 12 v2 — nous mètodes
    ProspectResponse analyzeWeb(UUID prospectId);
    int analyzeAllWeb(UUID campaignId);
    ProspectResponse generateAiReport(UUID prospectId);
    String getWidgetCode(UUID prospectId);
    Map<String, String> detectChannels(UUID prospectId);
    Map<String, Object> getCampaignDashboard(UUID campaignId);
    Map<String, Object> getGlobalDashboard();

    // Spec 12 v2.1 — millores
    String generatePitch(UUID prospectId);
    Map<String, String> generateDemo(UUID prospectId);
    Map<String, Integer> importCsv(UUID campaignId, MultipartFile file);
    List<DuplicateGroupResponse> findDuplicates();
    void trackPitchOpen(UUID prospectId);

    // Spec 12 v2.2 — enviament automàtic de pitch
    Map<String, String> sendPitch(UUID prospectId);
}
