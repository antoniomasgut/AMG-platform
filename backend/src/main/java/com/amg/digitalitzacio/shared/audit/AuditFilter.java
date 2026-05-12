package com.amg.digitalitzacio.shared.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password"
    );

    private final AuditLogRepository auditLogRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        var path = request.getRequestURI();
        if (isPublic(path) || !path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        var start = System.currentTimeMillis();

        try {
            filterChain.doFilter(request, response);
        } finally {
            var duration = System.currentTimeMillis() - start;
            var status = response.getStatus();
            var auth = SecurityContextHolder.getContext().getAuthentication();
            var userId = extractUserId(auth);
            var email = extractEmail(auth);
            var ip = request.getRemoteAddr();

            auditLogRepository.save(AuditLog.builder()
                    .timestamp(Instant.now())
                    .userId(userId)
                    .email(email)
                    .ip(ip)
                    .method(request.getMethod())
                    .path(path)
                    .status(status)
                    .durationMs(duration)
                    .build());
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private UUID extractUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        var principal = auth.getPrincipal();
        if (principal instanceof com.amg.digitalitzacio.shared.security.UserPrincipal up) {
            return up.id();
        }
        return null;
    }

    private String extractEmail(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getName();
    }
}
