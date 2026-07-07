package com.amg.digitalitzacio.chat.api;

import com.amg.digitalitzacio.chat.domain.LandingChatContext;
import com.amg.digitalitzacio.chat.domain.LandingChatContextRepository;
import com.amg.digitalitzacio.engine.domain.Landing;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engine/landings/{landingId}/chat")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ChatContextController {

    private final LandingChatContextRepository chatContextRepository;
    private final LandingRepository landingRepository;

    record ChatContextRequest(String businessName, String sector, String systemPrompt, String profanityAction) {}
    record ChatContextResponse(UUID landingId, String businessName, String sector,
                               String systemPrompt, String profanityAction, Instant updatedAt) {}

    @GetMapping
    public ChatContextResponse get(@PathVariable UUID landingId,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        requireLandingAccess(landingId, principal);
        return chatContextRepository.findById(landingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Chat context not found for landing: " + landingId));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public ChatContextResponse upsert(@PathVariable UUID landingId,
                                      @RequestBody ChatContextRequest req,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        requireLandingAccess(landingId, principal);
        var ctx = chatContextRepository.findById(landingId)
                .orElseGet(() -> LandingChatContext.builder().landingId(landingId).build());

        if (req.businessName() != null) ctx.setBusinessName(req.businessName());
        if (req.sector() != null)       ctx.setSector(req.sector());
        if (req.systemPrompt() != null) ctx.setSystemPrompt(req.systemPrompt());
        if (req.profanityAction() != null) ctx.setProfanityAction(req.profanityAction());
        ctx.setUpdatedAt(Instant.now());

        return toResponse(chatContextRepository.save(ctx));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID landingId,
                       @AuthenticationPrincipal UserPrincipal principal) {
        requireLandingAccess(landingId, principal);
        chatContextRepository.deleteById(landingId);
    }

    // Valida que la landing existeix i que l'usuari hi té accés: SUPER_ADMIN/ADMIN sempre,
    // CLIENT només si la landing pertany al seu tenant. Retorna 404 (no 403) per no filtrar existència.
    private void requireLandingAccess(UUID landingId, UserPrincipal principal) {
        Landing landing = landingRepository.findById(landingId)
                .orElseThrow(() -> new ResourceNotFoundException("Landing not found: " + landingId));
        String role = principal.role();
        boolean privileged = "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
        if (!privileged && !landing.getTenantId().equals(principal.tenantId())) {
            throw new ResourceNotFoundException("Landing not found: " + landingId);
        }
    }

    private ChatContextResponse toResponse(LandingChatContext c) {
        return new ChatContextResponse(c.getLandingId(), c.getBusinessName(), c.getSector(),
                c.getSystemPrompt(), c.getProfanityAction(), c.getUpdatedAt());
    }
}
