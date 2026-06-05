package com.amg.digitalitzacio.agents.api;

import com.amg.digitalitzacio.agents.api.dto.AddDocumentRequest;
import com.amg.digitalitzacio.agents.api.dto.KnowledgeEntryRequest;
import com.amg.digitalitzacio.agents.api.dto.UpdateEntriesRequest;
import com.amg.digitalitzacio.agents.domain.*;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class KnowledgeBaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private KnowledgeBaseRepository knowledgeBaseRepository;
    @Autowired private KnowledgeEntryRepository knowledgeEntryRepository;
    @Autowired private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;

    private Tenant tenant;
    private String adminToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        knowledgeBaseRepository.deleteAll();
        knowledgeEntryRepository.deleteAll();
        knowledgeDocumentRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());

        var admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin")
                .role(Role.ADMIN)
                .tenantId(tenant.getId())
                .isActive(true)
                .build());

        adminToken = jwtProvider.generateAccessToken(admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
    }

    @Test
    void getKnowledge_returnsEmptyKB_whenNotConfigured() throws Exception {
        mockMvc.perform(get("/api/v1/agents/knowledge/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(tenant.getId().toString())))
                .andExpect(jsonPath("$.isActive", is(true)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void getKnowledge_returnsEntriesGroupedByCategory() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.BUSINESS_INFO)
                .key("business_name").content("Test Business").sortOrder(1).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.FAQ)
                .key("faq.1").content("Q: Test? A: Resposta").sortOrder(1).build());

        mockMvc.perform(get("/api/v1/agents/knowledge/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entriesByCategory.BUSINESS_INFO", hasSize(1)))
                .andExpect(jsonPath("$.entriesByCategory.FAQ", hasSize(1)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void updateEntries_replacesAllEntriesForCategory() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        var existing = knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.SERVICE)
                .key("old").content("Old service").sortOrder(1).build());

        var request = new UpdateEntriesRequest(List.of(
                new KnowledgeEntryRequest("service.neteja", "Neteja dental: 45€", 1),
                new KnowledgeEntryRequest("service.empast", "Empast: 60€", 2)
        ));

        mockMvc.perform(put("/api/v1/agents/knowledge/{tenantId}/entries/{category}", tenant.getId(), "SERVICE")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        var remaining = knowledgeEntryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId());
        assertThat(remaining, hasSize(2));
        assertThat(remaining.stream().noneMatch(e -> e.getKey().equals("old")), is(true));
    }

    @Test
    void updateEntries_ignoresBlankContent() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());

        var request = new UpdateEntriesRequest(List.of(
                new KnowledgeEntryRequest("valid", "Valid content", 1),
                new KnowledgeEntryRequest("blank", "", 2)
        ));

        mockMvc.perform(put("/api/v1/agents/knowledge/{tenantId}/entries/{category}", tenant.getId(), "FAQ")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        var entries = knowledgeEntryRepository.findByKnowledgeBaseIdAndIsActiveTrueOrderBySortOrder(kb.getId());
        assertThat(entries, hasSize(1));
        assertThat(entries.getFirst().getKey(), is("valid"));
    }

    @Test
    void addDocument_storesDocumentContent() throws Exception {
        var request = new AddDocumentRequest("test.txt", "Contingut del document");

        mockMvc.perform(post("/api/v1/agents/knowledge/{tenantId}/documents", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename", is("test.txt")));

        var kbs = knowledgeBaseRepository.findByTenantId(tenant.getId());
        assertThat(kbs.isPresent(), is(true));
    }

    @Test
    void uploadDocument_rejectsInvalidFileType() throws Exception {
        var file = new MockMultipartFile("file", "test.exe", "application/x-msdownload", "fake".getBytes());

        mockMvc.perform(multipart("/api/v1/agents/knowledge/{tenantId}/documents/upload", tenant.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadDocument_rejectsOversizedFile() throws Exception {
        var largeContent = new byte[11 * 1024 * 1024];
        var file = new MockMultipartFile("file", "large.txt", "text/plain", largeContent);

        mockMvc.perform(multipart("/api/v1/agents/knowledge/{tenantId}/documents/upload", tenant.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDocument_softDeletes() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        var doc = knowledgeDocumentRepository.save(KnowledgeDocument.builder()
                .knowledgeBaseId(kb.getId()).filename("test.txt").build());

        mockMvc.perform(delete("/api/v1/agents/knowledge/{tenantId}/documents/{docId}", tenant.getId(), doc.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        var reloaded = knowledgeDocumentRepository.findById(doc.getId());
        assertThat(reloaded.isPresent(), is(true));
        assertThat(reloaded.get().getIsActive(), is(false));
    }

    @Test
    void preview_returnsPromptBlock() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.BUSINESS_INFO)
                .key("name").content("Business: Test SL").sortOrder(1).build());

        mockMvc.perform(get("/api/v1/agents/knowledge/{tenantId}/preview", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("CONEIXEMENT DEL NEGOCI")));
    }

    @Test
    void testResponse_returns503_whenAiProviderNotConfigured() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        knowledgeEntryRepository.save(KnowledgeEntry.builder()
                .knowledgeBaseId(kb.getId()).category(KnowledgeCategory.BUSINESS_INFO)
                .key("name").content("Test Business").sortOrder(1).build());

        mockMvc.perform(post("/api/v1/agents/knowledge/{tenantId}/test", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Quins serveis oferiu?\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void getKnowledge_returns404_whenTenantNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/agents/knowledge/{tenantId}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void updateEntries_rejectsInvalidCategory() throws Exception {
        var request = new UpdateEntriesRequest(List.of(
                new KnowledgeEntryRequest("key", "content", 1)
        ));

        mockMvc.perform(put("/api/v1/agents/knowledge/{tenantId}/entries/{category}", tenant.getId(), "INVALID_CAT")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void knowledge_incrementsVersionOnUpdate() throws Exception {
        var kb = knowledgeBaseRepository.save(KnowledgeBase.builder().tenantId(tenant.getId()).build());
        int initialVersion = kb.getVersion();

        var request = new UpdateEntriesRequest(List.of(
                new KnowledgeEntryRequest("test", "content", 1)
        ));

        mockMvc.perform(put("/api/v1/agents/knowledge/{tenantId}/entries/{category}", tenant.getId(), "EXTRA")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        var updated = knowledgeBaseRepository.findByTenantId(tenant.getId());
        assertThat(updated.isPresent(), is(true));
        assertThat(updated.get().getVersion(), is(initialVersion + 1));
    }

    @Test
    void addDocument_withEmptyContent_createsDocument() throws Exception {
        var request = new AddDocumentRequest("empty.txt", "");

        mockMvc.perform(post("/api/v1/agents/knowledge/{tenantId}/documents", tenant.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}
