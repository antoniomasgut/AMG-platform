package com.amg.digitalitzacio.finops.application;

import com.amg.digitalitzacio.finops.api.dto.*;
import org.springframework.data.domain.Page;

import java.util.List;
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

    // SEPA
    SepaMandateResponse registerSepaMandate(UUID tenantId, SepaMandateRequest request);
    SepaMandateResponse getSepaMandate(UUID tenantId);
    void revokeSepaMandate(UUID tenantId);

    // Facturació mensual
    List<MonthlyInvoiceResponse> generateMonthlyInvoices(String period);
    MonthlyInvoiceResponse generateMonthlyInvoiceForTenant(UUID tenantId, String period);
    MonthlyInvoiceResponse getMonthlyInvoice(UUID id);
    Page<MonthlyInvoiceResponse> listMonthlyInvoices(UUID tenantId, String period, int page, int size);

    // SEPA XML
    byte[] exportSepaXml(String period);
    void markSepaCollected(String period);
}
