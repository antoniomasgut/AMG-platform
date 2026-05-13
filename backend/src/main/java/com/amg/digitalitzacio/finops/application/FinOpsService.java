package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.finops.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface FinOpsService {
    HoldedConfigResponse configure(HoldedConfigRequest request);
    HoldedConfigResponse getConfig(UUID tenantId);
    HoldedConfigResponse syncContact(UUID tenantId);

    Page<InvoiceResponse> listInvoices(UUID tenantId, String status, int page, int size);
    InvoiceResponse getInvoice(UUID invoiceId, UUID currentTenantId);
    String getInvoicePdfUrl(UUID invoiceId, UUID currentTenantId);
    InvoiceResponse cancelInvoice(UUID invoiceId);

    InvoiceResponse createInvoiceFromBudget(UUID budgetId);

    FinOpsDashboardResponse getDashboard(UUID tenantId);
    FinOpsDashboardGlobalResponse getGlobalDashboard();

    WebhookResponse processWebhook(WebhookRequest request);
}
