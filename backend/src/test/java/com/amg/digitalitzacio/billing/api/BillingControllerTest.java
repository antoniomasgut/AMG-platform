package com.amg.digitalitzacio.billing.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.billing.api.dto.*;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.amg.digitalitzacio.vault.domain.CatalogService;
import com.amg.digitalitzacio.vault.domain.CatalogServiceRepository;
import com.amg.digitalitzacio.vault.domain.Phase;
import com.amg.digitalitzacio.vault.domain.PhaseRepository;
import com.amg.digitalitzacio.vault.domain.ServiceProfile;
import com.amg.digitalitzacio.vault.domain.ServiceProfileRepository;
import com.amg.digitalitzacio.vault.domain.ServiceType;
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
import java.time.LocalDate;
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
class BillingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private BudgetLineRepository budgetLineRepository;
    @Autowired private DiscountRepository discountRepository;
    @Autowired private ServiceProfileRepository serviceProfileRepository;
    @Autowired private PhaseRepository phaseRepository;
    @Autowired private CatalogServiceRepository catalogServiceRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        budgetRepository.deleteAll();
        budgetLineRepository.deleteAll();
        discountRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());

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

    /* ── TC-01: Crear descompte ── */
    @Test
    void tc01_crearDescompte_Returns201() throws Exception {
        var request = new CreateDiscountRequest(tenant.getId(), "PERCENTAGE", BigDecimal.TEN,
                "BUDGET", null, "Test 10%", LocalDate.now(), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.label").value("Test 10%"))
                .andExpect(jsonPath("$.type").value("PERCENTAGE"))
                .andExpect(jsonPath("$.value").value(10));
    }

    /* ── TC-02: Llistar descomptes ── */
    @Test
    void tc02_llistarDescomptes_Returns200() throws Exception {
        discountRepository.save(Discount.builder()
                .tenantId(tenant.getId()).type(DiscountType.PERCENTAGE).value(BigDecimal.TEN)
                .appliesTo(DiscountAppliesTo.BUDGET).label("10% OFF").createdBy(UUID.randomUUID())
                .validFrom(LocalDate.now()).validUntil(LocalDate.now().plusMonths(1)).isActive(true)
                .build());
        discountRepository.save(Discount.builder()
                .tenantId(tenant.getId()).type(DiscountType.FIXED).value(BigDecimal.valueOf(50))
                .appliesTo(DiscountAppliesTo.BUDGET).label("50€ OFF").createdBy(UUID.randomUUID())
                .validFrom(LocalDate.now()).validUntil(LocalDate.now().plusMonths(1)).isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + superAdminToken)
                .param("tenantId", tenant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /* ── TC-03: ADMIN pot crear descomptes ── */
    @Test
    void tc03_adminPotCrearDescompte_Returns201() throws Exception {
        var request = new CreateDiscountRequest(tenant.getId(), "FIXED", BigDecimal.valueOf(25),
                "BUDGET", null, "25€ OFF", LocalDate.now(), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    /* ── TC-04: CLIENT no pot crear descomptes → 403 ── */
    @Test
    void tc04_clientNoPotCrearDescompte_Returns403() throws Exception {
        var request = new CreateDiscountRequest(tenant.getId(), "PERCENTAGE", BigDecimal.TEN,
                "BUDGET", null, "Test", LocalDate.now(), LocalDate.now().plusMonths(1));

        mockMvc.perform(post("/api/v1/billing/discounts")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-05: Actualitzar descompte ── */
    @Test
    void tc05_actualitzarDescompte_Returns200() throws Exception {
        var discount = discountRepository.save(Discount.builder()
                .tenantId(tenant.getId()).type(DiscountType.PERCENTAGE).value(BigDecimal.TEN)
                .appliesTo(DiscountAppliesTo.BUDGET).label("Original").isActive(true).createdBy(UUID.randomUUID())
                .validFrom(LocalDate.now()).validUntil(LocalDate.now().plusMonths(1)).build());

        var request = new UpdateDiscountRequest("FIXED", BigDecimal.valueOf(100), "BUDGET", null,
                "Updated", true, LocalDate.now(), LocalDate.now().plusMonths(2));

        mockMvc.perform(put("/api/v1/billing/discounts/{id}", discount.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.label").value("Updated"))
                .andExpect(jsonPath("$.value").value(100));
    }

    /* ── TC-06: Eliminar (desactivar) descompte ── */
    @Test
    void tc06_eliminarDescompte_Returns204() throws Exception {
        var discount = discountRepository.save(Discount.builder()
                .tenantId(tenant.getId()).type(DiscountType.PERCENTAGE).value(BigDecimal.TEN)
                .appliesTo(DiscountAppliesTo.BUDGET).label("To Delete").isActive(true).createdBy(UUID.randomUUID())
                .validFrom(LocalDate.now()).validUntil(LocalDate.now().plusMonths(1)).build());

        mockMvc.perform(delete("/api/v1/billing/discounts/{id}", discount.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        var deleted = discountRepository.findById(discount.getId()).orElseThrow();
        assert !deleted.getIsActive() : "El descompte hauria d'estar desactivat";
    }

    /* ── TC-07: ADMIN no pot eliminar descomptes → 403 ── */
    @Test
    void tc07_adminNoPotEliminarDescompte_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/billing/discounts/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    /* ── TC-08: Crear pressupost requereix SUPER_ADMIN/ADMIN ── */
    @Test
    void tc08_crearPressupost_Returns201() throws Exception {
        var profile = serviceProfileRepository.save(ServiceProfile.builder()
                .name("Pla Test").slug("pla-test-billing").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        catalogServiceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("WhatsApp").slug("whatsapp-billing")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.TEN).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());

        var request = new CreateBudgetRequest(profile.getId(), List.of(), List.of(),
                "Notes", null, List.of(), LocalDate.now().plusDays(30), null, null, null, null, null);

        mockMvc.perform(post("/api/v1/billing/tenants/{tenantId}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetNumber").isString())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    /* ── TC-09: CLIENT no pot crear pressupostos → 403 ── */
    @Test
    void tc09_clientNoPotCrearPressupost_Returns403() throws Exception {
        var request = new CreateBudgetRequest(UUID.randomUUID(), List.of(), List.of(),
                null, null, List.of(), LocalDate.now().plusDays(30), null, null, null, null, null);

        mockMvc.perform(post("/api/v1/billing/tenants/{tenantId}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-10: Llistar pressupostos ── */
    @Test
    void tc10_llistarPressupostos_Returns200() throws Exception {
        budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId()).budgetNumber("BUD-2026-0001")
                .status(BudgetStatus.DRAFT)
                .subtotal(BigDecimal.valueOf(100)).discountTotal(BigDecimal.ZERO).total(BigDecimal.valueOf(100))
                .validUntil(LocalDate.now().plusDays(30)).build());

        mockMvc.perform(get("/api/v1/billing/tenants/{tenantId}/budgets", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    /* ── TC-11: Cancel·lar pressupost — DRAFT → CANCELLED ── */
    @Test
    void tc11_cancelarPressupost_Returns204() throws Exception {
        var budget = budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId()).budgetNumber("BUD-2026-0002")
                .status(BudgetStatus.DRAFT)
                .subtotal(BigDecimal.valueOf(100)).discountTotal(BigDecimal.ZERO).total(BigDecimal.valueOf(100))
                .validUntil(LocalDate.now().plusDays(30)).build());

        mockMvc.perform(delete("/api/v1/billing/budgets/{id}", budget.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        var cancelled = budgetRepository.findById(budget.getId()).orElseThrow();
        assert cancelled.getStatus() == BudgetStatus.CANCELLED : "Hauria d'estar cancel·lat";
    }

    /* ── TC-12: Accés sense JWT → 401 ── */
    @Test
    void tc12_accesSenseJWT_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/billing/discounts"))
                .andExpect(status().isUnauthorized());
    }

    /* ── TC-13: Dashboard ── */
    @Test
    void tc13_dashboard_Returns200() throws Exception {
        budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId()).budgetNumber("BUD-2026-0003")
                .status(BudgetStatus.SENT).subtotal(BigDecimal.valueOf(200))
                .discountTotal(BigDecimal.ZERO).total(BigDecimal.valueOf(200))
                .validUntil(LocalDate.now().plusDays(30)).build());

        mockMvc.perform(get("/api/v1/billing/tenants/{tenantId}/dashboard", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingBudgets").value(1));
    }

    /* ── TC-14: CLIENT no pot eliminar pressupost → 403 ── */
    @Test
    void tc14_clientNoPotCancelarPressupost_Returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/billing/budgets/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
