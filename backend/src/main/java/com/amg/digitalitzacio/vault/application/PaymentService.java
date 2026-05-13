package com.amg.digitalitzacio.vault.application;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {
    PaymentResult charge(UUID tenantId, UUID invoiceId, BigDecimal amount);
    PaymentResult refund(UUID tenantId, UUID invoiceId);

    record PaymentResult(boolean success, String transactionId, String errorMessage) {}
}
