package com.amg.digitalitzacio.leads.api.dto;

import java.util.List;
import java.util.UUID;

public record OutreachRequest(
        List<UUID> leadIds,
        String subject,
        String body,
        String demoUrl,
        String language
) {}
