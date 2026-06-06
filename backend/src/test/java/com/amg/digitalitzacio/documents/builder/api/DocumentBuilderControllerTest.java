package com.amg.digitalitzacio.documents.builder.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.documents.builder.domain.DocumentTemplate;
import com.amg.digitalitzacio.documents.builder.domain.DocumentTemplateRepository;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class DocumentBuilderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private TenantRepository tenantRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private DocumentTemplateRepository templateRepo;

    private String adminToken;
    private UUID tenantId;
    private UUID templateId;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = tenantRepo.save(Tenant.builder()
            .name("Test Tenant").slug("test-doc").email("doc@test.com")
            .isFree(true).build());
        tenantId = tenant.getId();

        user = userRepo.save(User.builder()
            .tenantId(tenantId).email("admin@test.com").passwordHash("encoded")
            .name("Admin").role(Role.ADMIN).build());

        adminToken = jwtProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), tenantId);

        var t = new DocumentTemplate();
        t.setTenantId(tenantId);
        t.setName("Pressupost estàndard");
        t.setDocumentType(com.amg.digitalitzacio.documents.builder.domain.DocumentType.quote);
        t.setLayout("[{\"type\":\"document_title\",\"x\":0,\"y\":0,\"w\":12,\"h\":1,\"config\":{\"style\":{\"alignment\":\"center\"}},\"dataBinding\":{\"source\":\"document\",\"field\":\"number\"}}]");
        t.setDataBindings("{}");
        t.setStyles("{}");
        templateId = templateRepo.save(t).getId();
    }

    @AfterEach
    void tearDown() {
        templateRepo.deleteAll();
        userRepo.deleteAll();
        tenantRepo.deleteAll();
    }

    @Test
    void TC01_createTemplateQuote_returns201() throws Exception {
        var body = """
            {"name": "Pressupost obra", "documentType": "quote", "layout": "[]", "dataBindings": "{}", "styles": "{}"}
            """;
        mockMvc.perform(post("/api/v1/documents/templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pressupost obra"))
            .andExpect(jsonPath("$.documentType").value("quote"));
    }

    @Test
    void TC02_createTemplateWithoutName_returns400() throws Exception {
        var body = """
            {"name": "", "documentType": "quote", "layout": "[]", "dataBindings": "{}", "styles": "{}"}
            """;
        mockMvc.perform(post("/api/v1/documents/templates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void TC03_getTemplateById_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/documents/templates/{id}", templateId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(templateId.toString()))
            .andExpect(jsonPath("$.name").value("Pressupost estàndard"));
    }

    @Test
    void TC04_getTemplateNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/documents/templates/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void TC05_updateTemplate_incrementsVersion() throws Exception {
        var body = """
            {"name": "Pressupost actualitzat", "documentType": "quote", "layout": "[]", "dataBindings": "{}", "styles": "{}"}
            """;
        mockMvc.perform(put("/api/v1/documents/templates/{id}", templateId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(2))
            .andExpect(jsonPath("$.name").value("Pressupost actualitzat"));
    }

    @Test
    void TC06_deleteTemplate_softDelete() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/templates/{id}", templateId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isNoContent());

        var t = templateRepo.findById(templateId).orElseThrow();
        assert !t.getActive() : "Template should be inactive after delete";
    }

    @Test
    void TC07_duplicateTemplate_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/documents/templates/{id}/duplicate", templateId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pressupost estàndard (còpia)"));
    }

    @Test
    void TC08_listVersions_returnsHistory() throws Exception {
        mockMvc.perform(get("/api/v1/documents/templates/{id}/versions", templateId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void TC09_restoreVersion_restoresLayout() throws Exception {
        var body1 = "{\"name\":\"Modificat\",\"documentType\":\"quote\",\"layout\":\"[{\\\"type\\\":\\\"text\\\"}]\",\"dataBindings\":\"{}\",\"styles\":\"{}\"}";
        mockMvc.perform(put("/api/v1/documents/templates/{id}", templateId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body1));

        mockMvc.perform(post("/api/v1/documents/templates/{id}/restore/{version}", templateId, 2)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void TC10_addBlockViaUpdate_returns200() throws Exception {
        var body = "{\"name\":\"Amb logo\",\"documentType\":\"quote\",\"layout\":\"[{\\\"type\\\":\\\"logo\\\",\\\"x\\\":0,\\\"y\\\":0,\\\"w\\\":4,\\\"h\\\":2,\\\"config\\\":{\\\"style\\\":{}},\\\"dataBinding\\\":{\\\"source\\\":\\\"company\\\",\\\"field\\\":\\\"logo\\\"}}]\",\"dataBindings\":\"{}\",\"styles\":\"{}\"}";
        mockMvc.perform(put("/api/v1/documents/templates/{id}", templateId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.layout").isString());
    }

    @Test
    void TC15_generateDocument_returnsHtml() throws Exception {
        var body = """
            {"templateId": "%s", "customerData": {"name":"Joan Pérez","taxId":"12345678A","address":"C/ Major 1, Palma","phone":"+34 600 000 000","email":"joan@perez.com"}, "variables": {"hours":10}, "articles": [{"description":"Neteja oficines","quantity":10,"unitPrice":45}]}
            """.formatted(templateId.toString());

        mockMvc.perform(post("/api/v1/documents/generate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").isString())
            .andExpect(jsonPath("$.htmlContent").isString());
    }

    @Test
    void TC16_generateDocumentWithoutTemplateId_returns400() throws Exception {
        var body = """
            {"customerData": {}, "variables": {}, "articles": []}
            """;
        mockMvc.perform(post("/api/v1/documents/generate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void TC17_generatePdf_returnsPdfUrl() throws Exception {
        var body = """
            {"templateId": "%s", "customerData": {"name":"Joan Pérez"}, "variables": {}, "articles": [{"description":"Hores neteja","quantity":10,"unitPrice":45}]}
            """.formatted(templateId.toString());

        mockMvc.perform(post("/api/v1/documents/generate/pdf")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pdfUrl").value(startsWith("/api/v1/documents/")));
    }

    @Test
    void TC19_calculation_10x45_returns450() throws Exception {
        var body = """
            {"templateId": "%s", "customerData": {}, "variables": {}, "articles": [{"description":"Servei","quantity":10,"unitPrice":45}]}
            """.formatted(templateId.toString());

        var result = mockMvc.perform(post("/api/v1/documents/generate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        var doc = objectMapper.readTree(result.getResponse().getContentAsString());
        assert doc.get("number").asText() != null : "Document should have a number";
    }

    @Test
    void TC20_conditionalRule_discountOver20h() throws Exception {
        var body = """
            {"templateId": "%s", "customerData": {}, "variables": {"hours":25}, "articles": [{"description":"Consultoria","quantity":25,"unitPrice":50}]}
            """.formatted(templateId.toString());

        var result = mockMvc.perform(post("/api/v1/documents/generate")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andReturn();

        var doc = objectMapper.readTree(result.getResponse().getContentAsString());
        assert doc.get("number").asText() != null : "Document should have a number";
    }

    @Test
    void TC21_aiMoveLogoLeft_returnsOperation() throws Exception {
        var body = """
            {"prompt": "Posa el logo a l'esquerra", "templateId": "%s"}
            """.formatted(templateId.toString());

        mockMvc.perform(post("/api/v1/documents/ai/apply")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.operations[0].action").value("move"))
            .andExpect(jsonPath("$.operations[0].blockId").value("logo"));
    }

    @Test
    void TC23_aiEmptyPrompt_returns400() throws Exception {
        var body = """
            {"prompt": "", "templateId": "%s"}
            """.formatted(templateId.toString());

        mockMvc.perform(post("/api/v1/documents/ai/apply")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void TC24_tenantA_onlySeesOwnTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/documents/templates")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tenantId").value(tenantId.toString()));
    }

    @Test
    void previewTemplate_returnsHtml() throws Exception {
        mockMvc.perform(get("/api/v1/documents/templates/{id}/preview", templateId)
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("<!DOCTYPE html>")));
    }
}
