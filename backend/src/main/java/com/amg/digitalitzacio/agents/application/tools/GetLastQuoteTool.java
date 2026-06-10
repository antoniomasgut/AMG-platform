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
public class GetLastQuoteTool implements AgentTool {

    private final BudgetRepository budgetRepository;

    @Override
    public String name() { return "get_last_quote"; }

    @Override
    public String description() {
        return "Obté l'últim pressupost enviat al tenant. Retorna l'import, l'estat i el número de referència.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "status", Map.of("type", "string",
                    "description", "Filtrar per estat: DRAFT, SENT, ACCEPTED, REJECTED (opcional)",
                    "enum", new String[]{"DRAFT", "SENT", "ACCEPTED", "REJECTED"})
            ),
            "required", new String[]{}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        var pageable = PageRequest.of(0, 1);

        Object statusInput = input.get("status");
        var page = statusInput != null && !statusInput.toString().isBlank()
            ? budgetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId,
                BudgetStatus.valueOf(statusInput.toString().toUpperCase()), pageable)
            : budgetRepository.findByTenantIdOrderByCreatedAtDesc(tenantId, pageable);

        if (page.isEmpty()) return "No s'ha trobat cap pressupost.";

        var b = page.getContent().get(0);
        return "Pressupost #" + b.getBudgetNumber()
            + " | Estat: " + b.getStatus()
            + " | Total: " + b.getTotal() + " €"
            + (b.getValidUntil() != null ? " | Vàlid fins: " + b.getValidUntil() : "")
            + (b.getNotes() != null && !b.getNotes().isBlank() ? "\nNotes: " + b.getNotes() : "");
    }
}
