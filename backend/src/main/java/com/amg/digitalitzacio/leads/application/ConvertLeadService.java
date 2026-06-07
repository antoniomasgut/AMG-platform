package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.auth.api.dto.CreateTenantRequest;
import com.amg.digitalitzacio.auth.application.TenantService;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.finops.application.FinOpsService;
import com.amg.digitalitzacio.gocardless.application.GoCardlessService;
import com.amg.digitalitzacio.leads.api.dto.ConvertLeadRequest;
import com.amg.digitalitzacio.leads.api.dto.ConvertLeadResult;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import com.amg.digitalitzacio.payments.application.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConvertLeadService {

    private final LeadRepository leadRepo;
    private final TenantService tenantService;
    private final BudgetRepository budgetRepo;
    private final PaymentService paymentService;
    private final GoCardlessService goCardlessService;
    private final FinOpsService finOpsService;

    @Transactional
    public ConvertLeadResult convert(UUID leadId, ConvertLeadRequest req) {
        Lead lead = leadRepo.findById(leadId)
                .orElseThrow(() -> new IllegalArgumentException("Lead not found: " + leadId));

        // 1. Crear tenant
        String slug = toSlug(req.tenantName());
        String fullAddress = req.billingAddress() != null
                ? req.billingAddress() + (req.billingCity() != null ? ", " + req.billingCity() : "")
                : req.billingCity();

        var newTenant = tenantService.createTenant(new CreateTenantRequest(
                req.tenantName(), slug,
                req.billingEmail(), req.billingPhone(),
                fullAddress, req.billingCity(),
                req.billingNif(),
                req.billingPhone(),
                null,
                req.sector(), req.businessSize(),
                null, null, false
        ));
        UUID newTenantId = newTenant.id();
        log.info("Tenant creat des de lead {}: tenantId={}", leadId, newTenantId);

        // 2. Pressupost de setup (ACCEPTED) per a Stripe + Holded
        UUID budgetId = null;
        String stripeUrl = null;
        String holdedInvoiceId = null;

        if (req.setupAmount() != null && req.setupAmount().compareTo(BigDecimal.ZERO) > 0) {
            var budget = new Budget();
            budget.setTenantId(newTenantId);
            budget.setStatus(BudgetStatus.ACCEPTED);
            budget.setSubtotal(req.setupAmount());
            budget.setDiscountTotal(BigDecimal.ZERO);
            budget.setTotal(req.setupAmount());
            budget.setAcceptedAt(Instant.now());
            budget.setBudgetNumber("SETUP-" + System.currentTimeMillis());
            budget.setNotes("Setup generat automàticament a la conversió del lead " + leadId);
            if (req.sector() != null) budget.setSector(req.sector());
            if (req.businessSize() != null) budget.setBusinessSize(req.businessSize());
            budget = budgetRepo.save(budget);
            budgetId = budget.getId();

            // Stripe checkout
            try {
                var payment = paymentService.createCheckoutSession(budgetId);
                stripeUrl = payment.checkoutUrl();
                log.info("Stripe checkout creat per tenant {}: {}", newTenantId, stripeUrl);
            } catch (Exception e) {
                log.warn("No s'ha pogut crear el checkout Stripe per al tenant {}: {}", newTenantId, e.getMessage());
            }

            // Holded invoice (si Holded configurat)
            try {
                var invoice = finOpsService.createInvoiceFromBudget(budgetId);
                holdedInvoiceId = invoice.holdedInvoiceId();
                log.info("Holded invoice creat: {}", holdedInvoiceId);
            } catch (Exception e) {
                log.warn("No s'ha pogut crear la factura Holded (probablement no configurat): {}", e.getMessage());
            }
        }

        // 3. GoCardless mandate per als mensuals
        String goCardlessUrl = null;
        if (req.monthlyAmount() != null && req.monthlyAmount().compareTo(BigDecimal.ZERO) > 0) {
            try {
                String base = req.portalBaseUrl() != null ? req.portalBaseUrl() : "https://api.amgdl.com";
                String returnUrl = base + "/api/v1/gocardless/tenants/" + newTenantId + "/mandate/complete";
                var mandate = goCardlessService.initiateMandate(newTenantId, returnUrl);
                goCardlessUrl = mandate.redirectUrl();
                log.info("GoCardless mandate iniciat per tenant {}: {}", newTenantId, mandate.redirectFlowId());
            } catch (Exception e) {
                log.warn("No s'ha pogut iniciar el mandat GoCardless per al tenant {}: {}", newTenantId, e.getMessage());
            }
        }

        // 4. Marcar lead com a convertit
        lead.setConvertedTenantId(newTenantId);
        lead.setConvertedAt(Instant.now());
        lead.setStage(PipelineStage.WON);
        leadRepo.save(lead);

        return new ConvertLeadResult(newTenantId, newTenant.name(), stripeUrl, goCardlessUrl, holdedInvoiceId);
    }

    private String toSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[àáâãäå]", "a")
                .replaceAll("[èéêë]", "e")
                .replaceAll("[ìíîï]", "i")
                .replaceAll("[òóôõö]", "o")
                .replaceAll("[ùúûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        return base + "-" + System.currentTimeMillis() % 10000;
    }
}
