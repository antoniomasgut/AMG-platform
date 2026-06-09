package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.leads.domain.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SearchCustomerTool implements AgentTool {

    private final LeadRepository leadRepository;

    @Override
    public String name() { return "search_customer"; }

    @Override
    public String description() {
        return "Cerca un client al CRM per nom, email o telèfon. Retorna les dades bàsiques dels resultats.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "query", Map.of("type", "string",
                    "description", "Nom, email o telèfon del client a cercar")
            ),
            "required", new String[]{"query"}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        String query = String.valueOf(input.getOrDefault("query", "")).trim();
        if (query.isBlank()) return "Cal proporcionar un terme de cerca.";

        var page = leadRepository
            .findByTenantIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                tenantId, query, query, query, PageRequest.of(0, 5));

        if (page.isEmpty()) return "No s'ha trobat cap client amb '" + query + "'.";

        return page.getContent().stream().map(l ->
            "- " + l.getName()
            + (l.getEmail() != null ? " | " + l.getEmail() : "")
            + (l.getPhone() != null ? " | " + l.getPhone() : "")
            + " | Etapa: " + (l.getStage() != null ? l.getStage().name() : "—")
        ).collect(Collectors.joining("\n", "Clients trobats:\n", ""));
    }
}
