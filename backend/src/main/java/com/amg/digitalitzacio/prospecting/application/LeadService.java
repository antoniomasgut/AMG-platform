package com.amg.digitalitzacio.prospecting.application;

import java.util.UUID;

public interface LeadService {

    record LeadCreationResult(UUID leadId, boolean isNew) {}

    LeadCreationResult createLead(String name, String email, String phone, String website,
                                  String description, String source, UUID tenantId);

    LeadCreationResult createLeadFromProspect(String name, String email, String phone, String website,
                                              String description, String source, UUID tenantId,
                                              String sector, String city, String aiPitch, String prospectTier);
}
