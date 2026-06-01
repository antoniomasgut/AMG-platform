package com.amg.digitalitzacio.leads.api.dto;

import java.util.List;
import java.util.UUID;

public record SendTemplateRequest(UUID templateId, List<UUID> leadIds, String channel) {}
