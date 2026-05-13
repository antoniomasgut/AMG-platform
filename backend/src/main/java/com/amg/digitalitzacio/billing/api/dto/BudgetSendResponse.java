package com.amg.digitalitzacio.billing.api.dto;

import java.time.Instant;
import java.util.UUID;

public record BudgetSendResponse(UUID id, String status, Instant sentAt, String acceptanceUrl) {}
