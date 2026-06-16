package com.amg.digitalitzacio.payments.application;

import java.util.UUID;

public record SetupPaymentCompletedEvent(UUID budgetId, UUID tenantId) {}
