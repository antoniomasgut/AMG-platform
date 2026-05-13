package com.amg.digitalitzacio.automations.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.automations.api.dto.*;
import com.amg.digitalitzacio.automations.domain.*;
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
class AutomationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private WorkflowTemplateRepository workflowTemplateRepository;
    @Autowired private TenantWorkflowRepository tenantWorkflowRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        tenantWorkflowRepository.deleteAll();
        workflowTemplateRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Automation Test Tenant").slug("auto-test-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Client User").role(Role.CLIENT)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
        clientToken = jwtProvider.generateAccessToken(
                client.getId(), client.getEmail(), client.getRole(), client.getTenantId());
    }

    @Test
    void tc01_listTemplates_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/automations/templates")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void tc02_assignWorkflow_returns201() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Formulari a WhatsApp").key("form-to-whatsapp")
                .description("Envia un missatge de WhatsApp quan s'omple un formulari")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());

        var request = new AssignWorkflowRequest(template.getKey(), Map.of("targetPhone", "+34600123456"));

        mockMvc.perform(post("/api/v1/automations/tenants/{tenantId}/workflows", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.templateKey").value("form-to-whatsapp"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void tc03_deployWorkflow_returns200() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-deploy")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        var workflow = tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.PENDING).build());

        mockMvc.perform(post("/api/v1/automations/workflows/{workflowId}/deploy", workflow.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEPLOYED"))
                .andExpect(jsonPath("$.n8nWorkflowId").isNotEmpty());
    }

    @Test
    void tc04_activateWorkflow_returns200() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-activate")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        var workflow = tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.DEPLOYED).build());

        mockMvc.perform(post("/api/v1/automations/workflows/{workflowId}/activate", workflow.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void tc05_deactivateWorkflow_returns200() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-deactivate")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        var workflow = tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.ACTIVE).build());

        mockMvc.perform(post("/api/v1/automations/workflows/{workflowId}/deactivate", workflow.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISABLED"));
    }

    @Test
    void tc06_listWorkflows_returns200() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-list")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.ACTIVE).build());

        mockMvc.perform(get("/api/v1/automations/tenants/{tenantId}/workflows", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflows", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void tc07_listExecutions_returns200() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-execs")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        var workflow = tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.ACTIVE).build());

        mockMvc.perform(get("/api/v1/automations/workflows/{workflowId}/executions", workflow.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void tc08_webhookPublic_returns200() throws Exception {
        var request = new WebhookRequest("wf-1", "exec-1", "success", Map.of("result", "ok"), null, null, null);

        mockMvc.perform(post("/api/v1/automations/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    void tc09_clientCannotDeploy_returns403() throws Exception {
        var template = workflowTemplateRepository.save(WorkflowTemplate.builder()
                .name("Test").key("test-forbidden")
                .category(WorkflowCategory.BASIC)
                .activationType(WorkflowActivationType.MANUAL)
                .build());
        var workflow = tenantWorkflowRepository.save(TenantWorkflow.builder()
                .tenantId(tenant.getId()).templateId(template.getId())
                .status(TenantWorkflowStatus.PENDING).build());

        mockMvc.perform(post("/api/v1/automations/workflows/{workflowId}/deploy", workflow.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc10_accessWithoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/automations/templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc11_webhookHealthPublic_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/automations/webhook/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
