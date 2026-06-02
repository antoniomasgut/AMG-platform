package com.amg.digitalitzacio.chat.api;

import com.amg.digitalitzacio.chat.domain.LandingChatContext;
import com.amg.digitalitzacio.chat.domain.LandingChatContextRepository;
import com.amg.digitalitzacio.engine.domain.LandingRepository;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/engine/landings/{landingId}/chat")
@RequiredArgsConstructor
public class ChatContextController {

    private final LandingChatContextRepository chatContextRepository;
    private final LandingRepository landingRepository;

    record ChatContextRequest(String businessName, String sector, String systemPrompt, String profanityAction) {}
    record ChatContextResponse(UUID landingId, String businessName, String sector,
                               String systemPrompt, String profanityAction, Instant updatedAt) {}

    @GetMapping
    public ChatContextResponse get(@PathVariable UUID landingId) {
        return chatContextRepository.findById(landingId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Chat context not found for landing: " + landingId));
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public ChatContextResponse upsert(@PathVariable UUID landingId,
                                      @RequestBody ChatContextRequest req) {
        if (!landingRepository.existsById(landingId)) {
            throw new ResourceNotFoundException("Landing not found: " + landingId);
        }
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
    public void delete(@PathVariable UUID landingId) {
        chatContextRepository.deleteById(landingId);
    }

    private ChatContextResponse toResponse(LandingChatContext c) {
        return new ChatContextResponse(c.getLandingId(), c.getBusinessName(), c.getSector(),
                c.getSystemPrompt(), c.getProfanityAction(), c.getUpdatedAt());
    }
}
