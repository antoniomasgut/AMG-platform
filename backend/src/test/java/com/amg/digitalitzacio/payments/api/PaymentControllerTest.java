package com.amg.digitalitzacio.payments.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.billing.domain.*;
import com.amg.digitalitzacio.payments.api.dto.*;
import com.amg.digitalitzacio.payments.domain.*;
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
class PaymentControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private StripeConfigRepository stripeConfigRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private com.amg.digitalitzacio.shared.sysconfig.application.SystemConfigService systemConfigService;
    @Autowired private BudgetRepository budgetRepository;
    @Autowired private BudgetLineRepository budgetLineRepository;

    private Tenant tenant;
    private Tenant otherTenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private String otherClientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        stripeConfigRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Pay Tenant").slug("pay-tenant").isActive(true).build());
        otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@pay.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@pay.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@pay.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Client User").role(Role.CLIENT)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var otherClient = userRepository.save(User.builder()
                .email("other@pay.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Other Client").role(Role.CLIENT)
                .tenantId(otherTenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(
                superAdmin.getId(), superAdmin.getEmail(), superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
        clientToken = jwtProvider.generateAccessToken(
                client.getId(), client.getEmail(), client.getRole(), client.getTenantId());
        otherClientToken = jwtProvider.generateAccessToken(
                otherClient.getId(), otherClient.getEmail(), otherClient.getRole(), otherClient.getTenantId());
    }

    @Test
    void tc01_configureStripe_returns201() throws Exception {
        var request = new StripeConfigRequest(tenant.getId(), "vault-ref-stripe", "whsec_test");

        mockMvc.perform(post("/api/v1/payments/configure")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void tc02_getConfig_returns200() throws Exception {
        stripeConfigRepository.save(StripeConfig.builder()
                .tenantId(tenant.getId())
                .apiKeyRef("vault-ref-stripe")
                .webhookSecret("whsec_test")
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/v1/payments/configure/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }

    @Test
    void tc03_clientCannotViewConfig_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/payments/configure/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc04_createCheckoutForAcceptedBudget_returns201() throws Exception {
        var budget = budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId())
                .subtotal(new BigDecimal("150.00"))
                .discountTotal(BigDecimal.ZERO)
                .total(new BigDecimal("150.00"))
                .status(BudgetStatus.ACCEPTED)
                .build());

        mockMvc.perform(post("/api/v1/payments/budgets/{budgetId}/checkout", budget.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.checkoutUrl").isNotEmpty());
    }

    @Test
    void tc05_createCheckoutForDraftBudget_returns400() throws Exception {
        var budget = budgetRepository.save(Budget.builder()
                .tenantId(tenant.getId())
                .subtotal(new BigDecimal("100.00"))
                .discountTotal(BigDecimal.ZERO)
                .total(new BigDecimal("100.00"))
                .status(BudgetStatus.DRAFT)
                .build());

        mockMvc.perform(post("/api/v1/payments/budgets/{budgetId}/checkout", budget.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tc06_listPaymentsEmpty_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/payments")
                        .param("tenantId", tenant.getId().toString())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void tc07_getPayment_returns200() throws Exception {
        var payment = paymentRepository.save(Payment.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .status(PaymentStatus.PENDING)
                .checkoutUrl("https://mock.stripe.com/checkout/test")
                .build());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void tc08_clientCanViewOwnPayment_returns200() throws Exception {
        var payment = paymentRepository.save(Payment.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .status(PaymentStatus.COMPLETED)
                .build());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void tc09_clientCannotViewOtherTenantPayment_returns403() throws Exception {
        var payment = paymentRepository.save(Payment.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .status(PaymentStatus.COMPLETED)
                .build());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getId())
                        .header("Authorization", "Bearer " + otherClientToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void tc10_refundPayment_returns200() throws Exception {
        var payment = paymentRepository.save(Payment.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("100.00"))
                .currency("EUR")
                .status(PaymentStatus.COMPLETED)
                .stripePaymentIntentId("pi_mock_001")
                .build());

        mockMvc.perform(post("/api/v1/payments/{paymentId}/refund", payment.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));
    }

    @Test
    void tc11_dashboard_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/payments/dashboard")
                        .param("tenantId", tenant.getId().toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").isNumber());
    }

    @Test
    void tc12_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/payments"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tc13_clientCannotCreateCheckout_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/payments/budgets/{budgetId}/checkout", UUID.randomUUID())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tc14_webhookPublic_returns200() throws Exception {
        systemConfigService.setInternal("STRIPE_WEBHOOK_SECRET", "test-webhook-secret");
        var request = new WebhookRequest("checkout.session.completed", "sess_mock_001", "pi_mock_001");

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Token", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true));
    }

    @Test
    void tc15_webhookCompletesPayment() throws Exception {
        var payment = paymentRepository.save(Payment.builder()
                .tenantId(tenant.getId())
                .budgetId(UUID.randomUUID())
                .amount(new BigDecimal("200.00"))
                .currency("EUR")
                .stripeSessionId("sess_mock_002")
                .status(PaymentStatus.PENDING)
                .build());

        systemConfigService.setInternal("STRIPE_WEBHOOK_SECRET", "test-webhook-secret");
        var webhookRequest = new WebhookRequest("checkout.session.completed", "sess_mock_002", "pi_mock_002");

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Token", "test-webhook-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(webhookRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/payments/{paymentId}", payment.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }
}
