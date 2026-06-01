package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.prospecting.api.dto.*;

import java.util.List;
import java.util.UUID;

public interface ProspectingService {
    CampaignResponse createCampaign(CreateCampaignRequest request, UUID createdBy);
    CampaignResponse getCampaign(UUID campaignId);
    List<CampaignResponse> listCampaigns(String sector, String location);
    CampaignResponse updateCampaign(UUID campaignId, UpdateCampaignRequest request);
    void deleteCampaign(UUID campaignId);
    CampaignRunResponse runCampaign(UUID campaignId);
    List<ProspectResponse> listProspects(UUID campaignId, String status);
    ProspectResponse updateProspect(UUID prospectId, UpdateProspectRequest request);
    ProspectResponse enrichProspect(UUID prospectId);
    LeadExportResponse exportProspect(UUID prospectId);
    int exportAllProspects(UUID campaignId);
    int exportContactableProspects(UUID campaignId);
    CampaignResponse cloneCampaign(UUID campaignId);
    int enrichAllProspects(UUID campaignId);
    List<ProspectResponse> scoreProspects(UUID campaignId);
    int qualifyTop(UUID campaignId, int topN);
    int qualifyByMinScore(UUID campaignId, int minScore);
    int exportQualifiedProspects(UUID campaignId);
}
