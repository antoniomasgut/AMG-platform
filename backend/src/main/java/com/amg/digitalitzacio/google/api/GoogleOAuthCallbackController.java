package com.amg.digitalitzacio.google.api;

import com.amg.digitalitzacio.google.application.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/google")
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthCallbackController {

    private final GoogleAuthService authService;

    @Value("${app.frontend-url:https://portal.amg.cat}")
    private String frontendUrl;

    @GetMapping("/callback")
    public ResponseEntity<Void> oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            var result = authService.handleCallback(code, state);
            var redirectUrl = frontendUrl + "/ca/portal/admin/integrations/google?success=true&email="
                + java.net.URLEncoder.encode(result.email(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
        } catch (Exception e) {
            log.error("Google OAuth callback error: {}", e.getMessage());
            var redirectUrl = frontendUrl + "/ca/portal/admin/integrations/google?error="
                + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .build();
        }
    }
}
