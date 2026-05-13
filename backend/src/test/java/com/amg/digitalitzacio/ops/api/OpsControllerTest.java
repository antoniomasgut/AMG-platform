package com.amg.digitalitzacio.ops.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.ops.domain.*;
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

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class OpsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private BackupRecordRepository backupRecordRepository;

    private String superAdminToken;
    private String adminToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
        backupRecordRepository.deleteAll();

        var tenant = tenantRepository.save(Tenant.builder()
                .name("Ops Tenant").slug("ops-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@ops.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@ops.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
    }

    @Test
    void tc01_publicHealthCheck_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").isString());
    }

    @Test
    void tc02_allHealthAsSuperAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/health/all")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services").isArray())
                .andExpect(jsonPath("$.summary.total").isNumber());
    }

    @Test
    void tc03_adminCannotAccessAllHealth_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ops/health/all")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc04_allHealthNoAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/ops/health/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc05_listIncidentsEmpty_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/incidents")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void tc06_acknowledgeIncident_returns200() throws Exception {
        var incident = incidentRepository.save(Incident.builder()
                .serviceName("backend").severity(IncidentSeverity.CRITICAL)
                .status(IncidentStatus.OPEN).title("Backend down")
                .startedAt(Instant.now()).build());

        mockMvc.perform(put("/api/v1/ops/incidents/{id}/acknowledge", incident.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"));
    }

    @Test
    void tc07_resolveIncident_returns200() throws Exception {
        var incident = incidentRepository.save(Incident.builder()
                .serviceName("backend").severity(IncidentSeverity.CRITICAL)
                .status(IncidentStatus.OPEN).title("Backend down")
                .startedAt(Instant.now().minusSeconds(120)).build());

        mockMvc.perform(put("/api/v1/ops/incidents/{id}/resolve", incident.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.durationSeconds").isNumber());
    }

    @Test
    void tc08_dashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/dashboard")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").exists())
                .andExpect(jsonPath("$.openIncidents").isNumber());
    }

    @Test
    void tc09_startBackup_returns202() throws Exception {
        var request = new com.amg.digitalitzacio.ops.api.dto.BackupRequest("DATABASE", "Manual backup");

        mockMvc.perform(post("/api/v1/ops/backups")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type").value("DATABASE"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void tc10_listBackups_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/backups")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void tc11_getLogs_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/ops/logs/backend")
                        .param("limit", "50")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void tc12_adminCannotAccessOps_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ops/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
