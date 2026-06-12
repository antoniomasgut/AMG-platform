package com.amg.digitalitzacio.chat.api;

import com.amg.digitalitzacio.chat.application.ChatSessionService;
import jakarta.servlet.http.HttpServletRequest;
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
    record CreateSessionResponse(String sessionId, String greeting) {}
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
        return new CreateSessionResponse(result.sessionId(), result.greeting());
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSessionResponse createSession(@RequestBody CreateSessionRequest req,
                                               HttpServletRequest httpReq) {
        var result = chatSessionService.createSession(
                req.landingSlug(), req.contactName(), req.contactPhone(), extractIp(httpReq));
        return new CreateSessionResponse(result.sessionId(), result.greeting());
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public SendMessageResponse sendMessage(@PathVariable String sessionId,
                                           @RequestBody SendMessageRequest req,
                                           HttpServletRequest httpReq) {
        var result = chatSessionService.sendMessage(sessionId, req.message(), extractIp(httpReq));
        return new SendMessageResponse(result.sessionId(), result.reply(), result.terminated());
    }

    private String extractIp(HttpServletRequest req) {
        var forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
