package com.amg.digitalitzacio.vault.api.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ApprovePhaseResponse {
    private UUID phaseId;
    private String approvalStatus;
    private String paymentStatus;
    private String implementationStatus;
    private String invoiceId;
    private BigDecimal amount;
}
