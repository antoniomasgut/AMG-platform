package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.leads.application.MessageTemplateService;
import com.amg.digitalitzacio.leads.application.MessageTemplateService.TemplateRequest;
import com.amg.digitalitzacio.leads.domain.MessageTemplate;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// DTO de resposta — top-level record per evitar problemes de serialització amb Jackson
record TemplateResponse(
        UUID id,
        String name,
        String type,
        String subject,
        String body,
        Instant createdAt,
        Instant updatedAt
) {}

@RestController
@RequestMapping("/api/v1/leads/templates")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService templateService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TemplateResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TemplateResponse> result = templateService.list(principal)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TemplateResponse> create(
            @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        MessageTemplate created = templateService.create(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable UUID id,
            @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        MessageTemplate updated = templateService.update(id, request, principal);
        return ResponseEntity.ok(toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }

    // Mapeig de l'entitat al DTO de resposta
    private TemplateResponse toResponse(MessageTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getName(),
                t.getType(),
                t.getSubject(),
                t.getBody(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
