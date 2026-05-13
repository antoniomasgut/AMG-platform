package com.amg.digitalitzacio.prospecting.application;

import java.util.UUID;

public interface LeadService {
    UUID createLead(String name, String email, String phone, String website, String description, String source, UUID tenantId);
}
