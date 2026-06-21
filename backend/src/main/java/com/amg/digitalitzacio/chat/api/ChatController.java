package com.amg.digitalitzacio.chat.api;

import com.amg.digitalitzacio.chat.application.ChatSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatSessionService chatSessionService;

    record CreateSessionRequest(String landingSlug, String contactName, String contactPhone) {}
    record CreateAgencySessionRequest(String contactName, String contactPhone) {}
    record HistoryMessage(String role, String content) {}
    record CreateSessionResponse(String sessionId, String greeting, List<HistoryMessage> history) {}

    record SendMessageRequest(String message) {}
    record SendMessageResponse(String sessionId, String reply, boolean terminated) {}
    record AgencyStatusResponse(boolean enabled) {}

    @GetMapping("/agency/status")
    public AgencyStatusResponse getAgencyStatus() {
        return new AgencyStatusResponse(chatSessionService.isAgencyChatEnabled());
    }

    @PostMapping("/agency/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createAgencySession(@RequestBody CreateAgencySessionRequest req,
                                                     HttpServletRequest httpReq) {
        var result = chatSessionService.createAgencySession(req.contactName(), req.contactPhone(), extractIp(httpReq));
        var history = result.history().stream()
                .map(m -> new HistoryMessage(m.getRole(), m.getContent()))
                .toList();
        return new CreateSessionResponse(result.sessionId(), result.greeting(), history);
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createSession(@RequestBody CreateSessionRequest req,
                                               HttpServletRequest httpReq) {
        var result = chatSessionService.createSession(
                req.landingSlug(), req.contactName(), req.contactPhone(), extractIp(httpReq));
        return new CreateSessionResponse(result.sessionId(), result.greeting(), List.of());
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public SendMessageResponse sendMessage(@PathVariable String sessionId,
                                           @RequestBody SendMessageRequest req,
                                           HttpServletRequest httpReq) {
        var result = chatSessionService.sendMessage(sessionId, req.message(), extractIp(httpReq));
        return new SendMessageResponse(result.sessionId(), result.reply(), result.terminated());
    }

    private String extractIp(HttpServletRequest req) {
        // Traefik posa la IP real del client a X-Real-IP (no és manipulable pel client)
        var realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr();
    }
}
