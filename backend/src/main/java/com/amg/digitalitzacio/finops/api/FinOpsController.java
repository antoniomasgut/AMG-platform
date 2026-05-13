package com.amg.digitalitzacio.finops.api;

import com.amg.digitalitzacio.finops.api.dto.*;
import com.amg.digitalitzacio.finops.application.FinOpsService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/finops")
@RequiredArgsConstructor
public class FinOpsController {

    private final FinOpsService finOpsService;

    @PostMapping("/configure")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public HoldedConfigResponse configure(@RequestBody HoldedConfigRequest request) {
        return finOpsService.configure(request);
    }

    @GetMapping("/configure/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public HoldedConfigResponse getConfig(@PathVariable UUID tenantId) {
        return finOpsService.getConfig(tenantId);
    }

    @PostMapping("/configure/{tenantId}/sync")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public HoldedConfigResponse syncContact(@PathVariable UUID tenantId) {
        return finOpsService.syncContact(tenantId);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public Page<InvoiceResponse> listInvoices(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return finOpsService.listInvoices(tenantId, status, page, size);
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("isAuthenticated()")
    public InvoiceResponse getInvoice(@PathVariable UUID invoiceId,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return finOpsService.getInvoice(invoiceId, principal.tenantId());
    }

    @GetMapping("/invoices/{invoiceId}/pdf")
    @PreAuthorize("isAuthenticated()")
    public String getInvoicePdf(@PathVariable UUID invoiceId,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return finOpsService.getInvoicePdfUrl(invoiceId, principal.tenantId());
    }

    @PostMapping("/invoices/{invoiceId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public InvoiceResponse cancelInvoice(@PathVariable UUID invoiceId) {
        return finOpsService.cancelInvoice(invoiceId);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public FinOpsDashboardResponse getDashboard(@RequestParam UUID tenantId) {
        return finOpsService.getDashboard(tenantId);
    }

    @GetMapping("/dashboard/global")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public FinOpsDashboardGlobalResponse getGlobalDashboard() {
        return finOpsService.getGlobalDashboard();
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public WebhookResponse webhook(@RequestBody WebhookRequest request) {
        return finOpsService.processWebhook(request);
    }
}
