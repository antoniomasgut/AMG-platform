package com.amg.digitalitzacio.leads.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.leads.api.dto.*;
import com.amg.digitalitzacio.leads.domain.*;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private User adminUser;
    private User clientUser;
    private String adminToken;
    private String clientToken;
    private Lead existingLead;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
        leadRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
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

        clientUser = userRepository.save(User.builder()
                .email("client@test.com")
                .passwordHash(passwordEncoder.encode("test1234"))
                .name("Client User")
                .role(Role.CLIENT)
                .tenantId(tenant.getId())
                .isActive(true)
                .isBlocked(false)
                .build());

        adminToken = jwtProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole(), adminUser.getTenantId());
        clientToken = jwtProvider.generateAccessToken(clientUser.getId(), clientUser.getEmail(), clientUser.getRole(), clientUser.getTenantId());

        existingLead = leadRepository.save(createLeadEntity("Joan Servera", LeadSource.WEB));
    }

    private Lead createLeadEntity(String name, LeadSource source) {
        Lead lead = new Lead();
        lead.setTenantId(tenant.getId());
        lead.setName(name);
        lead.setEmail(name.toLowerCase().replace(" ", ".") + "@test.com");
        lead.setPhone("+34 600 000 000");
        lead.setSource(source);
        lead.setStage(PipelineStage.NEW);
        return lead;
    }

    // --- Create Lead ---

    @Test
    void createLead_returns201() throws Exception {
        var body = objectMapper.writeValueAsString(
                new LeadRequest("Maria Pons", "maria@test.com", "+34 611 111 111",
                        LeadSource.WHATSAPP, adminUser.getId(), null, "Interessada en pla basic", null));

        mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Pons"))
                .andExpect(jsonPath("$.stage").value("NEW"))
                .andExpect(jsonPath("$.source").value("WHATSAPP"));
    }

    @Test
    void createLead_withoutName_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(
                new LeadRequest(null, "maria@test.com", null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- List Leads ---

    @Test
    void listLeads_returnsPage() throws Exception {
        mockMvc.perform(get("/api/v1/leads")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void listLeads_filterByStage() throws Exception {
        mockMvc.perform(get("/api/v1/leads?stage=NEW")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void listLeads_searchByName() throws Exception {
        mockMvc.perform(get("/api/v1/leads?search=Joan")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Joan Servera"));
    }

    // --- Get Lead ---

    @Test
    void getLead_returnsLead() throws Exception {
        mockMvc.perform(get("/api/v1/leads/{id}", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingLead.getId().toString()))
                .andExpect(jsonPath("$.name").value("Joan Servera"));
    }

    // --- Update Lead ---

    @Test
    void updateLead_returnsUpdatedLead() throws Exception {
        var body = objectMapper.writeValueAsString(
                new LeadRequest("Joan Servera Updated", null, null, null, null, null, null, null));

        mockMvc.perform(put("/api/v1/leads/{id}", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Joan Servera Updated"));
    }

    // --- Delete Lead ---

    @Test
    void deleteLead_admin_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/leads/{id}", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteLead_client_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/leads/{id}", existingLead.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    // --- Change Stage ---

    @Test
    void changeStage_toContacted_returns200() throws Exception {
        var body = objectMapper.writeValueAsString(new StageChangeRequest(PipelineStage.CONTACTED, null));

        mockMvc.perform(patch("/api/v1/leads/{id}/stage", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("CONTACTED"));
    }

    @Test
    void changeStage_toLostWithoutReason_returns400() throws Exception {
        var body = objectMapper.writeValueAsString(new StageChangeRequest(PipelineStage.LOST, null));

        mockMvc.perform(patch("/api/v1/leads/{id}/stage", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStage_toWon_returns200() throws Exception {
        var body = objectMapper.writeValueAsString(new StageChangeRequest(PipelineStage.WON, null));

        mockMvc.perform(patch("/api/v1/leads/{id}/stage", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stage").value("WON"));
    }

    // --- Stats ---

    @Test
    void getStats_returnsCounts() throws Exception {
        mockMvc.perform(get("/api/v1/leads/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.byStage").isMap())
                .andExpect(jsonPath("$.conversionRate").isNumber());
    }

    // --- Activities ---

    @Test
    void createActivity_returns201() throws Exception {
        var body = objectMapper.writeValueAsString(
                new ActivityRequest(ActivityType.NOTE, "Pendent de trucar", null));

        mockMvc.perform(post("/api/v1/leads/{leadId}/activities", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("NOTE"))
                .andExpect(jsonPath("$.description").value("Pendent de trucar"));
    }

    @Test
    void listActivities_returnsPage() throws Exception {
        mockMvc.perform(get("/api/v1/leads/{leadId}/activities", existingLead.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // --- Tenant isolation ---

    @Test
    void clientUser_cannotAccessOtherTenantLead() throws Exception {
        // Create a lead in a different tenant
        Tenant otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-tenant").isActive(true).build());

        User otherClient = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash(passwordEncoder.encode("test1234"))
                .name("Other Client")
                .role(Role.CLIENT)
                .tenantId(otherTenant.getId())
                .isActive(true)
                .isBlocked(false)
                .build());
        String otherToken = jwtProvider.generateAccessToken(otherClient.getId(), otherClient.getEmail(), otherClient.getRole(), otherClient.getTenantId());

        mockMvc.perform(get("/api/v1/leads/{id}", existingLead.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}
