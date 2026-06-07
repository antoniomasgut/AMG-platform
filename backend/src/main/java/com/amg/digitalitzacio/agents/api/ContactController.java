package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.*;
import com.amg.digitalitzacio.agents.application.ContactService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agents/contacts")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @GetMapping("/{tenantId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<ContactSummaryResponse>> listContacts(
            @PathVariable UUID tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contactService.listContacts(tenantId));
    }

    @GetMapping("/{tenantId}/{contactId}/thread")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<List<ConversationResponse>> getThread(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(contactService.getThread(tenantId, contactId));
    }

    @PostMapping("/{tenantId}/{contactId}/reply")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> sendReply(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId,
            @Valid @RequestBody SendReplyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        contactService.sendReply(tenantId, contactId, request.text());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{tenantId}/{contactId}/name")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> renameContact(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId,
            @Valid @RequestBody RenameContactRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        contactService.renameContact(tenantId, contactId, request.displayName());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{tenantId}/{contactId}/profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN') or (hasRole('CLIENT') and #principal.tenantId == #tenantId)")
    public ResponseEntity<Void> updateProfile(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId,
            @Valid @RequestBody UpdateContactProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        contactService.updateProfile(tenantId, contactId, request.displayName(), request.phone(), request.email());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{tenantId}/{contactId}/memory")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<Void> clearMemory(
            @PathVariable UUID tenantId,
            @PathVariable UUID contactId) {
        contactService.clearMemory(tenantId, contactId);
        return ResponseEntity.noContent().build();
    }
}
