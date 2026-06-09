package com.amg.digitalitzacio.auth.application;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.*;
import com.amg.digitalitzacio.shared.config.JwtProperties;
import com.amg.digitalitzacio.shared.exception.RateLimitExceededException;
import com.amg.digitalitzacio.shared.exception.TokenException;
import com.amg.digitalitzacio.shared.exception.TokenReuseException;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;

    @Transactional
    public LoginResponse login(@Valid LoginRequest request, String ip) {
        if (loginAttemptService.isBlocked(request.email(), ip)) {
            throw new RateLimitExceededException("Massa intents. Prova de nou en 15 minuts.");
        }

        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    loginAttemptService.registerAttempt(request.email(), ip);
                    return new BadCredentialsException("Email o contrasenya incorrectes");
                });

        if (user.getIsBlocked()) {
            throw new LockedException("Compte blocat per intents fallits. Contacta amb l'administrador.");
        }

        if (!user.getIsActive()) {
            throw new LockedException("Compte inactiu. Contacta amb l'administrador.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            loginAttemptService.registerAttempt(request.email(), ip);
            throw new BadCredentialsException("Email o contrasenya incorrectes");
        }

        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        loginAttemptService.resetForEmail(request.email());

        var accessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
        var refreshToken = jwtProvider.generateRefreshToken();
        var refreshTokenId = UUID.randomUUID().toString();
        var tokenHash = RefreshTokenService.hashToken(refreshToken);
        refreshTokenService.save(refreshTokenId, user.getId(), tokenHash);

        LoginResponse.TenantInfo tenantInfo = null;
        if (user.getTenantId() != null) {
            var tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            if (tenant != null) {
                tenantInfo = new LoginResponse.TenantInfo(tenant.getId(), tenant.getName());
            }
        }

        return new LoginResponse(
                accessToken,
                refreshTokenId + ":" + refreshToken,
                jwtProperties.accessTokenExpiration() / 1000,
                "Bearer",
                new LoginResponse.UserInfo(
                        user.getId(), user.getEmail(), user.getName(), user.getPosition(),
                        user.getRole(), tenantInfo));
    }

    @Transactional
    public RefreshResponse refresh(String rawToken) {
        var parts = rawToken.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Format de token invàlid");
        }
        var tokenId = parts[0];
        var incomingToken = parts[1];

        var storedValue = refreshTokenService.getStoredValue(tokenId);
        if (storedValue == null) {
            // Comprovar si el token va ser usat (possible robatori)
            if (refreshTokenService.isUsed(tokenId)) {
                throw new TokenReuseException("Refresh token ja utilitzat — possible robatori de token");
            }
            throw new TokenException("Refresh token invàlid o expirat");
        }

        // Verificar hash del token rebut
        var expectedHash = refreshTokenService.getTokenHash(storedValue);
        var actualHash = RefreshTokenService.hashToken(incomingToken);
        if (!actualHash.equals(expectedHash)) {
            refreshTokenService.markUsed(tokenId);
            throw new TokenReuseException("Refresh token ja utilitzat — possible robatori de token");
        }

        var userId = refreshTokenService.getUserIdFromValue(storedValue);

        // Rotacio: marcar token antic com a usat i eliminar-lo
        refreshTokenService.markUsed(tokenId);
        refreshTokenService.delete(tokenId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));

        var newAccessToken = jwtProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole(), user.getTenantId());
        var newRefreshToken = jwtProvider.generateRefreshToken();
        var newTokenId = UUID.randomUUID().toString();
        var newTokenHash = RefreshTokenService.hashToken(newRefreshToken);
        refreshTokenService.save(newTokenId, user.getId(), newTokenHash);

        return new RefreshResponse(
                newAccessToken,
                newTokenId + ":" + newRefreshToken,
                jwtProperties.accessTokenExpiration() / 1000,
                "Bearer");
    }

    @Transactional
    public void forgotPassword(@Valid ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            return;
        }

        var user = userOpt.get();
        var token = generateSecureToken();
        var tokenHash = RefreshTokenService.hashToken(token);
        var expiresAt = Instant.now().plusSeconds(1800);

        var resetToken = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .used(false)
                .build();
        resetTokenRepository.save(resetToken);

        var resetLink = "https://" + (user.getTenantId() != null ? "app" : "admin")
                + ".amgdigitalitzacio.com/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(request.email(), resetLink);
    }

    @Transactional
    public void resetPassword(@Valid ResetPasswordRequest request) {
        var tokenHash = RefreshTokenService.hashToken(request.token());
        var resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Token invàlid"));

        if (resetToken.getUsed()) {
            throw new IllegalArgumentException("Token ja utilitzat");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Token expirat");
        }

        var user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordChangedAt(Instant.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        refreshTokenService.deleteAllForUser(user.getId());
    }

    public void logout(String tokenId) {
        refreshTokenService.markUsed(tokenId);
        refreshTokenService.delete(tokenId);
    }

    public LoginResponse.UserInfo getCurrentUser(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuari no trobat"));
        LoginResponse.TenantInfo tenantInfo = null;
        if (user.getTenantId() != null) {
            var tenant = tenantRepository.findById(user.getTenantId()).orElse(null);
            if (tenant != null) {
                tenantInfo = new LoginResponse.TenantInfo(tenant.getId(), tenant.getName());
            }
        }
        return new LoginResponse.UserInfo(
                user.getId(), user.getEmail(), user.getName(), user.getPosition(),
                user.getRole(), tenantInfo);
    }

    private String generateSecureToken() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
