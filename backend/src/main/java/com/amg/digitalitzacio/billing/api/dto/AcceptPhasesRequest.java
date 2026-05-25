package com.amg.digitalitzacio.billing.api.dto;

import java.util.List;
import java.util.UUID;

public record AcceptPhasesRequest(List<UUID> phaseIds) {}
