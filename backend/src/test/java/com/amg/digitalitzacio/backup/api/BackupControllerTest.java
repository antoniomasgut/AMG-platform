package com.amg.digitalitzacio.backup.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.backup.api.dto.RestoreRequest;
import com.amg.digitalitzacio.backup.api.dto.StartBackupRequest;
import com.amg.digitalitzacio.backup.domain.*;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class BackupControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private BackupTaskRepository backupTaskRepository;
    @Autowired private RestoreTaskRepository restoreTaskRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private static final String PASSWORD = "pass1234";
    private static final String BASE_URL = "/api/v1/ops/backups";

    @BeforeEach
    void setUp() {
        restoreTaskRepository.deleteAll();
        backupTaskRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant-backup").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin-backup@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin-backup@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
    }

    /* ── TC-01: POST /backups sense autenticació → 401 ── */
    @Test
    void tc01_startBackup_senseAuth_Returns401() throws Exception {
        var request = new StartBackupRequest("MANUAL_FULL", null);

        mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    /* ── TC-02: POST /backups amb rol ADMIN → 403 ── */
    @Test
    void tc02_startBackup_ambAdminRole_Returns403() throws Exception {
        var request = new StartBackupRequest("MANUAL_FULL", null);

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-03: POST /backups MANUAL_FULL amb SUPER_ADMIN → 202 ── */
    @Test
    void tc03_startBackup_manualFull_Returns202() throws Exception {
        var request = new StartBackupRequest("MANUAL_FULL", null);

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.type").value("MANUAL_FULL"));
    }

    /* ── TC-04: POST /backups MANUAL_TENANT amb tenantId vàlid → 202 ── */
    @Test
    void tc04_startBackup_manualTenant_ambTenantValid_Returns202() throws Exception {
        var request = new StartBackupRequest("MANUAL_TENANT", tenant.getId());

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type").value("MANUAL_TENANT"))
                .andExpect(jsonPath("$.scope").value("SINGLE_TENANT"));
    }

    /* ── TC-05: POST /backups MANUAL_TENANT sense tenantId → 404 ── */
    @Test
    void tc05_startBackup_manualTenant_senseTenantId_Returns404() throws Exception {
        var request = new StartBackupRequest("MANUAL_TENANT", null);

        mockMvc.perform(post(BASE_URL)
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    /* ── TC-06: GET /backups buit → 200 llista buida ── */
    @Test
    void tc06_listBackups_buit_Returns200() throws Exception {
        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /* ── TC-07: GET /backups després de crear un backup → 200 amb 1 element ── */
    @Test
    void tc07_listBackups_ambBackupCreat_Returns200() throws Exception {
        backupTaskRepository.save(BackupTask.builder()
                .type(BackupTaskType.MANUAL_FULL)
                .status(BackupTaskStatus.COMPLETED)
                .scope(BackupScope.ALL_TENANTS)
                .totalTenants(1).completedTenants(1).failedTenants(0)
                .startedAt(Instant.now())
                .retentionUntil(Instant.now().plusSeconds(86400L * 30))
                .build());

        mockMvc.perform(get(BASE_URL)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    /* ── TC-08: GET /backups/{id}/download inexistent → 404 ── */
    @Test
    void tc08_getBackup_inexistent_Returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    /* ── TC-09: GET /backups/dashboard → 200 ── */
    @Test
    void tc09_getDashboard_Returns200() throws Exception {
        mockMvc.perform(get(BASE_URL + "/dashboard")
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBackups").isNumber())
                .andExpect(jsonPath("$.retentionDays").value(30));
    }

    /* ── TC-10: GET /backups/{id} existent → 200 ── */
    @Test
    void tc10_getBackup_existent_Returns200() throws Exception {
        var task = backupTaskRepository.save(BackupTask.builder()
                .type(BackupTaskType.MANUAL_FULL)
                .status(BackupTaskStatus.COMPLETED)
                .scope(BackupScope.ALL_TENANTS)
                .totalTenants(1).completedTenants(1).failedTenants(0)
                .startedAt(Instant.now())
                .retentionUntil(Instant.now().plusSeconds(86400L * 30))
                .build());

        mockMvc.perform(get(BASE_URL + "/" + task.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId().toString()))
                .andExpect(jsonPath("$.type").value("MANUAL_FULL"));
    }

    /* ── TC-11: GET /backups/{id} inexistent → 404 ── */
    @Test
    void tc11_getBackup_idInexistent_Returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/" + UUID.randomUUID())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNotFound());
    }

    /* ── TC-12: POST /backups/restore amb SUPER_ADMIN → 202 ── */
    @Test
    void tc12_startRestore_ambSuperAdmin_Returns202() throws Exception {
        var request = new RestoreRequest(tenant.getId(), null, List.of("leads", "vault"));

        mockMvc.perform(post(BASE_URL + "/restore")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    /* ── TC-13: DELETE /backups/{id} existent → 204 ── */
    @Test
    void tc13_deleteBackup_existent_Returns204() throws Exception {
        var task = backupTaskRepository.save(BackupTask.builder()
                .type(BackupTaskType.MANUAL_FULL)
                .status(BackupTaskStatus.COMPLETED)
                .scope(BackupScope.ALL_TENANTS)
                .totalTenants(1).completedTenants(1).failedTenants(0)
                .startedAt(Instant.now())
                .retentionUntil(Instant.now().plusSeconds(86400L * 30))
                .build());

        mockMvc.perform(delete(BASE_URL + "/" + task.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());
    }

    /* ── TC-14: POST /backups/export/{tenantId} → 202 ── */
    @Test
    void tc14_exportTenant_Returns202() throws Exception {
        mockMvc.perform(post(BASE_URL + "/export/" + tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.type").value("MANUAL_TENANT"))
                .andExpect(jsonPath("$.scope").value("SINGLE_TENANT"))
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }
}
