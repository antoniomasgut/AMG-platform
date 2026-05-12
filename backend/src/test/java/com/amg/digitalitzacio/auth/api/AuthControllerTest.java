package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.*;
import com.amg.digitalitzacio.auth.domain.PasswordResetToken;
import com.amg.digitalitzacio.auth.domain.PasswordResetTokenRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.auth.application.RefreshTokenService;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private PasswordResetTokenRepository resetTokenRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private StringRedisTemplate redis;

    private User activeUser;
    private User blockedUser;
    private Tenant tenant;
    private static final String PASSWORD = "pass1234";
    private static final String WRONG_PASSWORD = "wrongpass";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        resetTokenRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());

        activeUser = userRepository.save(User.builder()
                .email("active@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Active User")
                .role(Role.CLIENT)
                .tenantId(tenant.getId())
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        blockedUser = userRepository.save(User.builder()
                .email("blocked@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Blocked User")
                .role(Role.CLIENT)
                .tenantId(tenant.getId())
                .isActive(true).isBlocked(true).failedAttempts(3)
                .build());
    }

    /* ─────────── TC-01: Login correcte ─────────── */
    @Test
    void tc01_loginCorrecte_Returns200WithTokens() throws Exception {
        var request = new LoginRequest("active@test.com", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("active@test.com"))
                .andExpect(jsonPath("$.user.name").value("Active User"))
                .andExpect(jsonPath("$.user.role").value("CLIENT"))
                .andExpect(jsonPath("$.user.tenant.id").value(tenant.getId().toString()));
    }

    /* ─────────── TC-02: Login incorrecte ─────────── */
    @Test
    void tc02_loginIncorrecte_Returns401AndIncrementsAttempts() throws Exception {
        var request = new LoginRequest("active@test.com", WRONG_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        var user = userRepository.findByEmail("active@test.com").orElseThrow();
        assert user.getFailedAttempts() == 1 : "failedAttempts hauria de ser 1";
    }

    /* ─────────── TC-03: Bloqueig per intents ─────────── */
    @Test
    void tc03_bloqueigPerIntents_BlockedInDb() throws Exception {
        var user = blockedUser;
        user.setIsBlocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);

        for (int i = 0; i < 3; i++) {
            var req = new LoginRequest("blocked@test.com", WRONG_PASSWORD);
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        var updated = userRepository.findByEmail("blocked@test.com").orElseThrow();
        assert updated.getIsBlocked() : "El compte hauria d'estar blocat";
        assert updated.getFailedAttempts() >= 3 : "failedAttempts hauria de ser >= 3";
    }

    /* ─────────── TC-04: Login després de bloqueig ─────────── */
    @Test
    void tc04_loginDespresDeBloqueig_Returns403() throws Exception {
        var request = new LoginRequest("blocked@test.com", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-05: Desbloqueig per SUPER_ADMIN ─────────── */
    @Test
    void tc05_desbloqueigPerSuperAdmin_Returns200() throws Exception {
        var admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin").role(Role.SUPER_ADMIN)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        var token = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());

        mockMvc.perform(post("/api/v1/users/{id}/unlock", blockedUser.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usuari desblocat correctament"));

        var updated = userRepository.findById(blockedUser.getId()).orElseThrow();
        assert !updated.getIsBlocked() : "L'usuari hauria d'estar desblocat";
        assert updated.getFailedAttempts() == 0 : "failedAttempts hauria de ser 0";
    }

    /* ─────────── TC-06: Refresh token vàlid ─────────── */
    @Test
    void tc06_refreshTokenValid_Returns200() throws Exception {
        var loginReq = new LoginRequest("active@test.com", PASSWORD);
        var loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        var loginBody = objectMapper.readValue(loginRes, LoginResponse.class);

        var refreshReq = new RefreshRequest(loginBody.refreshToken());
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    /* ─────────── TC-07: Refresh token expirat ─────────── */
    @Test
    void tc07_refreshTokenExpirat_Returns401() throws Exception {
        var refreshReq = new RefreshRequest("nonexistent-token-id:token");
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── TC-08: Refresh token reutilitzat ─────────── */
    @Test
    void tc08_refreshTokenReutilitzat_Returns401() throws Exception {
        var loginReq = new LoginRequest("active@test.com", PASSWORD);
        var loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        var loginBody = objectMapper.readValue(loginRes, LoginResponse.class);

        var refreshReq = new RefreshRequest(loginBody.refreshToken());
        // Primer ús — hauria de funcionar
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk());

        // Segon ús amb el mateix token — hauria de fallar (possible robatori)
        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-09: Logout ─────────── */
    @Test
    void tc09_logout_Returns200() throws Exception {
        var loginReq = new LoginRequest("active@test.com", PASSWORD);
        var loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        var loginBody = objectMapper.readValue(loginRes, LoginResponse.class);

        var token = loginBody.accessToken();
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new RefreshRequest(loginBody.refreshToken()))))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-10: Forgot password (email existent) ─────────── */
    @Test
    void tc10_forgotPasswordEmailExistent_Returns200() throws Exception {
        var request = new ForgotPasswordRequest("active@test.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    /* ─────────── TC-11: Forgot password (email inexistent) ─────────── */
    @Test
    void tc11_forgotPasswordEmailInexistent_Returns200() throws Exception {
        var request = new ForgotPasswordRequest("noexisteix@test.com");
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());
    }

    /* ─────────── TC-12: Reset password correcte ─────────── */
    @Test
    void tc12_resetPasswordCorrecte_Returns200() throws Exception {
        var rawToken = "my-raw-reset-token-for-test";
        var tokenHash = RefreshTokenService.hashToken(rawToken);
        resetTokenRepository.save(PasswordResetToken.builder()
                .userId(activeUser.getId())
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ResetPasswordRequest(rawToken, "newpass123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isString());

        var loginReq = new LoginRequest("active@test.com", "newpass123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-13: Reset password expirat ─────────── */
    @Test
    void tc13_resetPasswordExpirat_Returns400() throws Exception {
        var expiredToken = resetTokenRepository.save(PasswordResetToken.builder()
                .userId(activeUser.getId())
                .tokenHash("expired-hash")
                .expiresAt(Instant.now().minusSeconds(3600))
                .used(false)
                .build());

        var request = new ResetPasswordRequest("expired-hash", "newpass123");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-14: Reset password token usat ─────────── */
    @Test
    void tc14_resetPasswordTokenUsat_Returns400() throws Exception {
        resetTokenRepository.save(PasswordResetToken.builder()
                .userId(activeUser.getId())
                .tokenHash("used-hash")
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(true)
                .build());

        var request = new ResetPasswordRequest("used-hash", "newpass123");
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-18: GET /me amb JWT ─────────── */
    @Test
    void tc18_getMe_Returns200() throws Exception {
        var loginReq = new LoginRequest("active@test.com", PASSWORD);
        var loginRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andReturn().getResponse().getContentAsString();
        var loginBody = objectMapper.readValue(loginRes, LoginResponse.class);

        mockMvc.perform(get("/api/v1/auth/me")
                .header("Authorization", "Bearer " + loginBody.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("active@test.com"))
                .andExpect(jsonPath("$.name").value("Active User"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }
}
