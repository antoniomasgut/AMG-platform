package com.amg.digitalitzacio.payments.api;

import com.amg.digitalitzacio.payments.api.dto.*;
import com.amg.digitalitzacio.payments.application.PaymentService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/configure")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public StripeConfigResponse configure(@RequestBody StripeConfigRequest request) {
        return paymentService.configure(request);
    }

    @GetMapping("/configure/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public StripeConfigResponse getConfig(@PathVariable UUID tenantId) {
        return paymentService.getConfig(tenantId);
    }

    @PostMapping("/budgets/{budgetId}/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PaymentResponse createCheckoutSession(@PathVariable UUID budgetId) {
        return paymentService.createCheckoutSession(budgetId);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Page<PaymentResponse> listPayments(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.listPayments(tenantId, status, page, size);
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public PaymentResponse getPayment(@PathVariable UUID paymentId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getPayment(paymentId, principal.tenantId());
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("isAuthenticated()")
    public String getReceiptUrl(@PathVariable UUID paymentId,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return paymentService.getReceiptUrl(paymentId, principal.tenantId());
    }

    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PaymentResponse refundPayment(@PathVariable UUID paymentId) {
        return paymentService.refundPayment(paymentId);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public PaymentDashboardResponse getDashboard(@RequestParam UUID tenantId) {
        return paymentService.getDashboard(tenantId);
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public WebhookResponse webhook(@RequestBody WebhookRequest request) {
        return paymentService.processWebhook(request);
    }
}
