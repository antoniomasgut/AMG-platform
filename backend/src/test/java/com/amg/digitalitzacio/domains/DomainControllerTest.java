package com.amg.digitalitzacio.domains;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.domains.api.dto.*;
import com.amg.digitalitzacio.domains.domain.*;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Tests d'integració per al Mòdul 23 — Domain Reseller
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class DomainControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private ManagedDomainRepository domainRepository;
    @Autowired private DomainDnsRecordRepository dnsRecordRepository;
    @Autowired private TldPricingRepository tldPricingRepository;

    private Tenant tenant;
    private Tenant otherTenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        // Neteja domini (DNS i dominis) abans de cada test per aïllament complet
        dnsRecordRepository.deleteAll();
        domainRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Domain Tenant").slug("domain-tenant").isActive(true).build());
        otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Domain Tenant").slug("other-domain-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@domain.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@domain.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin User").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@domain.com")
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

    // DOM-01: checkAvailability per un domini lliure → available:true, preu present
    @Test
    void dom01_checkAvailability_freeDomain_returnsAvailableWithPrice() throws Exception {
        var request = new DomainCheckRequest("mon-negoci-lliure.com");

        mockMvc.perform(post("/api/v1/domains/check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainName").value("mon-negoci-lliure.com"))
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.saleRegister").isNumber())
                .andExpect(jsonPath("$.alternatives", hasSize(0)));
    }

    // DOM-02: checkAvailability per un domini ocupat (acaba en "taken.com") → available:false, alternatives present
    @Test
    void dom02_checkAvailability_occupiedDomain_returnsUnavailableWithAlternatives() throws Exception {
        var request = new DomainCheckRequest("mon-negoci.taken.com");

        mockMvc.perform(post("/api/v1/domains/check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domainName").value("mon-negoci.taken.com"))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.alternatives", hasSize(greaterThan(0))));
    }

    // DOM-03: registerDomain (mock) → status ACTIVE, domainName correcte
    @Test
    void dom03_registerDomain_mock_returnsActiveWithCorrectName() throws Exception {
        var request = new RegisterDomainRequest(
                tenant.getId(),
                "restaurant-palma.com",
                null,
                true,
                "Joan Miró",
                "joan@palma.com",
                "+34600000001",
                "12345678A"
        );

        mockMvc.perform(post("/api/v1/domains/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.domainName").value("restaurant-palma.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tenantId").value(tenant.getId().toString()));
    }

    // DOM-04: DNS auto-config per a domini amb landing → dnsConfigured:true després de configure-dns
    @Test
    void dom04_configureDns_afterRegister_dnsConfiguredTrue() throws Exception {
        // Registra primer el domini sense landing
        var registerReq = new RegisterDomainRequest(
                tenant.getId(),
                "botiga-mallorca.es",
                null,
                false,
                "Maria Garcia",
                "maria@mallorca.com",
                "+34600000002",
                "87654321B"
        );

        var registerJson = mockMvc.perform(post("/api/v1/domains/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        var domainId = objectMapper.readTree(registerJson).get("id").asText();

        // Crida a configure-dns i comprova que dnsConfigured és true
        mockMvc.perform(post("/api/v1/domains/{id}/configure-dns", domainId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dnsConfigured").value(true))
                .andExpect(jsonPath("$.dnsRecords", hasSize(greaterThanOrEqualTo(2))));
    }

    // DOM-05: listExpiring → retorna llista (pot estar buida)
    @Test
    void dom05_listExpiring_returnsListPossiblyEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/domains/admin/expiring")
                        .param("days", "30")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // DOM-05b: listExpiring → retorna el domini que caduca aviat
    @Test
    void dom05b_listExpiring_withDomainExpiringIn7Days_returnsIt() throws Exception {
        // Crea un domini que caduca en 5 dies amb autoRenew=true
        domainRepository.save(ManagedDomain.builder()
                .tenantId(tenant.getId())
                .domainName("expiring-soon.cat")
                .tld("cat")
                .status(DomainStatus.EXPIRING_SOON)
                .providerDomainId("MOCK-EXPIRING-001")
                .autoRenew(true)
                .expiresAt(Instant.now().plus(5, ChronoUnit.DAYS))
                .build());

        mockMvc.perform(get("/api/v1/domains/admin/expiring")
                        .param("days", "30")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].domainName").value("expiring-soon.cat"));
    }

    // DOM-06: TLD pricing configurable → SUPER_ADMIN pot actualitzar saleRegister
    @Test
    void dom06_updateTldPricing_superAdmin_updatesPrice() throws Exception {
        var request = new TldPricingRequest(
                BigDecimal.valueOf(25),
                BigDecimal.valueOf(20),
                true
        );

        mockMvc.perform(put("/api/v1/domains/admin/tld-pricing/{tld}", "com")
                        .header("Authorization", "Bearer " + superAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tld").value("com"))
                .andExpect(jsonPath("$.saleRegister").value(25))
                .andExpect(jsonPath("$.saleRenew").value(20))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    // DOM-07: CLIENT no pot registrar dominis → 403
    @Test
    void dom07_registerDomain_clientRole_returns403() throws Exception {
        var request = new RegisterDomainRequest(
                tenant.getId(),
                "client-attempt.cat",
                null,
                false,
                null, null, null, null
        );

        mockMvc.perform(post("/api/v1/domains/register")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // DOM-08: domini no disponible → register retorna 400
    @Test
    void dom08_registerDomain_unavailableDomain_returns400() throws Exception {
        var request = new RegisterDomainRequest(
                tenant.getId(),
                "ocupat.taken.com",
                null,
                false,
                "Pere Pons",
                "pere@mallorca.com",
                "+34600000003",
                "11223344C"
        );

        mockMvc.perform(post("/api/v1/domains/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // DOM-09: credencials OPEN_PROVIDER absents no trenquen el mock → continua funcionant
    @Test
    void dom09_missingProviderCredentials_mockStillWorks() throws Exception {
        var request = new DomainCheckRequest("negoci-mock-test.net");

        // El mock no requereix credencials reals, ha de respondre correctament
        mockMvc.perform(post("/api/v1/domains/check")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    // DOM-10: llistar dominis per tenant → retorna els dominis del tenant correcte
    @Test
    void dom10_listByTenant_returnsOnlyCorrectTenantDomains() throws Exception {
        // Crea un domini pel tenant principal i un per l'altre tenant
        domainRepository.save(ManagedDomain.builder()
                .tenantId(tenant.getId())
                .domainName("tenant-a.es")
                .tld("es")
                .status(DomainStatus.ACTIVE)
                .providerDomainId("MOCK-A-001")
                .build());

        domainRepository.save(ManagedDomain.builder()
                .tenantId(otherTenant.getId())
                .domainName("tenant-b.es")
                .tld("es")
                .status(DomainStatus.ACTIVE)
                .providerDomainId("MOCK-B-001")
                .build());

        mockMvc.perform(get("/api/v1/domains/tenant/{tenantId}", tenant.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].domainName").value("tenant-a.es"))
                .andExpect(jsonPath("$[0].tenantId").value(tenant.getId().toString()));
    }

    // DOM-11: cancel·lar domini → status CANCELLED
    @Test
    void dom11_cancelDomain_statusBecomeCancelled() throws Exception {
        var domain = domainRepository.save(ManagedDomain.builder()
                .tenantId(tenant.getId())
                .domainName("cancel-me.cat")
                .tld("cat")
                .status(DomainStatus.ACTIVE)
                .providerDomainId("MOCK-CANCEL-001")
                .build());

        mockMvc.perform(delete("/api/v1/domains/{id}", domain.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isNoContent());

        // Comprova l'estat via GET
        mockMvc.perform(get("/api/v1/domains/{id}", domain.getId())
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // DOM-12: actualitzar registres DNS → registres desats
    @Test
    void dom12_updateDnsRecords_recordsSaved() throws Exception {
        var domain = domainRepository.save(ManagedDomain.builder()
                .tenantId(tenant.getId())
                .domainName("dns-update.es")
                .tld("es")
                .status(DomainStatus.ACTIVE)
                .providerDomainId("MOCK-DNS-001")
                .build());

        var records = List.of(
                new DnsRecordRequest("A", "@", "93.184.216.34", 3600, null),
                new DnsRecordRequest("A", "www", "93.184.216.34", 3600, null),
                new DnsRecordRequest("MX", "@", "mail.dns-update.es", 3600, 10)
        );

        mockMvc.perform(put("/api/v1/domains/{id}/dns", domain.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(records)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dnsConfigured").value(true))
                .andExpect(jsonPath("$.dnsRecords", hasSize(3)))
                .andExpect(jsonPath("$.dnsRecords[*].type", hasItems("A", "MX")));
    }

    // Extra: listAll → SUPER_ADMIN pot llistar tots els dominis paginat
    @Test
    void domExtra_listAll_superAdmin_returnsAllDomains() throws Exception {
        domainRepository.save(ManagedDomain.builder()
                .tenantId(tenant.getId())
                .domainName("list-all-a.com")
                .tld("com")
                .status(DomainStatus.ACTIVE)
                .providerDomainId("MOCK-ALL-001")
                .build());

        mockMvc.perform(get("/api/v1/domains")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    // Extra: noAuth → 401
    @Test
    void domExtra_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/domains/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DomainCheckRequest("test.com"))))
                .andExpect(status().isUnauthorized());
    }

    // Extra: listTldPricing → SUPER_ADMIN rep les tarifes dels TLDs actius
    @Test
    void domExtra_listTldPricing_superAdmin_returnsPricingList() throws Exception {
        mockMvc.perform(get("/api/v1/domains/admin/tld-pricing")
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].tld", hasItems("com", "es")));
    }

    // Extra: ADMIN no pot accedir a tld-pricing → 403
    @Test
    void domExtra_tldPricing_adminRole_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/domains/admin/tld-pricing")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // Extra: registerDomain amb landingId → dnsConfigured:true automàticament
    @Test
    void domExtra_registerDomainWithLanding_autoDnsConfigured() throws Exception {
        var request = new RegisterDomainRequest(
                tenant.getId(),
                "amb-landing.cat",
                java.util.UUID.randomUUID(),  // landingId present
                true,
                "Tomeu Vidal",
                "tomeu@mallorca.com",
                "+34600000099",
                "99887766D"
        );

        mockMvc.perform(post("/api/v1/domains/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.dnsConfigured").value(true))
                .andExpect(jsonPath("$.dnsRecords", hasSize(greaterThanOrEqualTo(2))));
    }
}
