package com.amg.digitalitzacio.auth.api;

import com.amg.digitalitzacio.auth.api.dto.CreateUserRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class UserControllerTest {

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
    private User clientUser;
    private Tenant tenant;
    private String adminToken;
    private String clientToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());

        superAdmin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("pass1234"))
                .name("Super Admin")
                .role(Role.SUPER_ADMIN)
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
        clientToken = jwtProvider.generateAccessToken(
                clientUser.getId(), clientUser.getEmail(), clientUser.getRole(), clientUser.getTenantId());
    }

    /* ─────────── TC-15: Crear usuari CLIENT (SUPER_ADMIN) ─────────── */
    @Test
    void tc15_createClientUserAsSuperAdmin_Returns201() throws Exception {
        var request = new CreateUserRequest(
                "newclient@test.com", "pass1234", "New Client",
                Role.CLIENT, tenant.getId());

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("newclient@test.com"))
                .andExpect(jsonPath("$.name").value("New Client"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.tenant.id").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    /* ─────────── TC-16: Crear CLIENT sense tenantId ─────────── */
    @Test
    void tc16_createClientUserWithoutTenant_Returns400() throws Exception {
        var request = new CreateUserRequest(
                "noclient@test.com", "pass1234", "No Tenant",
                Role.CLIENT, null);

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-17: CLIENT intenta crear usuari ─────────── */
    @Test
    void tc17_clientTriesToCreateUser_Returns403() throws Exception {
        var request = new CreateUserRequest(
                "another@test.com", "pass1234", "Another",
                Role.CLIENT, tenant.getId());

        mockMvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-18: SUPER_ADMIN veu perfil de client ─────────── */
    @Test
    void tc18_adminSeesClientProfile_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", clientUser.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(clientUser.getId().toString()))
                .andExpect(jsonPath("$.email").value("client@test.com"))
                .andExpect(jsonPath("$.name").value("Client User"))
                .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    /* ─────────── TC-19: CLIENT veu perfil d'altre usuari ─────────── */
    @Test
    void tc19_clientSeesOtherUserProfile_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/users/{id}", superAdmin.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
