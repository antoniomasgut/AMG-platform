package com.amg.digitalitzacio.social.api;

import com.amg.digitalitzacio.social.application.LinkedInAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Callback OAuth de LinkedIn (Mòdul 56 F4). Redirigeix al portal amb el resultat.
 */
@RestController
@RequestMapping("/api/v1/social/linkedin")
@RequiredArgsConstructor
@Slf4j
public class LinkedInOAuthCallbackController {

    private final LinkedInAuthService authService;

    @Value("${app.frontend-url:https://portal.amg.cat}")
    private String frontendUrl;

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        String base = frontendUrl + "/ca/portal/admin/social/linkedin";
        if (error != null || code == null || state == null) {
            return redirect(base + "?error=" + enc(error != null ? error : "cancelled"));
        }
        try {
            var result = authService.handleCallback(code, state);
            return redirect(base + "?success=true&name=" + enc(result.displayName()));
        } catch (Exception e) {
            log.error("LinkedIn OAuth callback error: {}", e.getMessage());
            return redirect(base + "?error=" + enc(e.getMessage()));
        }
    }

    private ResponseEntity<Void> redirect(String url) {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, url).build();
    }

    private String enc(String s) {
        return URLEncoder.encode(s != null ? s : "", StandardCharsets.UTF_8);
    }
}
