package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class TenantControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtProvider jwtProvider;

    private User superAdmin;
    private User adminUser;
    private User clientUser;
    private Tenant tenant;
    private Tenant otherTenant;
    private String adminToken;
    private String clientToken;
    private String operatorToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());
        otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-tenant").isActive(true).build());

        superAdmin = userRepository.save(User.builder()
                .email("super@test.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .name("Super Admin")
                .role(Role.SUPER_ADMIN)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("operator@test.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .name("Operator Admin")
                .role(Role.ADMIN)
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        clientUser = userRepository.save(User.builder()
                .email("client@test.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .name("Client User")
                .role(Role.CLIENT)
                .tenantId(tenant.getId())
                .isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        adminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        operatorToken = jwtProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole(), adminUser.getTenantId());
        clientToken = jwtProvider.generateAccessToken(
                clientUser.getId(), clientUser.getEmail(), clientUser.getRole(), clientUser.getTenantId());
    }

    /* ─────────── TC-20a: SUPER_ADMIN veu tenant ─────────── */
    @Test
    void tc20_adminSeesTenant_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{id}", tenant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.name").value("Test Tenant"))
                .andExpect(jsonPath("$.slug").value("test-tenant"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    /* ─────────── TC-20b: CLIENT no pot veure UN ALTRE tenant ─────────── */
    @Test
    void tc20_clientCannotSeeOtherTenant_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{id}", otherTenant.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-20: CLIENT veu el seu propi tenant ─────────── */
    @Test
    void tc20_clientSeesOwnTenant_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/{id}", tenant.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Tenant"));
    }

    /* ─────────── TC-24: ADMIN modifica tenant ─────────── */
    @Test
    void tc24_adminUpdatesTenant_Returns200() throws Exception {
        var json = """
                {"name": "Updated Tenant"}
                """;

        mockMvc.perform(put("/api/v1/tenants/{id}", tenant.getId())
                .header("Authorization", "Bearer " + operatorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Tenant"));
    }

    /* ─────────── ADMIN llista tenants ─────────── */
    @Test
    void adminListsTenants_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/tenants")
                .header("Authorization", "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
