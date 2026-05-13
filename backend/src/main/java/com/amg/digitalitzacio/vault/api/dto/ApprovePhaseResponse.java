package com.amg.digitalitzacio.vault.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ApprovePhaseResponse(
    UUID phaseId, String approvalStatus, String paymentStatus,
    String implementationStatus, String invoiceId, BigDecimal amount
) {}
