package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.leads.api.dto.*;
import com.amg.digitalitzacio.leads.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    public LeadResponse createLead(LeadRequest request, UserPrincipal principal) {
        UUID tenantId = principal.tenantId();
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID is required to create a lead");
        }

        Lead lead = new Lead();
        lead.setTenantId(tenantId);
        lead.setName(request.name());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setSource(request.source() != null ? request.source() : LeadSource.OTHER);
        lead.setStage(PipelineStage.NEW);
        lead.setAssignedTo(request.assignedTo());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setNotes(request.notes());
        lead.setTags(request.tags());

        lead = leadRepository.save(lead);
        return toLeadResponse(lead);
    }

    public Page<LeadResponse> listLeads(Pageable pageable, String stage, String source,
                                        UUID assignedTo, String search, UUID tenantIdFilter,
                                        UserPrincipal principal) {
        String role = principal.role();
        UUID tenantId = "SUPER_ADMIN".equals(role) && tenantIdFilter != null ? tenantIdFilter : principal.tenantId();

        Page<Lead> page;
        if (search != null && !search.isBlank()) {
            if (tenantId != null) {
                page = leadRepository.findByTenantIdAndNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                        tenantId, search, search, search, pageable);
            } else {
                // SUPER_ADMIN searching all tenants
                page = leadRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                        search, search, search, pageable);
            }
        } else if (stage != null && !stage.isBlank()) {
            PipelineStage ps = PipelineStage.valueOf(stage);
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndStage(tenantId, ps, pageable)
                    : leadRepository.findByStage(ps, pageable);
        } else if (source != null && !source.isBlank()) {
            LeadSource ls = LeadSource.valueOf(source);
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndSource(tenantId, ls, pageable)
                    : leadRepository.findBySource(ls, pageable);
        } else if (assignedTo != null) {
            page = tenantId != null
                    ? leadRepository.findByTenantIdAndAssignedTo(tenantId, assignedTo, pageable)
                    : leadRepository.findByAssignedTo(assignedTo, pageable);
        } else {
            page = tenantId != null
                    ? leadRepository.findByTenantId(tenantId, pageable)
                    : leadRepository.findAll(pageable);
        }
        return page.map(this::toLeadResponse);
    }

    public LeadResponse getLead(UUID id, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);
        return toLeadResponse(lead);
    }

    public LeadResponse updateLead(UUID id, LeadRequest request, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        if (request.name() != null) lead.setName(request.name());
        if (request.email() != null) lead.setEmail(request.email());
        if (request.phone() != null) lead.setPhone(request.phone());
        if (request.source() != null) lead.setSource(request.source());
        if (request.assignedTo() != null) lead.setAssignedTo(request.assignedTo());
        if (request.estimatedValue() != null) lead.setEstimatedValue(request.estimatedValue());
        if (request.notes() != null) lead.setNotes(request.notes());
        if (request.tags() != null) lead.setTags(request.tags());

        lead = leadRepository.save(lead);
        return toLeadResponse(lead);
    }

    public void deleteLead(UUID id, UserPrincipal principal) {
        String role = principal.role();
        if (!"SUPER_ADMIN".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException("Only SUPER_ADMIN or ADMIN can delete leads");
        }
        Lead lead = findLead(id);
        verifyAccess(lead, principal);
        lead.setIsActive(false);
        leadRepository.save(lead);
    }

    public LeadResponse changeStage(UUID id, StageChangeRequest request, UserPrincipal principal) {
        Lead lead = findLead(id);
        verifyAccess(lead, principal);

        PipelineStage newStage = request.stage();
        if (newStage == PipelineStage.LOST && (request.lostReason() == null || request.lostReason().isBlank())) {
            throw new IllegalArgumentException("lostReason is required when stage is LOST");
        }

        // Reopen from WON/LOST
        if (lead.getStage() == PipelineStage.WON || lead.getStage() == PipelineStage.LOST) {
            lead.setConvertedAt(null);
        }

        if (newStage == PipelineStage.WON) {
            lead.setConvertedAt(Instant.now());
        } else {
            lead.setConvertedAt(null);
        }

        if (newStage == PipelineStage.LOST) {
            lead.setLostReason(request.lostReason());
        }

        lead.setStage(newStage);
        lead = leadRepository.save(lead);

        // Auto-create activity
        String desc = switch (newStage) {
            case WON -> "Lead guanyat";
            case LOST -> "Lead perdut: " + request.lostReason();
            default -> "Lead canviat a " + newStage.name();
        };
        Activity activity = new Activity();
        activity.setLeadId(lead.getId());
        activity.setUserId(principal.id());
        activity.setType(ActivityType.NOTE);
        activity.setDescription(desc);
        activityRepository.save(activity);

        return toLeadResponse(lead);
    }

    public LeadStatsResponse getLeadStats(UserPrincipal principal) {
        UUID tenantId = principal.tenantId();
        if ("SUPER_ADMIN".equals(principal.role())) {
            tenantId = null;
        }

        long total = tenantId != null
                ? leadRepository.countByTenantId(tenantId)
                : leadRepository.count();

        Map<PipelineStage, Long> byStage = new HashMap<>();
        for (PipelineStage s : PipelineStage.values()) {
            long count = tenantId != null
                    ? leadRepository.countByTenantIdAndStage(tenantId, s)
                    : leadRepository.countByStage(s);
            byStage.put(s, count);
        }

        Map<LeadSource, Long> bySource = new HashMap<>();
        for (LeadSource s : LeadSource.values()) {
            long count = tenantId != null
                    ? leadRepository.countByTenantIdAndSource(tenantId, s)
                    : leadRepository.countBySource(s);
            bySource.put(s, count);
        }

        long won = byStage.getOrDefault(PipelineStage.WON, 0L);
        double conversionRate = total > 0 ? (double) won / total : 0.0;

        return new LeadStatsResponse(total, byStage, bySource, conversionRate);
    }

    private Lead findLead(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private void verifyAccess(Lead lead, UserPrincipal principal) {
        if (!"SUPER_ADMIN".equals(principal.role())
                && !lead.getTenantId().equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Lead not found: " + lead.getId());
        }
    }

    private LeadResponse toLeadResponse(Lead lead) {
        return new LeadResponse(
                lead.getId(), lead.getName(), lead.getEmail(), lead.getPhone(),
                lead.getSource(), lead.getStage(),
                getUserRef(lead.getAssignedTo()),
                lead.getEstimatedValue(), lead.getNotes(), lead.getTags(),
                lead.getLostReason(), lead.getConvertedAt(),
                lead.getIsActive(), lead.getCreatedAt(), lead.getUpdatedAt()
        );
    }

    private LeadResponse.UserRef getUserRef(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> new LeadResponse.UserRef(u.getId(), u.getName()))
                .orElse(null);
    }
}
