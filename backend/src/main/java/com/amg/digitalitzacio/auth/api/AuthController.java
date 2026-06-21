package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.application.AuthService;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest servletRequest) {
        String ip = servletRequest.getHeader("X-Real-IP");
        if (ip == null || ip.isBlank()) {
            ip = servletRequest.getHeader("X-Forwarded-For");
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }
        if (ip == null || ip.isBlank()) {
            ip = servletRequest.getRemoteAddr();
        }
        var response = authService.login(request, ip);
        var cookie = ResponseCookie.from("access_token", response.accessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(900)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        var response = authService.refresh(request.refreshToken());
        var cookie = ResponseCookie.from("access_token", response.accessToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(900)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody RefreshRequest request) {
        var parts = request.refreshToken().split(":", 2);
        if (parts.length == 2) {
            authService.logout(parts[0]);
        }
        return ResponseEntity.ok(Map.of());
    }

    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserInfo> me(@AuthenticationPrincipal UserPrincipal principal) {
        var user = authService.getCurrentUser(principal.id());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(new MessageResponse("S'ha enviat un enllaç de recuperació al teu email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Contrasenya actualitzada correctament"));
    }
}
