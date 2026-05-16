package com.amg.digitalitzacio.gocardless.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.gocardless.provider", havingValue = "mock", matchIfMissing = true)
public class GoCardlessMockClient implements GoCardlessClient {

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public RedirectFlowCreated createRedirectFlow(String tenantId, String successReturnUrl, String description) {
        var flowId = "RE_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new RedirectFlowCreated(flowId, "https://pay.sandbox.gocardless.com/obauth/" + flowId);
    }

    @Override
    public RedirectFlowResult completeRedirectFlow(String redirectFlowId) {
        return new RedirectFlowResult(
                "MD_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "Mock Account Holder",
                "Caixa d'Estalvis Mock SA",
                "4242"
        );
    }

    @Override
    public String createPayment(String mandateId, BigDecimal amount, LocalDate chargeDate, String description) {
        return "PM_MOCK_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public void cancelMandate(String mandateId) {
        // mock — no-op
    }
}
