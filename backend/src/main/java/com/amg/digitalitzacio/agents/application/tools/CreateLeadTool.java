package com.amg.digitalitzacio.agents.application.tools;

import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateLeadTool implements AgentTool {

    private final LeadRepository leadRepository;

    @Override
    public String name() { return "create_lead"; }

    @Override
    public String description() {
        return "Crea un nou lead (contacte potencial) al CRM del tenant. Usa'l quan un client nou es posa en contacte per primera vegada. Si el client ja existeix per telèfon o email, retorna les dades existents sense crear duplicat.";
    }

    @Override
    public Map<String, Object> inputSchema() {
        return Map.of(
            "type", "object",
            "properties", Map.of(
                "name",   Map.of("type", "string",  "description", "Nom complet del client"),
                "phone",  Map.of("type", "string",  "description", "Telèfon del client (amb prefix internacional si és possible)"),
                "email",  Map.of("type", "string",  "description", "Email del client (opcional)"),
                "source", Map.of("type", "string",  "description", "Canal d'origen: WHATSAPP, EMAIL, CHAT_WIDGET, MANUAL (per defecte WHATSAPP)",
                    "enum", new String[]{"WHATSAPP", "EMAIL", "CHAT_WIDGET", "MANUAL", "OTHER"}),
                "notes",  Map.of("type", "string",  "description", "Notes inicials sobre la consulta del client (opcional)")
            ),
            "required", new String[]{"name"}
        );
    }

    @Override
    public String execute(UUID tenantId, Map<String, Object> input) {
        String name   = String.valueOf(input.getOrDefault("name", "")).trim();
        String phone  = input.get("phone") instanceof String s ? s.trim() : null;
        String email  = input.get("email") instanceof String s ? s.trim() : null;
        String source = input.get("source") instanceof String s ? s : "WHATSAPP";
        String notes  = input.get("notes") instanceof String s ? s.trim() : null;

        if (name.isBlank()) return "El nom del client és obligatori.";

        // Cerca duplicat per telèfon o email
        if (phone != null && !phone.isBlank()) {
            var existing = leadRepository.findFirstByTenantIdAndPhone(tenantId, phone);
            if (existing.isPresent()) {
                var l = existing.get();
                return "Client ja existent: " + l.getName()
                    + " | Etapa: " + l.getStage()
                    + (l.getEmail() != null ? " | Email: " + l.getEmail() : "")
                    + " (no s'ha creat duplicat)";
            }
        }
        if (email != null && !email.isBlank()) {
            var existing = leadRepository.findFirstByTenantIdAndEmail(tenantId, email);
            if (existing.isPresent()) {
                var l = existing.get();
                return "Client ja existent: " + l.getName()
                    + " | Etapa: " + l.getStage()
                    + (l.getPhone() != null ? " | Tel: " + l.getPhone() : "")
                    + " (no s'ha creat duplicat)";
            }
        }

        LeadSource leadSource;
        try { leadSource = LeadSource.valueOf(source.toUpperCase()); }
        catch (IllegalArgumentException e) { leadSource = LeadSource.OTHER; }

        var lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setName(name);
        lead.setPhone(phone);
        lead.setEmail(email);
        lead.setSource(leadSource);
        lead.setStage(PipelineStage.NEW);
        lead.setNotes(notes);
        lead.setIsActive(true);
        lead.setCreatedAt(Instant.now());
        lead.setUpdatedAt(Instant.now());
        leadRepository.save(lead);

        return "Lead creat: " + name
            + (phone != null ? " | Tel: " + phone : "")
            + (email != null ? " | Email: " + email : "")
            + " | Etapa: NEW";
    }
}
