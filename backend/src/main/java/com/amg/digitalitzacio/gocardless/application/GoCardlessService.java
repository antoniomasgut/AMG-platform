package com.amg.digitalitzacio.gocardless.application;

import com.amg.digitalitzacio.gocardless.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface GoCardlessService {
    GoCardlessConfigResponse configure(GoCardlessConfigRequest request);
    GoCardlessConfigResponse getConfig(UUID tenantId);
    InitiateMandateResponse initiateMandate(UUID tenantId, String successReturnUrl);
    GoCardlessMandateResponse completeMandate(UUID tenantId, String redirectFlowId);
    GoCardlessMandateResponse getMandate(UUID tenantId);
    Optional<GoCardlessMandateResponse> findMandate(UUID tenantId);
    void cancelMandate(UUID tenantId);
    Page<GoCardlessPaymentResponse> listPayments(UUID tenantId, int page, int size);
    GoCardlessWebhookResponse processWebhook(GoCardlessWebhookRequest request);
    void chargeMonthlyInvoices(String period);
}
