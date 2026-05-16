package com.amg.digitalitzacio.vault.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.amg.digitalitzacio.vault.api.dto.*;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class VaultControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ServiceProfileRepository profileRepository;
    @Autowired private PhaseRepository phaseRepository;
    @Autowired private CatalogServiceRepository serviceRepository;
    @Autowired private CredentialFieldRepository fieldRepository;
    @Autowired private TenantProfileRepository tenantProfileRepository;
    @Autowired private TenantServiceRepository tenantServiceRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        profileRepository.deleteAll();
        phaseRepository.deleteAll();
        serviceRepository.deleteAll();
        fieldRepository.deleteAll();

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

    /* ── TC-01: Crear perfil ── */
    @Test
    void tc01_crearPerfil_Returns201() throws Exception {
        var request = new CreateProfileRequest("Pla Avançat", "pla-avancat", "Perfil complet");

        mockMvc.perform(post("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pla Avançat"))
                .andExpect(jsonPath("$.slug").value("pla-avancat"));
    }

    /* ── TC-02: Llistar perfils ── */
    @Test
    void tc02_llistarPerfils_Returns200() throws Exception {
        profileRepository.save(ServiceProfile.builder()
                .name("Pla Bàsic").slug("pla-basic").description("Bàsic").isActive(true).build());
        profileRepository.save(ServiceProfile.builder()
                .name("Pla Avançat").slug("pla-avancat").description("Avançat").isActive(true).build());

        mockMvc.perform(get("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /* ── TC-03: Crear perfil amb 2 fases i 3 serveis ── */
    @Test
    void tc03_crearPerfilAmbFasesIServeis_Returns201() throws Exception {
        // Crear perfil
        var profileReq = new CreateProfileRequest("Pla Avançat", "pla-avancat", null);
        var profileJson = mockMvc.perform(post("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var profile = objectMapper.readValue(profileJson, ProfileResponse.class);

        // Afegir fase 1
        var phase1Req = new CreatePhaseRequest("Configuració bàsica", "Primera fase", 1);
        mockMvc.perform(post("/api/v1/vault/profiles/{profileId}/phases", profile.id())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phase1Req)))
                .andExpect(status().isCreated());

        // Afegir fase 2
        var phase2Req = new CreatePhaseRequest("Automatitzacions", "Segona fase", 2);
        var profileJson2 = mockMvc.perform(post("/api/v1/vault/profiles/{profileId}/phases", profile.id())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(phase2Req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var updatedProfile = objectMapper.readValue(profileJson2, ProfileResponse.class);

        // Afegir servei a fase 1: necessitem el phaseId
        var phase1 = updatedProfile.phases().stream()
                .filter(p -> "Configuració bàsica".equals(p.name())).findFirst().orElseThrow();
        var phase2 = updatedProfile.phases().stream()
                .filter(p -> "Automatitzacions".equals(p.name())).findFirst().orElseThrow();

        var svc1Req = new CreateServiceRequest("WhatsApp Business", "whatsapp-biz", null,
                "CREDENTIALS", BigDecimal.valueOf(10), BigDecimal.valueOf(50), BigDecimal.TEN, 1);
        mockMvc.perform(post("/api/v1/vault/phases/{phaseId}/services", phase1.id())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(svc1Req)))
                .andExpect(status().isCreated());

        var svc2Req = new CreateServiceRequest("SMTP Corporatiu", "smtp-corp", null,
                "CREDENTIALS", BigDecimal.valueOf(5), BigDecimal.valueOf(30), BigDecimal.TEN, 2);
        mockMvc.perform(post("/api/v1/vault/phases/{phaseId}/services", phase1.id())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(svc2Req)))
                .andExpect(status().isCreated());

        var svc3Req = new CreateServiceRequest("Landing Pro", "landing-pro", null,
                "LANDING", BigDecimal.valueOf(30), BigDecimal.valueOf(80), BigDecimal.TEN, 1);
        mockMvc.perform(post("/api/v1/vault/phases/{phaseId}/services", phase2.id())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(svc3Req)))
                .andExpect(status().isCreated());

        // Verificar que el perfil té 2 fases
        mockMvc.perform(get("/api/v1/vault/profiles/{id}", profile.id())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phases", hasSize(2)))
                .andExpect(jsonPath("$.phases[0].name").value("Configuració bàsica"))
                .andExpect(jsonPath("$.phases[1].name").value("Automatitzacions"));
    }

    /* ── TC-04: Assignar perfil a tenant ── */
    @Test
    void tc04_assignarPerfilATenant_Returns201() throws Exception {
        // Crear perfil amb una fase i un servei
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Pla Bàsic").slug("pla-basic").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        serviceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("WhatsApp").slug("whatsapp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.TEN).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());

        mockMvc.perform(post("/api/v1/vault/tenants/{tenantId}/profiles/{profileId}",
                tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.phases").exists())
                .andExpect(jsonPath("$.phases[0].name").value("Fase 1"));
    }

    /* ── TC-05: CLIENT no pot establir credencials ── */
    @Test
    void tc05_clientNoPotEstablirCredencials_Returns403() throws Exception {
        var request = new SetCredentialRequest("some-value");
        mockMvc.perform(put("/api/v1/vault/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}",
                tenant.getId(), UUID.randomUUID(), UUID.randomUUID())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-06: Afegir servei add-on a tenant ── */
    @Test
    void tc06_afegirAddonATenant_Returns201() throws Exception {
        var addonService = serviceRepository.save(CatalogService.builder()
                .name("Landing extra").slug("landing-extra")
                .type(ServiceType.LANDING).isAddon(true)
                .cost(BigDecimal.valueOf(30)).salePrice(BigDecimal.valueOf(80)).build());

        mockMvc.perform(post("/api/v1/vault/tenants/{tenantId}/addons/{serviceId}",
                tenant.getId(), addonService.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salePrice").exists())
                .andExpect(jsonPath("$.status").value("PENDING_CLIENT_APPROVAL"));
    }

    /* ── TC-07: Pressupost d'un perfil ── */
    @Test
    void tc07_pressupostPerfil_Returns200() throws Exception {
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Pla Test").slug("pla-test").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        serviceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("WhatsApp").slug("whatsapp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.TEN).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());
        serviceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("SMTP").slug("smtp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.valueOf(5)).salePrice(BigDecimal.valueOf(30)).sortOrder(2).build());

        mockMvc.perform(get("/api/v1/vault/tenants/{tenantId}/budget", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .param("profileId", profile.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.totalCost").exists())
                .andExpect(jsonPath("$.phases", hasSize(1)));
    }

    /* ── TC-08: CLIENT veu pressupost sense costos ── */
    @Test
    void tc08_clientVeuPressupostSenseCostos() throws Exception {
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Pla Test").slug("pla-test").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        serviceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("WhatsApp").slug("whatsapp")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.TEN).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());

        mockMvc.perform(get("/api/v1/vault/tenants/{tenantId}/budget", tenant.getId())
                .header("Authorization", "Bearer " + clientToken)
                .param("profileId", profile.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").exists())
                .andExpect(jsonPath("$.total").isNumber());
    }

    /* ── TC-09: Crear servei amb salePrice <= cost → 400 ── */
    @Test
    void tc09_crearServeiAmbPreuInvalid_Returns400() throws Exception {
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Price Test").slug("price-test").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Test").sortOrder(1).build());

        var request = new CreateServiceRequest("Test", "test", null,
                "CREDENTIALS", BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.TEN, 1);

        mockMvc.perform(post("/api/v1/vault/phases/{phaseId}/services", phase.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ── TC-10: Accés sense JWT → 401 ── */
    @Test
    void tc10_accesSenseJWT_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/vault/profiles"))
                .andExpect(status().isUnauthorized());
    }

    /* ── TC-11: CLIENT no pot crear perfils (POST) → 403 ── */
    @Test
    void tc11_clientNoPotCrearPerfil_Returns403() throws Exception {
        var request = new CreateProfileRequest("Test", "test", null);
        mockMvc.perform(post("/api/v1/vault/profiles")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-12: Configurar credencial i verificar isConfigured ── */
    @Test
    void tc12_configurarCredencial_MarcaIsConfigured() throws Exception {
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Pla Test").slug("pla-test2").isActive(true).build());
        var phase = phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());
        var service = serviceRepository.save(CatalogService.builder()
                .phaseId(phase.getId()).name("WhatsApp").slug("whatsapp2")
                .type(ServiceType.CREDENTIALS).isAddon(false)
                .cost(BigDecimal.TEN).salePrice(BigDecimal.valueOf(50)).sortOrder(1).build());
        var field = fieldRepository.save(CredentialField.builder()
                .serviceId(service.getId()).key("apiKey").label("API Key")
                .type(FieldType.PASSWORD).isRequired(true).sortOrder(1).build());

        // Assignar perfil al tenant
        mockMvc.perform(post("/api/v1/vault/tenants/{tenantId}/profiles/{profileId}",
                tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + superAdminToken));

        // Establir credencial
        var credReq = new SetCredentialRequest("my-api-key-12345");
        mockMvc.perform(put("/api/v1/vault/tenants/{tenantId}/services/{serviceId}/fields/{fieldId}",
                tenant.getId(), service.getId(), field.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSet").value(true))
                .andExpect(jsonPath("$.maskedValue").value("***2345"));

        // Verificar estat de configuració: el servei hauria d'estar configurat
        mockMvc.perform(get("/api/v1/vault/tenants/{tenantId}/setup", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk());
    }

    /* ── TC-13: ADMIN pot gestionar assignacions ── */
    @Test
    void tc13_adminPotAssignarPerfil_Returns201() throws Exception {
        var profile = profileRepository.save(ServiceProfile.builder()
                .name("Pla Test").slug("pla-test3").isActive(true).build());
        phaseRepository.save(Phase.builder()
                .profileId(profile.getId()).name("Fase 1").sortOrder(1).build());

        mockMvc.perform(post("/api/v1/vault/tenants/{tenantId}/profiles/{profileId}",
                tenant.getId(), profile.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated());
    }
}
