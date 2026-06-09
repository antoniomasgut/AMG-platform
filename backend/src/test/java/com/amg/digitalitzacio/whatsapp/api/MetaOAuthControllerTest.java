package com.amg.digitalitzacio.whatsapp.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
import com.amg.digitalitzacio.shared.config.TestRedisConfig;
import com.amg.digitalitzacio.shared.security.JwtProvider;
import com.amg.digitalitzacio.shared.security.Role;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSetting;
import com.amg.digitalitzacio.shared.sysconfig.domain.SystemSettingRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.amg.digitalitzacio.whatsapp.application.MetaGraphApiClient;
import com.amg.digitalitzacio.whatsapp.application.MetaGraphApiClient.PhoneOption;
import com.amg.digitalitzacio.whatsapp.application.MetaGraphApiClient.WabaOption;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfig;
import com.amg.digitalitzacio.whatsapp.domain.WhatsAppWabaConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class MetaOAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private WhatsAppWabaConfigRepository wabaConfigRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private VaultEncryption vaultEncryption;
    @Autowired private PasswordEncoder passwordEncoder;

    // Mockeja les crides reals a la Graph API de Meta
    @MockBean private MetaGraphApiClient graphClient;

    private UUID tenantId;
    private String adminToken;

    @BeforeEach
    void setUp() {
        wabaConfigRepository.deleteAll();
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        // Sembrem configuració Meta necessària per als tests
        seedConfig("META_APP_ID", "test-meta-app-123");
        seedConfig("API_BASE_URL", "https://api.test.amgdl.com");
        seedConfig("FRONTEND_URL", "https://test.amgdl.com");

        var tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());
        tenantId = tenant.getId();

        var admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("test1234"))
                .name("Admin")
                .role(Role.ADMIN)
                .tenantId(tenantId)
                .isActive(true)
                .isBlocked(false)
                .build());

        adminToken = jwtProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), admin.getTenantId());
    }

    // ── Pas 1: generar URL ────────────────────────────────────────────────────

    @Test
    void connect_returns200_withOAuthUrl() throws Exception {
        mockMvc.perform(get("/api/v1/meta/whatsapp/connect")
                        .param("tenantId", tenantId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.oauthUrl").value(containsString("facebook.com/dialog/oauth")))
                .andExpect(jsonPath("$.oauthUrl").value(containsString("whatsapp_business_management")));
    }

    @Test
    void connect_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/meta/whatsapp/connect")
                        .param("tenantId", tenantId.toString()))
                .andExpect(status().isUnauthorized());
    }

    // ── Pas 2: callback (redirecció) ──────────────────────────────────────────

    @Test
    void callback_missingCode_redirectsWithError() throws Exception {
        mockMvc.perform(get("/oauth/meta/whatsapp/callback")
                        .param("error", "access_denied")
                        .param("error_description", "User cancelled"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("error=")));
    }

    @Test
    void callback_validCode_redirectsWithSession() throws Exception {
        // Primer generem un state vàlid cridant a connect
        var connectResult = mockMvc.perform(get("/api/v1/meta/whatsapp/connect")
                        .param("tenantId", tenantId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        String url = objectMapper.readTree(connectResult.getResponse().getContentAsString()).get("oauthUrl").asText();
        String state = extractParam(url, "state");

        // Stub Graph API
        when(graphClient.exchangeCodeForToken(eq("test-code"), any())).thenReturn("test-access-token");
        when(graphClient.getUserId("test-access-token")).thenReturn("meta-user-123");
        when(graphClient.listWabas("test-access-token")).thenReturn(
                List.of(new WabaOption("waba-123", "Empresa Test SA")));
        when(graphClient.listPhoneNumbers("test-access-token", "waba-123")).thenReturn(
                List.of(new PhoneOption("phone-456", "+34600000001", "Empresa Test")));

        mockMvc.perform(get("/oauth/meta/whatsapp/callback")
                        .param("code", "test-code")
                        .param("state", state))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("/portal/admin/integrations/whatsapp?session=")));
    }

    // ── Pas 3: llegir sessió ──────────────────────────────────────────────────

    @Test
    void oauthSession_unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/meta/whatsapp/oauth-session/unknown-token"))
                .andExpect(status().isNotFound());
    }

    // ── Pas 4: confirm ────────────────────────────────────────────────────────

    @Test
    void confirm_fullFlow_savesConfig() throws Exception {
        // Step 1: connect
        var connectResult = mockMvc.perform(get("/api/v1/meta/whatsapp/connect")
                        .param("tenantId", tenantId.toString())
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn();
        String connectUrl = objectMapper.readTree(connectResult.getResponse().getContentAsString()).get("oauthUrl").asText();
        String state = extractParam(connectUrl, "state");

        // Step 2: callback
        when(graphClient.exchangeCodeForToken(any(), any())).thenReturn("access-token-xyz");
        when(graphClient.getUserId(any())).thenReturn("meta-user-abc");
        when(graphClient.listWabas(any())).thenReturn(List.of(new WabaOption("waba-999", "Test WABA")));
        when(graphClient.listPhoneNumbers(any(), eq("waba-999"))).thenReturn(
                List.of(new PhoneOption("phone-777", "+34611111111", "Test Phone")));

        var callbackResult = mockMvc.perform(get("/oauth/meta/whatsapp/callback")
                        .param("code", "auth-code")
                        .param("state", state))
                .andReturn();

        String location = callbackResult.getResponse().getHeader("Location");
        String sessionToken = extractParam(location, "session");

        // Step 3: confirm
        var body = objectMapper.writeValueAsString(Map.of(
                "sessionToken", sessionToken,
                "wabaId", "waba-999",
                "phoneNumberId", "phone-777"));

        mockMvc.perform(post("/api/v1/meta/whatsapp/confirm")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        // Verifica que la config s'ha desat
        var saved = wabaConfigRepository.findByTenantId(tenantId);
        assert saved.isPresent();
        assert "waba-999".equals(saved.get().getWabaId());
        assert "phone-777".equals(saved.get().getPhoneNumberId());
    }

    // ── Desconnexió ───────────────────────────────────────────────────────────

    @Test
    void disconnect_returns204_withExistingConfig() throws Exception {
        wabaConfigRepository.save(WhatsAppWabaConfig.builder()
                .tenantId(tenantId)
                .wabaId("waba-old")
                .phoneNumberId("phone-old")
                .accessTokenEncrypted("enc-token")
                .metaUserId("meta-old")
                .build());

        mockMvc.perform(delete("/api/v1/meta/whatsapp/disconnect/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void disconnect_noConfig_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/meta/whatsapp/disconnect/" + tenantId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seedConfig(String key, String value) {
        systemSettingRepository.save(SystemSetting.builder()
                .key(key)
                .encryptedValue(vaultEncryption.encrypt(value))
                .isSecret(false)
                .description("Test config")
                .valueType("string")
                .updatedAt(Instant.now())
                .build());
    }

    private String extractParam(String url, String param) {
        String marker = param + "=";
        int start = url.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Param '" + param + "' not found in: " + url);
        start += marker.length();
        int end = url.indexOf('&', start);
        return end < 0 ? url.substring(start) : url.substring(start, end);
    }
}
