package com.amg.digitalitzacio.gocardless.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface GoCardlessClient {

    boolean isConnected();

    RedirectFlowCreated createRedirectFlow(String tenantId, String successReturnUrl, String description);

    RedirectFlowResult completeRedirectFlow(String tenantId, String redirectFlowId);

    String createPayment(String tenantId, String mandateId, BigDecimal amount, LocalDate chargeDate, String description);

    void cancelMandate(String tenantId, String mandateId);

    record RedirectFlowCreated(String flowId, String redirectUrl) {}

    record RedirectFlowResult(
            String mandateId,
            String accountHolderName,
            String bankName,
            String lastFourDigits
    ) {}
}
