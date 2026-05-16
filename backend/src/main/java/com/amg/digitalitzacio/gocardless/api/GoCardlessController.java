package com.amg.digitalitzacio.gocardless.api;

import com.amg.digitalitzacio.gocardless.api.dto.*;
import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gocardless")
@RequiredArgsConstructor
public class GoCardlessController {

    private final GoCardlessService goCardlessService;

    @PostMapping("/configure/{tenantId}")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public GoCardlessConfigResponse configure(@PathVariable UUID tenantId,
                                               @RequestBody GoCardlessConfigRequest request) {
        return goCardlessService.configure(new GoCardlessConfigRequest(
                tenantId,
                request.apiKeyRef(),
                request.environment(),
                request.creditorId(),
                request.webhookSecret()));
    }

    @GetMapping("/configure/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public GoCardlessConfigResponse getConfig(@PathVariable UUID tenantId) {
        return goCardlessService.getConfig(tenantId);
    }

    @PostMapping("/tenants/{tenantId}/mandate/initiate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public InitiateMandateResponse initiateMandate(
            @PathVariable UUID tenantId,
            @RequestParam(required = false) String successReturnUrl) {
        return goCardlessService.initiateMandate(tenantId, successReturnUrl);
    }

    @GetMapping("/tenants/{tenantId}/mandate/complete")
    public GoCardlessMandateResponse completeMandate(
            @PathVariable UUID tenantId,
            @RequestParam("redirect_flow_id") String redirectFlowId) {
        return goCardlessService.completeMandate(tenantId, redirectFlowId);
    }

    @GetMapping("/tenants/{tenantId}/mandate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public GoCardlessMandateResponse getMandate(@PathVariable UUID tenantId) {
        return goCardlessService.getMandate(tenantId);
    }

    @DeleteMapping("/tenants/{tenantId}/mandate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void cancelMandate(@PathVariable UUID tenantId) {
        goCardlessService.cancelMandate(tenantId);
    }

    @GetMapping("/tenants/{tenantId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Page<GoCardlessPaymentResponse> listPayments(
            @PathVariable UUID tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return goCardlessService.listPayments(tenantId, page, size);
    }

    @PostMapping("/webhook")
    public GoCardlessWebhookResponse webhook(@RequestBody GoCardlessWebhookRequest request) {
        return goCardlessService.processWebhook(request);
    }
}
