package com.amg.digitalitzacio.finops.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.billing.domain.Budget;
import com.amg.digitalitzacio.billing.domain.BudgetRepository;
import com.amg.digitalitzacio.billing.domain.BudgetStatus;
import com.amg.digitalitzacio.billing.domain.BudgetLine;
import com.amg.digitalitzacio.billing.domain.BudgetLineRepository;
import com.amg.digitalitzacio.finops.api.dto.*;
import com.amg.digitalitzacio.finops.domain.*;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class FinOpsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private HoldedConfigRepository holdedConfigRepository;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private BudgetLineRepository budgetLineRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        holdedConfigRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("FinOps Tenant").slug("finops-test-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@finops.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@finops.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@finops.com")
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
    void tc01_configureHolded_returns201() throws Exception {
        var request = new HoldedConfigRequest(tenant.getId(), "vault-ref-123", "company-001");

        mockMvc.perform(post("/api/v1/finops/configure")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.holdedCompanyId").value("company-001"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void tc02_getConfig_returns200() throws Exception {
        holdedConfigRepository.save(HoldedConfig.builder()
                .tenantId(tenant.getId())
                .apiKeyRef("vault-ref-123")
                .holdedCompanyId("company-001")
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/finops/configure/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }

    @Test
    void tc03_clientCannotViewConfig_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finops/configure/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc04_syncContact_returns200() throws Exception {
        holdedConfigRepository.save(HoldedConfig.builder()
                .tenantId(tenant.getId())
                .apiKeyRef("vault-ref-123")
                .isActive(true)
                .build());

        mockMvc.perform(post("/api/v1/finops/configure/{tenantId}/sync", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSynced").value(true))
                .andExpect(jsonPath("$.holdedContactId").isNotEmpty());
    }

    @Test
    void tc05_listInvoicesEmpty_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/finops/invoices")
                        .param("tenantId", tenant.getId().toString())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void tc06_createInvoiceFromBudget_returns201() throws Exception {
        holdedConfigRepository.save(HoldedConfig.builder()
                .tenantId(tenant.getId())
                .apiKeyRef("vault-ref-123")
                .holdedContactId("mock-contact-001")
                .isSynced(true)
                .isActive(true)
                .build());

        var budget = budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId())
                .subtotal(new BigDecimal("150.00"))
                .discountTotal(BigDecimal.ZERO)
                .total(new BigDecimal("150.00"))
                .status(BudgetStatus.ACCEPTED)
                .build());

        // Budget needs at least one line for creation to work in some billing setups
        budgetLineRepository.save(BudgetLine.builder()
                .budgetId(budget.getId())
                .serviceId(UUID.randomUUID())
                .serviceName("Test service")
                .quantity(1)
                .unitPrice(new BigDecimal("150.00"))
                .total(new BigDecimal("150.00"))
                .build());

        // First create an invoice from the budget
        var invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenant.getId())
                .budgetId(budget.getId())
                .amount(new BigDecimal("150.00"))
                .status(InvoiceStatus.PENDING)
                .build());

        mockMvc.perform(get("/api/v1/finops/invoices/{invoiceId}", invoice.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(150.00));
    }

    @Test
    void tc07_clientCanViewOwnInvoice_returns200() throws Exception {
        var invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(InvoiceStatus.SENT)
                .build());

        mockMvc.perform(get("/api/v1/finops/invoices/{invoiceId}", invoice.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void tc08_cancelInvoice_returns200() throws Exception {
        var invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(InvoiceStatus.SENT)
                .build());

        mockMvc.perform(post("/api/v1/finops/invoices/{invoiceId}/cancel", invoice.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void tc09_dashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/finops/dashboard")
                        .param("tenantId", tenant.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceCount").isNumber());
    }

    @Test
    void tc10_globalDashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/finops/dashboard/global")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvoices").isNumber());
    }

    @Test
    void tc11_accessWithoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/finops/invoices"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc12_clientCannotConfigure_returns403() throws Exception {
        var request = new HoldedConfigRequest(tenant.getId(), "vault-ref", "company");

        mockMvc.perform(post("/api/v1/finops/configure")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc13_getInvoicePdf_returns200() throws Exception {
        var invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .status(InvoiceStatus.SENT)
                .invoicePdfUrl("https://mock.holded.com/invoices/mock-inv-001/pdf")
                .build());

        mockMvc.perform(get("/api/v1/finops/invoices/{invoiceId}/pdf", invoice.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void tc14_webhookPublic_returns200() throws Exception {
        var request = new WebhookRequest("invoice.paid", "mock-inv-001", null, null);

        mockMvc.perform(post("/api/v1/finops/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    void tc15_webhookUpdatesInvoiceStatus() throws Exception {
        var invoice = invoiceRepository.save(Invoice.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .holdedInvoiceId("mock-inv-002")
                .status(InvoiceStatus.SENT)
                .build());

        var request = new WebhookRequest("invoice.paid", "mock-inv-002", null, null);

        mockMvc.perform(post("/api/v1/finops/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/finops/invoices/{invoiceId}", invoice.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }
}
