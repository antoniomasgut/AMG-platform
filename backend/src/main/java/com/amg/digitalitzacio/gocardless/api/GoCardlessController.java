package com.amg.digitalitzacio.gocardless.api;

import com.amg.digitalitzacio.gocardless.api.dto.*;
import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gocardless")
@RequiredArgsConstructor
@Slf4j
public class GoCardlessController {

    private final GoCardlessService goCardlessService;
    private final SystemConfigService systemConfigService;
    private final ObjectMapper objectMapper;

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
    public GoCardlessWebhookResponse webhook(
            @RequestHeader(value = "Webhook-Signature", required = false) String signature,
            @RequestBody byte[] rawBody) throws Exception {
        String secret = systemConfigService.get("GOCARDLESS_WEBHOOK_SECRET");
        if (secret != null && !secret.isBlank()) {
            validateWebhookSignature(rawBody, signature, secret);
        } else {
            log.warn("GOCARDLESS_WEBHOOK_SECRET not configured — skipping signature validation");
        }
        GoCardlessWebhookRequest request = objectMapper.readValue(rawBody, GoCardlessWebhookRequest.class);
        return goCardlessService.processWebhook(request);
    }

    private void validateWebhookSignature(byte[] body, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(body));
            if (signature == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8))) {
                log.warn("GoCardless webhook signature mismatch");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC validation error", e);
        }
    }
}
