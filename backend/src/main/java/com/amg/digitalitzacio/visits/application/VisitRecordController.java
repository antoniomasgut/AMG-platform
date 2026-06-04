package com.amg.digitalitzacio.visits.application;

import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.visits.api.dto.VisitRecordRequest;
import com.amg.digitalitzacio.visits.api.dto.VisitRecordResponse;
import com.amg.digitalitzacio.visits.domain.VisitRecord;
import com.amg.digitalitzacio.visits.domain.VisitRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
public class VisitRecordController {

    private final VisitRecordRepository repository;

    @GetMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public List<VisitRecordResponse> listByTenant(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        checkAccess(tenantId, principal);
        return repository.findByTenantIdOrderByVisitDateDesc(tenantId)
                .stream().map(VisitRecordResponse::from).toList();
    }

    @GetMapping("/tenants/{tenantId}/contacts/{identifier}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','CLIENT')")
    public List<VisitRecordResponse> listByContact(
            @PathVariable UUID tenantId,
            @PathVariable String identifier,
            @AuthenticationPrincipal UserPrincipal principal) {
        checkAccess(tenantId, principal);
        return repository.findByTenantIdAndContactIdentifierOrderByVisitDateDesc(tenantId, identifier)
                .stream().map(VisitRecordResponse::from).toList();
    }

    @PostMapping("/tenants/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VisitRecordResponse> create(
            @PathVariable UUID tenantId,
            @RequestBody VisitRecordRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        checkAccess(tenantId, principal);

        var record = VisitRecord.builder()
                .tenantId(tenantId)
                .contactIdentifier(req.contactIdentifier())
                .contactName(req.contactName())
                .visitDate(req.visitDate() != null ? req.visitDate() : LocalDate.now())
                .treatmentType(req.treatmentType())
                .notes(req.notes())
                .nextVisitDue(req.nextVisitDue())
                .build();

        return ResponseEntity.ok(VisitRecordResponse.from(repository.save(record)));
    }

    @PutMapping("/tenants/{tenantId}/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VisitRecordResponse> update(
            @PathVariable UUID tenantId,
            @PathVariable UUID id,
            @RequestBody VisitRecordRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        checkAccess(tenantId, principal);

        var record = repository.findById(id)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + id));

        if (req.contactName()   != null) record.setContactName(req.contactName());
        if (req.visitDate()     != null) record.setVisitDate(req.visitDate());
        if (req.treatmentType() != null) record.setTreatmentType(req.treatmentType());
        if (req.notes()         != null) record.setNotes(req.notes());
        if (req.nextVisitDue()  != null) record.setNextVisitDue(req.nextVisitDue());

        return ResponseEntity.ok(VisitRecordResponse.from(repository.save(record)));
    }

    @DeleteMapping("/tenants/{tenantId}/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable UUID tenantId,
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        checkAccess(tenantId, principal);
        var record = repository.findById(id)
                .filter(r -> r.getTenantId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + id));
        repository.delete(record);
        return ResponseEntity.noContent().build();
    }

    private void checkAccess(UUID tenantId, UserPrincipal principal) {
        if ("SUPER_ADMIN".equals(principal.role())) return;
        if (!tenantId.equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Not found");
        }
    }
}
