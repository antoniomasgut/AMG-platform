package com.amg.digitalitzacio.gocardless.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class GoCardlessControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;

    private String superAdminToken;
    private String adminToken;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.save(Tenant.builder()
                .name("GC Tenant").slug("gc-tenant").isActive(true).build());
        tenantId = tenant.getId();

        var superAdmin = userRepository.save(User.builder()
                .email("sa@gc.com").passwordHash(passwordEncoder.encode("pass1234"))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenantId).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@gc.com").passwordHash(passwordEncoder.encode("pass1234"))
                .name("Admin").role(Role.ADMIN)
                .tenantId(tenantId).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
    }

    @Test
    void tc01_superAdminCanConfigure_returns201() throws Exception {
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "apiKeyRef", "live_key_abc",
                "environment", "SANDBOX",
                "creditorId", "CR123",
                "webhookSecret", "wh_secret");

        mockMvc.perform(post("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.environment").value("SANDBOX"));
    }

    @Test
    void tc02_adminCannotConfigure_returns403() throws Exception {
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "apiKeyRef", "key", "environment", "SANDBOX",
                "creditorId", "CR1", "webhookSecret", "s");

        mockMvc.perform(post("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc03_getConfigNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc04_getConfigAfterConfigure_returns200() throws Exception {
        var body = Map.of(
                "tenantId", tenantId.toString(),
                "apiKeyRef", "live_key", "environment", "LIVE",
                "creditorId", "CR999", "webhookSecret", "sec");

        mockMvc.perform(post("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditorId").value("CR999"))
                .andExpect(jsonPath("$.environment").value("LIVE"));
    }

    @Test
    void tc05_multiTenantIsolation_differentTenantCannotSeeConfig() throws Exception {
        var otherTenantId = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-gc-tenant").isActive(true).build()).getId();

        var body = Map.of(
                "tenantId", tenantId.toString(),
                "apiKeyRef", "key", "environment", "SANDBOX",
                "creditorId", "CRX", "webhookSecret", "s");

        mockMvc.perform(post("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // El tenant configurat retorna la config
        mockMvc.perform(get("/api/v1/gocardless/configure/{tenantId}", tenantId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());

        // Un altre tenant no té config → 404
        mockMvc.perform(get("/api/v1/gocardless/configure/{tenantId}", otherTenantId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc06_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/gocardless/configure/{tenantId}", tenantId))
                .andExpect(status().isUnauthorized());
    }
}
