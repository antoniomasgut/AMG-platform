package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.amg.digitalitzacio.vault.domain.*;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class BillingControllerTest {

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
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ServiceProfileRepository profileRepository;
    @Autowired
    private PhaseRepository phaseRepository;
    @Autowired
    private ServiceRepository serviceRepository;
    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private BudgetLineRepository budgetLineRepository;
    @Autowired
    private DiscountRepository discountRepository;

    private Tenant tenant;
    private Tenant otherTenant;
    private User superAdmin;
    private User adminUser;
    private User clientUser;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;

    private ServiceProfile profile;
    private Phase phase1;
    private CatalogService service1;
    private CatalogService service2;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());
        otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-tenant").isActive(true).build());

        superAdmin = userRepository.save(User.builder()
                .email("super@test.com").passwordHash(passwordEncoder.encode("pass"))
                .name("Super").role(Role.SUPER_ADMIN).isActive(true).isBlocked(false).failedAttempts(0).build());
        adminUser = userRepository.save(User.builder()
                .email("admin@test.com").passwordHash(passwordEncoder.encode("pass"))
                .name("Admin").role(Role.ADMIN).isActive(true).isBlocked(false).failedAttempts(0).build());
        clientUser = userRepository.save(User.builder()
                .email("client@test.com").passwordHash(passwordEncoder.encode("pass"))
                .name("Client").role(Role.CLIENT).tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0).build());

        superAdminToken = jwtProvider.generateAccessToken(superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(adminUser.getId(), adminUser.getEmail(), adminUser.getRole(), adminUser.getTenantId());
        clientToken = jwtProvider.generateAccessToken(clientUser.getId(), clientUser.getEmail(), clientUser.getRole(), clientUser.getTenantId());

        // Crear perfil amb fases i serveis
        profile = profileRepository.save(ServiceProfile.builder()
                .name("Test Profile").slug("test-profile").isActive(true).build());

        phase1 = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());

        service1 = serviceRepository.save(CatalogService.builder()
                .phaseId(phase1.getId()).name("WhatsApp").slug("whatsapp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.valueOf(10)).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());
        service2 = serviceRepository.save(CatalogService.builder()
                .phaseId(phase1.getId()).name("SMTP").slug("smtp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.valueOf(5)).salePrice(BigDecimal.valueOf(30)).sortOrder(2).build());
    }

    /* ─────────── TC-01: Crear pressupost DRAFT ─────────── */
    @Test
    void tc01_createBudget_Returns201() throws Exception {
        var json = """
                {"profileId": "%s"}
                """.formatted(profile.getId());

        mockMvc.perform(post("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.total").value(80.0))
                .andExpect(jsonPath("$.lines.length()").value(2));
    }

    /* ─────────── TC-02: Llistar pressupostos ─────────── */
    @Test
    void tc02_listBudgets_Returns200() throws Exception {
        createTestBudget();

        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgets.length()").value(1));
    }

    /* ─────────── TC-03: Veure pressupost ─────────── */
    @Test
    void tc03_getBudget_Returns200() throws Exception {
        var budgetId = createTestBudget();

        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/budgets/{id}", tenant.getId(), budgetId)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgetNumber").value("BUD-2026-0001"));
    }

    /* ─────────── TC-04: Enviar pressupost ─────────── */
    @Test
    void tc04_sendBudget_Returns200() throws Exception {
        var budgetId = createTestBudget();

        mockMvc.perform(post("/api/v1/billing/budgets/{id}/send", budgetId)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.sentAt").isNotEmpty());
    }

    /* ─────────── TC-05: Acceptar pressupost via token ─────────── */
    @Test
    void tc05_acceptBudget_Returns200() throws Exception {
        var budgetId = createTestBudget();
        sendBudget(budgetId);

        // Obtenir el token
        var budget = budgetRepository.findById(budgetId).orElseThrow();

        mockMvc.perform(post("/api/v1/billing/budgets/accept?token={token}", budget.getAcceptanceToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    /* ─────────── TC-06: Rebutjar pressupost via token ─────────── */
    @Test
    void tc06_rejectBudget_Returns200() throws Exception {
        var budgetId = createTestBudget();
        sendBudget(budgetId);

        var budget = budgetRepository.findById(budgetId).orElseThrow();

        mockMvc.perform(post("/api/v1/billing/budgets/reject?token={token}", budget.getAcceptanceToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Preu massa elevat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    /* ─────────── TC-07: Token d'un sol ús ─────────── */
    @Test
    void tc07_tokenAlreadyUsed_Returns400() throws Exception {
        var budgetId = createTestBudget();
        sendBudget(budgetId);
        var budget = budgetRepository.findById(budgetId).orElseThrow();

        // Primer accepta
        mockMvc.perform(post("/api/v1/billing/budgets/accept?token={token}", budget.getAcceptanceToken()));

        // Segon intent ha de fallar
        mockMvc.perform(post("/api/v1/billing/budgets/accept?token={token}", budget.getAcceptanceToken())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-08: CLIENT llista pressupostos ─────────── */
    @Test
    void tc08_clientListsBudgets_Returns200() throws Exception {
        createTestBudget();

        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.budgets.length()").value(1));
    }

    /* ─────────── TC-09: CLIENT no pot crear pressupost ─────────── */
    @Test
    void tc09_clientCannotCreateBudget_Returns403() throws Exception {
        var json = """
                {"profileId": "%s"}
                """.formatted(profile.getId());

        mockMvc.perform(post("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-10: ADMIN crea pressupost ─────────── */
    @Test
    void tc10_adminCreatesBudget_Returns201() throws Exception {
        var json = """
                {"profileId": "%s"}
                """.formatted(profile.getId());

        mockMvc.perform(post("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    /* ─────────── TC-11: Crear descompte PERCENTAGE ─────────── */
    @Test
    void tc11_createPercentageDiscount_Returns201() throws Exception {
        var json = """
                {"tenantId": "%s", "type": "PERCENTAGE", "value": 10, "appliesTo": "BUDGET", "label": "Test 10 percent"}
                """.formatted(tenant.getId());

        mockMvc.perform(post("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PERCENTAGE"));
    }

    /* ─────────── TC-12: Crear descompte FIXED ─────────── */
    @Test
    void tc12_createFixedDiscount_Returns201() throws Exception {
        var json = """
                {"tenantId": "%s", "type": "FIXED", "value": 25, "appliesTo": "BUDGET", "label": "Test 25€"}
                """.formatted(tenant.getId());

        mockMvc.perform(post("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.value").value(25));
    }

    /* ─────────── TC-13: Dashboard ─────────── */
    @Test
    void tc13_dashboard_Returns200() throws Exception {
        createTestBudget();

        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/dashboard", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBudgets").value(1));
    }

    /* ─────────── TC-14: Accés sense JWT ─────────── */
    @Test
    void tc14_noJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/budgets", tenant.getId()))
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── TC-15: CLIENT no veu pressupostos d'altre tenant ─────────── */
    @Test
    void tc15_clientCannotSeeOtherTenant_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/billing/tenants/{tid}/budgets", otherTenant.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-16: Acceptar amb token invàlid ─────────── */
    @Test
    void tc16_invalidToken_Returns400() throws Exception {
        mockMvc.perform(post("/api/v1/billing/budgets/accept?token={token}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-17: ADMIN intenta eliminar pressupost (403) ─────────── */
    @Test
    void tc17_adminCannotDeleteBudget_Returns403() throws Exception {
        var budgetId = createTestBudget();

        mockMvc.perform(delete("/api/v1/billing/budgets/{id}", budgetId)
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-18: SUPER_ADMIN cancel·la pressupost ─────────── */
    @Test
    void tc18_superAdminCancelsBudget_Returns204() throws Exception {
        var budgetId = createTestBudget();

        mockMvc.perform(delete("/api/v1/billing/budgets/{id}", budgetId)
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());
    }

    /* ─────────── TC-19: Pressupost amb descompte ─────────── */
    @Test
    void tc19_budgetWithDiscount_ReturnsCorrectTotal() throws Exception {
        // Crear descompte del 10%
        var discount = discountRepository.save(Discount.builder()
                .tenantId(tenant.getId())
                .type(com.amg.digitalitzacio.billing.domain.DiscountType.PERCENTAGE)
                .value(BigDecimal.TEN)
                .appliesTo(AppliesTo.BUDGET)
                .label("Test 10%")
                .isActive(true)
                .appliedCount(0)
                .createdBy(superAdmin.getId())
                .build());

        var json = """
                {"profileId": "%s"}
                """.formatted(profile.getId());

        mockMvc.perform(post("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(80.0))
                .andExpect(jsonPath("$.discountTotal").value(8.0))
                .andExpect(jsonPath("$.total").value(72.0));
    }

    // ── Helpers ──

    private UUID createTestBudget() throws Exception {
        var json = """
                {"profileId": "%s"}
                """.formatted(profile.getId());

        var result = mockMvc.perform(post("/api/v1/billing/tenants/{tid}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andReturn();

        var content = result.getResponse().getContentAsString();
        return objectMapper.readTree(content).get("id").asText().isEmpty()
                ? null
                : UUID.fromString(objectMapper.readTree(content).get("id").asText());
    }

    private void sendBudget(UUID budgetId) throws Exception {
        mockMvc.perform(post("/api/v1/billing/budgets/{id}/send", budgetId)
                .header("Authorization", "Bearer " + superAdminToken));
    }
}
