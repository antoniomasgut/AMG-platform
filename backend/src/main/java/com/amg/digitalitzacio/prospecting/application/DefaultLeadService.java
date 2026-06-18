package com.amg.digitalitzacio.prospecting.application;

import com.amg.digitalitzacio.billing.application.PostAcceptanceService;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.leads.domain.LeadSource;
import com.amg.digitalitzacio.leads.domain.PipelineStage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLeadService implements LeadService {

    private final LeadRepository leadRepository;
    private final PostAcceptanceService postAcceptanceService;

    @Override
    @Transactional
    public LeadCreationResult createLead(String name, String email, String phone, String website,
                                         String description, String source, UUID tenantId) {
        // Cerca si ja existeix un lead actiu amb el mateix telèfon o email (evitar duplicats)
        if (phone != null && !phone.isBlank()) {
            var existing = leadRepository.findFirstByTenantIdAndPhone(tenantId, phone);
            if (existing.isPresent()) {
                log.debug("Prospect skipped — existing lead {} matches phone {}", existing.get().getId(), phone);
                return new LeadCreationResult(existing.get().getId(), false);
            }
        }
        if (email != null && !email.isBlank()) {
            var existing = leadRepository.findFirstByTenantIdAndEmail(tenantId, email);
            if (existing.isPresent()) {
                log.debug("Prospect skipped — existing lead {} matches email {}", existing.get().getId(), email);
                return new LeadCreationResult(existing.get().getId(), false);
            }
        }
        var lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setName(name);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setNotes(buildNotes(website, description));
        lead.setStage(PipelineStage.NEW);
        lead.setSource(parseSource(source));
        var saved = leadRepository.save(lead);
        log.debug("Created lead {} from prospect export", saved.getId());
        postAcceptanceService.onLeadCreated(tenantId, name, phone != null ? phone : email, source);
        return new LeadCreationResult(saved.getId(), true);
    }

    private String buildNotes(String website, String description) {
        var sb = new StringBuilder();
        if (description != null && !description.isBlank()) sb.append(description).append("\n");
        if (website != null && !website.isBlank()) sb.append("Web: ").append(website);
        return sb.isEmpty() ? null : sb.toString().trim();
    }

    private LeadSource parseSource(String source) {
        if (source == null) return LeadSource.OTHER;
        try { return LeadSource.valueOf(source); } catch (Exception e) { return LeadSource.OTHER; }
    }
}
