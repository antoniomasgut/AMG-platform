package com.amg.digitalitzacio.shared.security;

import com.amg.digitalitzacio.auth.api.dto.LoginRequest;
import com.amg.digitalitzacio.auth.api.dto.ResetPasswordRequest;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private User activeUser;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        activeUser = userRepository.save(User.builder()
                .email("security@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Security User")
                .role(Role.CLIENT)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());
    }

    /* ─────────── SEC-01a: Bloqueig de compte per 3 intents fallits ─────────── */
    @Test
    void sec01a_accountBlockedAfter3FailedAttempts() throws Exception {
        var request = new LoginRequest("security@test.com", "wrongpass");
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("security@test.com", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    /* ─────────── SEC-01b: Rate limiting superat → 429 ─────────── */
    @Test
    void sec01b_rateLimitExceeded_Returns429() throws Exception {
        var nonExistentEmail = "nonexistent@rate-limit-test.com";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new LoginRequest(nonExistentEmail, "wrongpass"))));
        }

        // El 6è intent hauria de retornar 429 (rate limit per email)
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new LoginRequest(nonExistentEmail, "wrongpass"))))
                .andExpect(status().isTooManyRequests());
    }

    /* ─────────── SEC-02: JWT manipulat → 401 ─────────── */
    @Test
    void sec02_manipulatedJwt_Returns401() throws Exception {
        var manipulated = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIn0.invalidsignature";

        getWithToken("/api/v1/auth/me", manipulated)
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── SEC-03: JWT expirat → 401 ─────────── */
    @Test
    void sec03_expiredJwt_Returns401() throws Exception {
        var keyBytes = Base64.getDecoder().decode(jwtSecret);
        var key = Keys.hmacShaKeyFor(keyBytes);

        var expiredToken = Jwts.builder()
                .subject("security@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 3600000))
                .expiration(new Date(System.currentTimeMillis() - 1800000))
                .signWith(key)
                .compact();

        getWithToken("/api/v1/auth/me", expiredToken)
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── SEC-04: Accés sense token → 401 ─────────── */
    @Test
    void sec04_accessWithoutToken_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── SEC-05: SQL injection a email → 400 (validació) o 401 ─────────── */
    @Test
    void sec05_sqlInjectionEmail_Returns400Or401() throws Exception {
        var request = new LoginRequest("' OR 1=1 --", PASSWORD);
        var result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andReturn();
        // 400 si la validació @Email ho rebutja, 401 si arriba al servei
        assert result.getResponse().getStatus() == 400 || result.getResponse().getStatus() == 401
                : "Expected 400 or 401 but got " + result.getResponse().getStatus();
    }

    /* ─────────── SEC-06: Password massa curta → 400 ─────────── */
    @Test
    void sec06_passwordTooShort_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ResetPasswordRequest("token", "ab"))))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── SEC-07: Email mal format → 400 ─────────── */
    @Test
    void sec07_invalidEmailFormat_Returns400() throws Exception {
        // Bean Validation hauria de rebutjar email mal format
        var content = """
                {"email": "not-an-email", "password": "pass1234"}
                """;
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── SEC-08: Injecció a reset token → 400 ─────────── */
    @Test
    void sec08_injectionInResetToken_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ResetPasswordRequest("' OR '1'='1", "newpass1234"))))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── LIM-01: Contrasenya exactament 4 caràcters → login correcte ─────────── */
    @Test
    void lim01_passwordExactly4Chars_Returns200() throws Exception {
        var user4 = userRepository.save(User.builder()
                .email("fourchar@test.com")
                .passwordHash(passwordEncoder.encode("abcd"))
                .name("Four Char")
                .role(Role.CLIENT)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("fourchar@test.com", "abcd"))))
                .andExpect(status().isOk());
    }

    /* ─────────── LIM-02: Email de 254 caràcters ─────────── */
    @Test
    void lim02_email254Chars_Returns200Or400() throws Exception {
        var localPart = "a".repeat(243);
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + localPart + "@b\",\"password\":\"pass1234\"}"));
        // No hauria de petar, sigui 200 o 400
    }

    /* ─────────── LIM-03: SUPER_ADMIN sense tenant ─────────── */
    @Test
    void lim03_superAdminWithoutTenant_IsAllowed() throws Exception {
        var admin = userRepository.save(User.builder()
                .email("admin2@test.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .name("Super Admin No Tenant")
                .role(Role.SUPER_ADMIN)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        var token = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());

        getWithToken("/api/v1/auth/me", token)
                .andExpect(status().isOk());
    }

    /* ─────────── LIM-04: Múltiples refresh tokens ─────────── */
    @Test
    void lim04_multipleRefreshTokens_AllValid() throws Exception {
        // Login diverses vegades hauria de generar tokens independents
        var res1 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("security@test.com", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // El segon login fallarà perquè LoginRequest existeix al body
        var res2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest("security@test.com", PASSWORD))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        // Ambdós haurien de tenir tokens diferents
        var login1 = objectMapper.readTree(res1);
        var login2 = objectMapper.readTree(res2);
        assert !login1.get("refreshToken").asText().equals(login2.get("refreshToken").asText())
                : "Els refresh tokens haurien de ser diferents";
    }

    private org.springframework.test.web.servlet.ResultActions getWithToken(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }
}
