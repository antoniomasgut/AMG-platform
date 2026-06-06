package com.amg.digitalitzacio.google.api;

import com.amg.digitalitzacio.google.application.GoogleAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/google")
@RequiredArgsConstructor
public class GoogleOAuthCallbackController {

    private final GoogleAuthService authService;

    @GetMapping("/callback")
    public ResponseEntity<String> oauthCallback(
            @RequestParam String code,
            @RequestParam String state) {
        try {
            var result = authService.handleCallback(code, state);
            return ResponseEntity.ok("Connexió Google completada amb: " + result.email());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
}
