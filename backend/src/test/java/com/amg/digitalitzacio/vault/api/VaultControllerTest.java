package com.amg.digitalitzacio.vault.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class VaultControllerTest {

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
    private CredentialFieldRepository fieldRepository;

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
    private Phase phase2;
    private CatalogService service1;
    private CatalogService service2;
    private CatalogService service3;
    private CatalogService addonService;
    private CredentialField field1;
    private CredentialField field2;

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

        // Crear perfil
        profile = profileRepository.save(ServiceProfile.builder()
                .name("Test Profile").slug("test-profile").isActive(true).build());

        // Crear fases
        phase1 = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        phase2 = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 2").sortOrder(2).build());

        // Crear serveis
        service1 = serviceRepository.save(CatalogService.builder()
                .phaseId(phase1.getId()).name("WhatsApp").slug("whatsapp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.valueOf(10)).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());
        service2 = serviceRepository.save(CatalogService.builder()
                .phaseId(phase1.getId()).name("SMTP").slug("smtp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.valueOf(5)).salePrice(BigDecimal.valueOf(30)).sortOrder(2).build());
        service3 = serviceRepository.save(CatalogService.builder()
                .phaseId(phase2.getId()).name("Landing Pro").slug("landing-pro")
                .type(ServiceType.LANDING).isAddon(false)
                .cost(BigDecimal.valueOf(30)).salePrice(BigDecimal.valueOf(80)).sortOrder(1).build());
        addonService = serviceRepository.save(CatalogService.builder()
                .name("Landing extra").slug("landing-extra")
                .type(ServiceType.LANDING).isAddon(true)
                .cost(BigDecimal.valueOf(25)).salePrice(BigDecimal.valueOf(60)).build());

        // Crear camps de credencial
        field1 = fieldRepository.save(CredentialField.builder()
                .serviceId(service1.getId()).fieldKey("apiKey").label("API Key")
                .type(FieldType.PASSWORD).isRequired(true).sortOrder(1).build());
        field2 = fieldRepository.save(CredentialField.builder()
                .serviceId(service1.getId()).fieldKey("phoneId").label("Phone ID")
                .type(FieldType.TEXT).isRequired(true).sortOrder(2).build());
    }

    /* ─────────── TC-01: Crear perfil ─────────── */
    @Test
    void tc01_createProfile_Returns201() throws Exception {
        var json = """
                {"name": "New Profile", "slug": "new-profile"}
                """;
        mockMvc.perform(post("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Profile"));
    }

    /* ─────────── TC-02: Llistar perfils ─────────── */
    @Test
    void tc02_listProfiles_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Profile"));
    }

    /* ─────────── TC-03: Veure perfil complert ─────────── */
    @Test
    void tc03_getProfile_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/profiles/{id}", profile.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases.length()").value(2));
    }

    /* ─────────── TC-04: Assignar perfil a tenant ─────────── */
    @Test
    void tc04_assignProfile_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phases.length()").value(2));
    }

    /* ─────────── TC-05: Aprovar fase ─────────── */
    @Test
    void tc05_approvePhase_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/phases/{phid}/approve", tenant.getId(), phase1.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
    }

    /* ─────────── TC-06: Rebutjar fase ─────────── */
    @Test
    void tc06_rejectPhase_Returns204() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/phases/{phid}/reject", tenant.getId(), phase1.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    /* ─────────── TC-07: Establir credencial ─────────── */
    @Test
    void tc07_setCredential_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        var body = """
                {"value": "test-key-12345"}
                """;

        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/fields/{fid}",
                        tenant.getId(), service1.getId(), field1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSet").value(true));
    }

    /* ─────────── TC-08: Canviar estat servei ─────────── */
    @Test
    void tc08_changeServiceStatus_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        var body = """
                {"status": "AWAITING_CLIENT"}
                """;

        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/status",
                        tenant.getId(), service1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-09: CLIENT veu setup (emmascarat) ─────────── */
    @Test
    void tc09_clientSeesSetup_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/setup", tenant.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profiles").isArray());
    }

    /* ─────────── TC-10: Afegir add-on de pagament ─────────── */
    @Test
    void tc10_addonPagament_Returns201() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/addons/{sid}", tenant.getId(), addonService.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.approvalRequired").value(true));
    }

    /* ─────────── TC-11: Afegir add-on gratuït ─────────── */
    @Test
    void tc11_addonGratuit_Returns201() throws Exception {
        var freeService = serviceRepository.save(CatalogService.builder()
                .name("Free Addon").slug("free-addon")
                .type(ServiceType.OTHER).isAddon(true)
                .cost(BigDecimal.ZERO).salePrice(BigDecimal.ZERO).build());

        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/addons/{sid}", tenant.getId(), freeService.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    /* ─────────── TC-12: Avançar fase a COMPLETED ─────────── */
    @Test
    void tc12_advancePhase_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        // Aprovar fase
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/phases/{phid}/approve",
                tenant.getId(), phase1.getId()).header("Authorization", "Bearer " + adminToken));

        // Configurar credencials
        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/fields/{fid}",
                        tenant.getId(), service1.getId(), field1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"k\"}"));
        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/fields/{fid}",
                        tenant.getId(), service1.getId(), field2.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"p\"}"));

        // Verificar serveis
        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/status",
                        tenant.getId(), service1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"));
        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/status",
                        tenant.getId(), service2.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"));

        // Avançar a COMPLETED
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/phases/{phid}/advance",
                        tenant.getId(), phase1.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-13: Pressupost (SUPER_ADMIN) ─────────── */
    @Test
    void tc13_budget_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/budget?profileId={pid}",
                        tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());
    }

    /* ─────────── TC-14: CLIENT veu pressupost sense cost ─────────── */
    @Test
    void tc14_clientBudgetWithoutCost_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/budget?profileId={pid}",
                        tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.totalCost").doesNotExist());
    }

    /* ─────────── TC-15: Crear servei amb preu invàlid ─────────── */
    @Test
    void tc15_createServiceInvalidPrice_Returns400() throws Exception {
        var json = """
                {"name":"Bad","slug":"bad","type":"CREDENTIALS","cost":10,"salePrice":5}
                """;
        // Intentar crear addon amb salePrice <= cost
        mockMvc.perform(post("/api/v1/vault/services")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    /* ─────────── TC-16: CLIENT no pot establir credencials ─────────── */
    @Test
    void tc16_clientCannotSetCredential_Returns403() throws Exception {
        mockMvc.perform(put("/api/v1/vault/tenants/{tid}/services/{sid}/fields/{fid}",
                        tenant.getId(), service1.getId(), field1.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"value\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    /* ─────────── TC-17: Accés sense JWT ─────────── */
    @Test
    void tc17_noJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vault/profiles"))
                .andExpect(status().isUnauthorized());
    }

    /* ─────────── TC-18: Monitoring invoices ─────────── */
    @Test
    void tc18_monitoringInvoices_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/monitoring/invoices", tenant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-19: Monitoring payments ─────────── */
    @Test
    void tc19_monitoringPayments_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/monitoring/payments", tenant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-20: Monitoring phases ─────────── */
    @Test
    void tc20_monitoringPhases_Returns200() throws Exception {
        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/monitoring/phases", tenant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /* ─────────── TC-21: ADMIN veu setup amb valors en clar ─────────── */
    @Test
    void tc21_adminSeesSetupWithClearValues_Returns200() throws Exception {
        mockMvc.perform(post("/api/v1/vault/tenants/{tid}/profiles/{pid}", tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(get("/api/v1/vault/tenants/{tid}/setup", tenant.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }
}
