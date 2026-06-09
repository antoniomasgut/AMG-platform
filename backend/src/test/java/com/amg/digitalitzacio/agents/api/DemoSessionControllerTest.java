package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.domain.DemoSessionRepository;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.demo.application.DemoLandingService;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class DemoSessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private DemoSessionRepository demoSessionRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // Mockeja el servei de creació de landings per aïllar el test del motor d'Engine
    @MockBean private DemoLandingService demoLandingService;

    private User adminUser;
    private String adminToken;

    private static final String SECTOR = "PERRUQUERIA";
    private static final String DEMO_SLUG = "demo-test-slug";

    @BeforeEach
    void setUp() {
        demoSessionRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        var tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant")
                .slug("test-tenant")
                .isActive(true)
                .build());

        adminUser = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("test1234"))
                .name("Admin User")
                .role(Role.ADMIN)
                .tenantId(tenant.getId())
                .isActive(true)
                .isBlocked(false)
                .build());

        adminToken = jwtProvider.generateAccessToken(
                adminUser.getId(), adminUser.getEmail(), adminUser.getRole(), adminUser.getTenantId());

        // Stub del servei de landings
        var mockTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("DEMO_" + SECTOR)
                .slug("demo-" + SECTOR.toLowerCase())
                .isActive(true)
                .build();
        when(demoLandingService.getOrCreateDemoTenant(SECTOR)).thenReturn(mockTenant);
        when(demoLandingService.createAndPublishDemoLanding(
                any(UUID.class), any(UUID.class), eq(SECTOR), any(), any()))
                .thenReturn(DEMO_SLUG);
    }

    // ── Creació ───────────────────────────────────────────────────────────────

    @Test
    void createSession_returns201_withRequiredFields() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", SECTOR,
                "companyName", "Perruqueria Mireia",
                "locale", "ca"));

        mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.sector").value(SECTOR))
                .andExpect(jsonPath("$.companyName").value("Perruqueria Mireia"))
                .andExpect(jsonPath("$.locale").value("ca"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.landingUrl").value(containsString(DEMO_SLUG)));
    }

    @Test
    void createSession_locale_es_propagated() throws Exception {
        when(demoLandingService.createAndPublishDemoLanding(
                any(UUID.class), any(UUID.class), eq(SECTOR), any(), eq("es")))
                .thenReturn("demo-es-slug");

        var body = objectMapper.writeValueAsString(Map.of(
                "sector", SECTOR,
                "companyName", "Peluquería Sol",
                "locale", "es"));

        mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locale").value("es"))
                .andExpect(jsonPath("$.landingUrl").value(containsString("/es")));
    }

    @Test
    void createSession_defaultLocale_ca_whenOmitted() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", SECTOR,
                "companyName", "Sense idioma"));

        mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locale").value("ca"));
    }

    @Test
    void createSession_unauthenticated_returns401() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of("sector", SECTOR, "companyName", "X"));

        mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    // ── Llistat ───────────────────────────────────────────────────────────────

    @Test
    void listSessions_returns200_withCreatedSession() throws Exception {
        // Crea primer una sessió
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", SECTOR, "companyName", "Empresa Test", "locale", "en"));

        mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Llista
        mockMvc.perform(get("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].companyName").value("Empresa Test"))
                .andExpect(jsonPath("$[0].locale").value("en"));
    }

    @Test
    void listSessions_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/demo/sessions"))
                .andExpect(status().isUnauthorized());
    }

    // ── Desactivació ──────────────────────────────────────────────────────────

    @Test
    void deactivateSession_returns204_andSessionIsInactive() throws Exception {
        // Crea sessió
        var createBody = objectMapper.writeValueAsString(Map.of(
                "sector", SECTOR, "companyName", "A Desactivar", "locale", "ca"));

        var result = mockMvc.perform(post("/api/v1/admin/demo/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();

        // Desactiva
        mockMvc.perform(delete("/api/v1/admin/demo/sessions/" + token)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verifica que la sessió pública retorna 404
        mockMvc.perform(get("/api/v1/demo/sessions/" + token))
                .andExpect(status().isNotFound());
    }

    // ── Endpoint públic ───────────────────────────────────────────────────────

    @Test
    void publicSession_unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/demo/sessions/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
