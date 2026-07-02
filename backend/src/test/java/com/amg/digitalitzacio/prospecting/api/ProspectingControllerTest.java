package com.amg.digitalitzacio.prospecting.api;

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
class ProspectingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;

    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        var tenant = tenantRepository.save(Tenant.builder()
                .name("Prospecting Tenant").slug("prospecting-tenant").isActive(true).build());

        var admin = userRepository.save(User.builder()
                .email("admin@prospecting.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@prospecting.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Client User").role(Role.CLIENT)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
        clientToken = jwtProvider.generateAccessToken(
                client.getId(), client.getEmail(), client.getRole(), client.getTenantId());
    }

    @Test
    void tc01_createCampaign_returns201() throws Exception {
        var request = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Restaurants Palma", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Restaurants Palma"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void tc02_listCampaignsEmpty_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void tc03_runCampaign_returns202() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Restaurants Palma", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void tc04_listProspects_returns200() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Test Campaign", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        mockMvc.perform(get("/api/v1/prospecting/campaigns/{id}/prospects", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void tc05_updateProspectStatus_returns200() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Test", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        var prospectsJson = mockMvc.perform(get("/api/v1/prospecting/campaigns/{id}/prospects", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var prospectId = objectMapper.readTree(prospectsJson).get(0).get("id").asText();

        var updateReq = new com.amg.digitalitzacio.prospecting.api.dto.UpdateProspectRequest(
                com.amg.digitalitzacio.prospecting.domain.ProspectStatus.CONTACTED, "Called client",
                null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/prospecting/prospects/{id}", prospectId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONTACTED"))
                .andExpect(jsonPath("$.notes").value("Called client"));
    }

    @Test
    void tc06_enrichProspect_returns200() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Test", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        var prospectsJson = mockMvc.perform(get("/api/v1/prospecting/campaigns/{id}/prospects", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var prospectId = objectMapper.readTree(prospectsJson).get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/prospects/{id}/enrich", prospectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").isString())
                .andExpect(jsonPath("$.hasWebsite").isBoolean());
    }

    @Test
    void tc07_exportProspect_returns201() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Export Test", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        var prospectsJson = mockMvc.perform(get("/api/v1/prospecting/campaigns/{id}/prospects", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var prospectId = objectMapper.readTree(prospectsJson).get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/prospects/{id}/export", prospectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXPORTED"))
                .andExpect(jsonPath("$.leadId").isNotEmpty());
    }

    @Test
    void tc08_exportAllProspects_returns200() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Export All", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/export-all", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void tc09_clientCannotAccessProspecting_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc10_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/prospecting/campaigns"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc11_deleteCampaign_returns204() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Delete Me", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(delete("/api/v1/prospecting/campaigns/{id}", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void tc12_exportAlreadyExportedProspect_returns409() throws Exception {
        var createReq = new com.amg.digitalitzacio.prospecting.api.dto.CreateCampaignRequest(
                "Conflict Test", "restaurant", "Palma",
                com.amg.digitalitzacio.prospecting.domain.ProspectSource.GOOGLE_MAPS, null, null, null, null, null, null);

        var createJson = mockMvc.perform(post("/api/v1/prospecting/campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var campaignId = objectMapper.readTree(createJson).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/campaigns/{id}/run", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isAccepted());

        var prospectsJson = mockMvc.perform(get("/api/v1/prospecting/campaigns/{id}/prospects", campaignId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var prospectId = objectMapper.readTree(prospectsJson).get(0).get("id").asText();

        mockMvc.perform(post("/api/v1/prospecting/prospects/{id}/export", prospectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/prospecting/prospects/{id}/export", prospectId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
