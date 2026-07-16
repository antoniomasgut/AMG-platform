package com.amg.digitalitzacio.prospecting.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.prospecting.provider", havingValue = "mock", matchIfMissing = true)
@org.springframework.context.annotation.Primary
public class MockLeadService implements LeadService {

    @Override
    public LeadCreationResult createLead(String name, String email, String phone, String website, String description, String source, UUID tenantId) {
        return new LeadCreationResult(UUID.randomUUID(), true);
    }

    @Override
    public LeadCreationResult createLeadFromProspect(String name, String email, String phone, String website, String description, String source, UUID tenantId, String sector, String city, String aiPitch, String prospectTier) {
        return new LeadCreationResult(UUID.randomUUID(), true);
    }
}
