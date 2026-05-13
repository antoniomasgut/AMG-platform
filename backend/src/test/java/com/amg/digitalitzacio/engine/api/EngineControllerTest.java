package com.amg.digitalitzacio.engine.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.engine.api.dto.*;
import com.amg.digitalitzacio.engine.domain.*;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class EngineControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private LandingRepository landingRepository;
    @Autowired private LandingVersionRepository landingVersionRepository;
    @Autowired private ContactLeadRepository contactLeadRepository;
    @Autowired private CatalogServiceRepository catalogServiceRepository;
    @Autowired private TenantServiceRepository tenantServiceRepository;

    private Tenant tenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";
    private UUID landingServiceId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();
        landingRepository.deleteAll();
        landingVersionRepository.deleteAll();
        contactLeadRepository.deleteAll();
        tenantServiceRepository.deleteAll();
        catalogServiceRepository.deleteAll();

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

        // Create a LANDING service in catalog and assign to tenant
        var landingService = catalogServiceRepository.save(CatalogService.builder()
                .name("Landing Pro").slug("landing-pro")
                .type(ServiceType.LANDING).isAddon(false)
                .cost(java.math.BigDecimal.valueOf(30)).salePrice(java.math.BigDecimal.valueOf(80))
                .sortOrder(1).build());
        landingServiceId = landingService.getId();

        tenantServiceRepository.save(TenantService.builder()
                .tenantId(tenant.getId()).serviceId(landingService.getId())
                .status(ServiceStatus.VERIFIED).build());
    }

    /* ── TC-01: Crear landing (SUPER_ADMIN) ── */
    @Test
    void tc01_crearLanding_Returns201() throws Exception {
        var request = new CreateLandingRequest("Restaurant Can Pedro", "restaurant-can-pedro",
                "Restaurant de cuina mallorquina", landingServiceId);

        mockMvc.perform(post("/api/v1/engine/tenants/{tenantId}/landings", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Restaurant Can Pedro"))
                .andExpect(jsonPath("$.slug").value("restaurant-can-pedro"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    /* ── TC-02: Crear landing slug duplicat → 400 ── */
    @Test
    void tc02_crearLandingSlugDuplicat_Returns400() throws Exception {
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Existing").slug("duplicat").build());

        var request = new CreateLandingRequest("New", "duplicat", null, landingServiceId);

        mockMvc.perform(post("/api/v1/engine/tenants/{tenantId}/landings", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ── TC-03: Llistar landings ── */
    @Test
    void tc03_llistarLandings_Returns200() throws Exception {
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Landing 1").slug("landing-1").build());
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Landing 2").slug("landing-2").build());

        mockMvc.perform(get("/api/v1/engine/tenants/{tenantId}/landings", tenant.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    /* ── TC-04: Crear versió esborrany ── */
    @Test
    void tc04_crearVersio_Returns201() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-landing").build());

        var request = new CreateVersionRequest(
                Map.of("blocks", List.of(Map.of("id", "blk_1", "type", "hero", "props", Map.of("title", "Hola")))),
                Map.of("primaryColor", "#ff0000")
        );

        mockMvc.perform(post("/api/v1/engine/landings/{landingId}/versions", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionNumber").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    /* ── TC-05: Actualitzar versió DRAFT ── */
    @Test
    void tc05_actualitzarVersio_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-update").build());

        var version = landingVersionRepository.save(LandingVersion.builder()
                .landingId(landing.getId()).versionNumber(1)
                .status(VersionStatus.DRAFT).content("{\"blocks\":[]}").build());

        var request = new CreateVersionRequest(
                Map.of("blocks", List.of(Map.of("id", "blk_1", "type", "text", "props", Map.of("body", "Updated")))),
                null
        );

        mockMvc.perform(put("/api/v1/engine/landings/{landingId}/versions/{versionId}", landing.getId(), version.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.blocks[0].props.body").value("Updated"));
    }

    /* ── TC-06: Publicar landing ── */
    @Test
    void tc06_publicarLanding_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-publish").build());

        landingVersionRepository.save(LandingVersion.builder()
                .landingId(landing.getId()).versionNumber(1)
                .status(VersionStatus.DRAFT).content("{\"blocks\":[]}").build());

        mockMvc.perform(post("/api/v1/engine/landings/{landingId}/publish", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.versionNumber").value(1));
    }

    /* ── TC-07: Render públic de landing publicada ── */
    @Test
    void tc07_renderPublic_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test Render").slug("test-render")
                .metaDescription("Restaurant de cuina mallorquina").build());

        var version = landingVersionRepository.save(LandingVersion.builder()
                .landingId(landing.getId()).versionNumber(1)
                .status(VersionStatus.PUBLISHED).publishedAt(java.time.Instant.now())
                .content("{\"blocks\":[{\"id\":\"blk_1\",\"type\":\"hero\",\"props\":{\"title\":\"Hola Món\"}}]}")
                .build());

        landing.setStatus(LandingStatus.PUBLISHED);
        landing.setPublishedVersionId(version.getId());
        landingRepository.save(landing);

        mockMvc.perform(get("/api/v1/engine/render/test-render"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Hola Món")))
                .andExpect(content().string(containsString("og:title")))
                .andExpect(content().string(containsString("og:description")))
                .andExpect(content().string(containsString("canonical")))
                .andExpect(content().string(containsString("Avís legal")));
    }

    /* ── TC-08: Landing no publicada → 404 en públic ── */
    @Test
    void tc08_renderLandingNoPublicada_Returns404() throws Exception {
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Draft").slug("test-draft")
                .status(LandingStatus.DRAFT).build());

        mockMvc.perform(get("/api/v1/engine/render/test-draft"))
                .andExpect(status().isNotFound());
    }

    /* ── TC-09: CLIENT no pot crear landing → 403 ── */
    @Test
    void tc09_clientNoPotCrearLanding_Returns403() throws Exception {
        var request = new CreateLandingRequest("Test", "test", null, landingServiceId);

        mockMvc.perform(post("/api/v1/engine/tenants/{tenantId}/landings", tenant.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-10: Enviar formulari de contacte ── */
    @Test
    void tc10_enviarContacte_Returns201() throws Exception {
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-contact")
                .status(LandingStatus.PUBLISHED).build());

        var request = new ContactRequest("Joan Servera", "joan@example.com", "+34600123456", "Vull pressupost", true);

        mockMvc.perform(post("/api/v1/engine/render/test-contact/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Missatge rebut correctament"));
    }

    /* ── TC-11: Accés sense JWT a gestió → 401 ── */
    @Test
    void tc11_accesSenseJWT_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/engine/tenants/{tenantId}/landings", tenant.getId()))
                .andExpect(status().isUnauthorized());
    }

    /* ── TC-12: Accés públic a render sense JWT → 200 ── */
    @Test
    void tc12_renderPublicSenseJWT_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Public").slug("test-public")
                .status(LandingStatus.PUBLISHED).build());

        var version = landingVersionRepository.save(LandingVersion.builder()
                .landingId(landing.getId()).versionNumber(1)
                .status(VersionStatus.PUBLISHED).publishedAt(java.time.Instant.now())
                .content("{\"blocks\":[]}").build());

        landing.setPublishedVersionId(version.getId());
        landingRepository.save(landing);

        mockMvc.perform(get("/api/v1/engine/render/test-public"))
                .andExpect(status().isOk());
    }

    /* ── TC-13: Configurar domini autogestionat ── */
    @Test
    void tc13_configurarDominiAutogestionat_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-domain").build());

        var request = new DomainConfigRequest("canpedro.com", false, null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/engine/landings/{landingId}/domain", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("canpedro.com"))
                .andExpect(jsonPath("$.managed").value(false))
                .andExpect(jsonPath("$.domainStatus").value("DNS_PENDING"));
    }

    /* ── TC-14: Despublicar landing ── */
    @Test
    void tc14_despublicarLanding_Returns204() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-unpublish")
                .status(LandingStatus.PUBLISHED).publishedVersionId(UUID.randomUUID()).build());

        mockMvc.perform(post("/api/v1/engine/landings/{landingId}/unpublish", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        var updated = landingRepository.findById(landing.getId()).orElseThrow();
        assert updated.getStatus() == LandingStatus.DRAFT : "Hauria d'estar DRAFT";
        assert updated.getPublishedVersionId() == null : "publishedVersionId hauria de ser null";
    }

    /* ── TC-15: Enviar formulari sense consentiment → 400 ── */
    @Test
    void tc15_enviarContacteSenseConsent_Returns400() throws Exception {
        landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-noconsent")
                .status(LandingStatus.PUBLISHED).build());

        var request = new ContactRequest("Joan", "joan@test.com", null, "Sense consent", false);

        mockMvc.perform(post("/api/v1/engine/render/test-noconsent/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /* ── TC-16: Configurar domini gestionat ── */
    @Test
    void tc16_configurarDominiGestionat_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-managed-domain").build());

        var request = new DomainConfigRequest("canpedro.com", true, "Namecheap",
                java.time.LocalDate.now().plusYears(1), java.math.BigDecimal.valueOf(12.99),
                "Joan Servera", "joan@canpedro.com", "+34600123456");

        mockMvc.perform(put("/api/v1/engine/landings/{landingId}/domain", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("canpedro.com"))
                .andExpect(jsonPath("$.managed").value(true))
                .andExpect(jsonPath("$.domainStatus").value("PURCHASED"))
                .andExpect(jsonPath("$.registrar").value("Namecheap"));
    }

    /* ── TC-17: CLIENT no pot configurar domini gestionat → 403 ── */
    @Test
    void tc17_clientNoPotDominiGestionat_Returns403() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-client-managed").build());

        var request = new DomainConfigRequest("canpedro.com", true, null, null, null, null, null, null);

        mockMvc.perform(put("/api/v1/engine/landings/{landingId}/domain", landing.getId())
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    /* ── TC-18: Actualitzar estat del domini ── */
    @Test
    void tc18_actualitzarEstatDomini_Returns200() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-domain-status").customDomain("canpedro.com")
                .domainStatus(DomainStatus.PENDING_PURCHASE).build());

        var request = new UpdateDomainStatusRequest("PURCHASED", "Namecheap",
                java.time.LocalDate.now().plusYears(1), java.math.BigDecimal.valueOf(12.99));

        mockMvc.perform(post("/api/v1/engine/landings/{landingId}/domain/status", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainStatus").value("PURCHASED"))
                .andExpect(jsonPath("$.registrar").value("Namecheap"));
    }

    /* ── TC-19: Eliminar configuració de domini ── */
    @Test
    void tc19_eliminarDomini_Returns204() throws Exception {
        var landing = landingRepository.save(Landing.builder()
                .tenantId(tenant.getId()).serviceId(landingServiceId)
                .title("Test").slug("test-remove-domain")
                .customDomain("canpedro.com").domainStatus(DomainStatus.VERIFIED).build());

        mockMvc.perform(delete("/api/v1/engine/landings/{landingId}/domain", landing.getId())
                .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        var updated = landingRepository.findById(landing.getId()).orElseThrow();
        assert updated.getCustomDomain() == null : "customDomain hauria de ser null";
        assert updated.getDomainStatus() == DomainStatus.NOT_CONFIGURED : "Hauria d'estar NOT_CONFIGURED";
    }
}
