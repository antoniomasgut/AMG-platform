package com.amg.digitalitzacio.leads.application;

import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.leads.api.dto.ActivityRequest;
import com.amg.digitalitzacio.leads.api.dto.ActivityResponse;
import com.amg.digitalitzacio.leads.domain.Activity;
import com.amg.digitalitzacio.leads.domain.ActivityRepository;
import com.amg.digitalitzacio.leads.domain.Lead;
import com.amg.digitalitzacio.leads.domain.LeadRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;

    public Page<ActivityResponse> getActivities(UUID leadId, Pageable pageable, UserPrincipal principal) {
        verifyLeadAccess(leadId, principal);
        return activityRepository.findByLeadIdOrderByCreatedAtDesc(leadId, pageable)
                .map(this::toActivityResponse);
    }

    public ActivityResponse createActivity(UUID leadId, ActivityRequest request, UserPrincipal principal) {
        verifyLeadAccess(leadId, principal);

        Activity activity = new Activity();
        activity.setLeadId(leadId);
        activity.setUserId(principal.id());
        activity.setType(request.type());
        activity.setDescription(request.description());
        activity.setDueDate(request.dueDate());

        activity = activityRepository.save(activity);
        return toActivityResponse(activity);
    }

    public ActivityResponse completeActivity(UUID leadId, UUID activityId, UserPrincipal principal) {
        verifyLeadAccess(leadId, principal);

        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));

        if (!activity.getLeadId().equals(leadId)) {
            throw new ResourceNotFoundException("Activity not found for lead: " + leadId);
        }

        activity.setCompletedAt(Instant.now());
        activity = activityRepository.save(activity);
        return toActivityResponse(activity);
    }

    public ActivityResponse updateActivity(UUID leadId, UUID activityId, ActivityRequest request, UserPrincipal principal) {
        verifyLeadAccess(leadId, principal);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));
        if (!activity.getLeadId().equals(leadId)) {
            throw new ResourceNotFoundException("Activity not found for lead: " + leadId);
        }
        activity.setType(request.type());
        activity.setDescription(request.description());
        activity.setDueDate(request.dueDate());
        activity = activityRepository.save(activity);
        return toActivityResponse(activity);
    }

    public void deleteActivity(UUID leadId, UUID activityId, UserPrincipal principal) {
        verifyLeadAccess(leadId, principal);
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found: " + activityId));
        if (!activity.getLeadId().equals(leadId)) {
            throw new ResourceNotFoundException("Activity not found for lead: " + leadId);
        }
        activityRepository.delete(activity);
    }

    private void verifyLeadAccess(UUID leadId, UserPrincipal principal) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadId));
        if (!"SUPER_ADMIN".equals(principal.role())
                && !lead.getTenantId().equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Lead not found: " + leadId);
        }
    }

    private ActivityResponse toActivityResponse(Activity a) {
        return new ActivityResponse(
                a.getId(), a.getType(), a.getDescription(),
                getUserRef(a.getUserId()),
                a.getDueDate(), a.getCompletedAt(), a.getCreatedAt()
        );
    }

    private ActivityResponse.UserRef getUserRef(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(u -> new ActivityResponse.UserRef(u.getId(), u.getName()))
                .orElse(null);
    }
}
