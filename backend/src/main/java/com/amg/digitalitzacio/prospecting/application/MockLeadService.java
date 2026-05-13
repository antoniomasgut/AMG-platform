package com.amg.digitalitzacio.prospecting.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.prospecting.provider", havingValue = "mock", matchIfMissing = true)
public class MockLeadService implements LeadService {

    @Override
    public UUID createLead(String name, String email, String phone, String website, String description, String source, UUID tenantId) {
        return UUID.randomUUID();
    }
}
