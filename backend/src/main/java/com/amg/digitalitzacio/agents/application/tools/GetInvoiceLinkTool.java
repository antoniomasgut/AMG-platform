package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetInvoiceLinkTool implements AgentTool {

    private static final String ACCEPT_URL = "https://amgdl.com/accept-budget?token=";

    private final BudgetRepository budgetRepository;

    @Override
    public String name() { return "get_invoice_link"; }

    @Override
    public String description() {
        return "Genera l'enllaç segur per acceptar o revisar el pressupost/factura més recent del tenant.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(),
            "required", new String[]{}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        var page = budgetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, BudgetStatus.SENT,
            PageRequest.of(0, 1));

        if (page.isEmpty()) {
            var any = budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId,
                PageRequest.of(0, 1));
            if (any.isEmpty()) return "No hi ha cap pressupost disponible.";
            var b = any.getContent().get(0);
            return "El pressupost #" + b.getBudgetNumber() + " no s'ha enviat encara (estat: " + b.getStatus() + ").";
        }

        var b = page.getContent().get(0);
        if (b.getAcceptanceToken() == null || b.getAcceptanceToken().isBlank()) {
            return "El pressupost #" + b.getBudgetNumber() + " no té token d'acceptació generat.";
        }

        return "Pressupost #" + b.getBudgetNumber() + " | Total: " + b.getTotal() + " €\n"
            + "Enllaç d'acceptació: " + ACCEPT_URL + b.getAcceptanceToken();
    }
}
