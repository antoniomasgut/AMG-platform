package com.amg.digitalitzacio.comm.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplate;
import com.amg.digitalitzacio.comm.domain.CommunicationTemplateRepository;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class CommunicationTemplateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private CommunicationTemplateRepository templateRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;
    private CommunicationTemplate existingTemplate;

    @BeforeEach
    void setUp() {
        templateRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        var tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());

        var adminUser = userRepository.save(User.builder()
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

        existingTemplate = templateRepository.save(CommunicationTemplate.builder()
                .sector("ALL")
                .channel("EMAIL")
                .action("DEMO_SEND")
                .language("ca")
                .subject("Demo per a {COMPANY}")
                .body("Hola {NAME}, et presentem la teva demo personalitzada: {URL}")
                .isActive(true)
                .sortOrder(0)
                .build());
    }

    // ── Llistat ───────────────────────────────────────────────────────────────

    @Test
    void list_returns200_withExistingTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/admin/comm/templates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].action").value("DEMO_SEND"))
                .andExpect(jsonPath("$[0].channel").value("EMAIL"))
                .andExpect(jsonPath("$[0].language").value("ca"));
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/comm/templates"))
                .andExpect(status().isUnauthorized());
    }

    // ── Creació ───────────────────────────────────────────────────────────────

    @Test
    void create_returns201_withNewTemplate() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", "PERRUQUERIA",
                "channel", "WHATSAPP",
                "action", "APPOINTMENT_CONFIRM",
                "language", "es",
                "body", "Hola {NAME}, tu cita está confirmada para el {DATE}",
                "isActive", true,
                "sortOrder", 10));

        mockMvc.perform(post("/api/v1/admin/comm/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.sector").value("PERRUQUERIA"))
                .andExpect(jsonPath("$.channel").value("WHATSAPP"))
                .andExpect(jsonPath("$.action").value("APPOINTMENT_CONFIRM"))
                .andExpect(jsonPath("$.language").value("es"));
    }

    @Test
    void create_conflict_returns409_whenDuplicate() throws Exception {
        // Intenta crear un duplicat de la plantilla existent
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", "ALL",
                "channel", "EMAIL",
                "action", "DEMO_SEND",
                "language", "ca",
                "body", "Contingut duplicat",
                "isActive", true,
                "sortOrder", 0));

        mockMvc.perform(post("/api/v1/admin/comm/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void create_sameAction_differentLanguage_returns201() throws Exception {
        // Mateixa acció i sector però idioma diferent → no és conflicte
        var body = objectMapper.writeValueAsString(Map.of(
                "sector", "ALL",
                "channel", "EMAIL",
                "action", "DEMO_SEND",
                "language", "es",
                "body", "Hola {NAME}, aquí tienes tu demo: {URL}",
                "isActive", true,
                "sortOrder", 0));

        mockMvc.perform(post("/api/v1/admin/comm/templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.language").value("es"));
    }

    // ── Actualització ─────────────────────────────────────────────────────────

    @Test
    void update_returns200_withNewBody() throws Exception {
        var body = objectMapper.writeValueAsString(Map.of(
                "body", "Nou contingut actualitzat per a {NAME}",
                "isActive", true,
                "sortOrder", 5));

        mockMvc.perform(put("/api/v1/admin/comm/templates/" + existingTemplate.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Nou contingut actualitzat per a {NAME}"))
                .andExpect(jsonPath("$.sortOrder").value(5));
    }

    // ── Eliminació ────────────────────────────────────────────────────────────

    @Test
    void delete_returns204_andTemplateIsGone() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/comm/templates/" + existingTemplate.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/admin/comm/templates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
