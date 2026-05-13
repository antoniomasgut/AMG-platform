package com.amg.digitalitzacio.assets.api;

import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.auth.domain.User;
import com.amg.digitalitzacio.auth.domain.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
class AssetControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProvider jwtProvider;

    private Tenant tenant;
    private Tenant otherTenant;
    private String superAdminToken;
    private String adminToken;
    private String clientToken;
    private String otherClientToken;
    private static final String PASSWORD = "pass1234";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        tenantRepository.deleteAll();

        tenant = tenantRepository.save(Tenant.builder()
                .name("Test Tenant").slug("test-tenant").isActive(true).build());
        otherTenant = tenantRepository.save(Tenant.builder()
                .name("Other Tenant").slug("other-tenant").isActive(true).build());

        var superAdmin = userRepository.save(User.builder()
                .email("superadmin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Super Admin").role(Role.SUPER_ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Admin").role(Role.ADMIN)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var client = userRepository.save(User.builder()
                .email("client@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Client").role(Role.CLIENT)
                .tenantId(tenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());
        var otherClient = userRepository.save(User.builder()
                .email("other@test.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .name("Other").role(Role.CLIENT)
                .tenantId(otherTenant.getId()).isActive(true).isBlocked(false).failedAttempts(0)
                .build());

        superAdminToken = jwtProvider.generateAccessToken(superAdmin.getId(), superAdmin.getEmail(),
                superAdmin.getRole(), superAdmin.getTenantId());
        adminToken = jwtProvider.generateAccessToken(admin.getId(), admin.getEmail(),
                admin.getRole(), admin.getTenantId());
        clientToken = jwtProvider.generateAccessToken(client.getId(), client.getEmail(),
                client.getRole(), client.getTenantId());
        otherClientToken = jwtProvider.generateAccessToken(otherClient.getId(), otherClient.getEmail(),
                otherClient.getRole(), otherClient.getTenantId());
    }

    @AfterEach
    void cleanStorage() {
        // Clean up test storage directory
        try {
            var storagePath = Path.of("/tmp/test-assets");
            if (Files.exists(storagePath)) {
                try (var walk = Files.walk(storagePath)) {
                    walk.sorted(java.util.Comparator.reverseOrder())
                            .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                }
            }
        } catch (Exception ignored) {}
    }

    @Test
    void uploadPngImage_shouldReturn201() throws Exception {
        var png = createPngFile("test-logo.png", 800, 600);
        var result = mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(png)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.originalName").value("test-logo.png"))
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andExpect(jsonPath("$.width").value(800))
                .andExpect(jsonPath("$.height").value(600))
                .andExpect(jsonPath("$.url").value(containsString("/file")))
                .andExpect(jsonPath("$.thumbnailUrl").value(containsString("/thumbnail")))
                .andReturn();

        var assetId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        // Verify the file is accessible
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/file"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("public")));

        // Verify thumbnail is accessible
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/thumbnail"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadFileTooLarge_shouldReturn400() throws Exception {
        var largeFile = new MockMultipartFile(
                "file", "large.bin", "image/png",
                new byte[6_000_000] // >5MB
        );
        mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(largeFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadDisallowedType_shouldReturn400() throws Exception {
        var exe = new MockMultipartFile(
                "file", "virus.exe", "application/x-msdownload",
                "fake exe content".getBytes()
        );
        mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(exe)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listAssetsForTenant_shouldReturnOnlyTenantAssets() throws Exception {
        // Upload 2 images to tenant
        var png1 = createPngFile("img1.png", 100, 100);
        var png2 = createPngFile("img2.png", 200, 200);
        mockMvc.perform(multipart("/api/v1/assets/upload").file(png1)
                .header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(multipart("/api/v1/assets/upload").file(png2)
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(get("/api/v1/assets/tenant/" + tenant.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void clientListingOtherTenant_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/assets/tenant/" + otherTenant.getId())
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteAsset_shouldReturn204() throws Exception {
        var png = createPngFile("delete-me.png", 50, 50);
        var result = mockMvc.perform(multipart("/api/v1/assets/upload").file(png)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        var assetId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(delete("/api/v1/assets/" + assetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Accessing deleted asset should return 404
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/file"))
                .andExpect(status().isNotFound());
    }

    @Test
    void accessWithoutAuth_shouldReturn401() throws Exception {
        mockMvc.perform(multipart("/api/v1/assets/upload")
                        .file(createPngFile("test.png", 10, 10)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/assets/tenant/" + tenant.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void serveFilePubliclyWithoutAuth_shouldReturn200() throws Exception {
        var png = createPngFile("public.png", 300, 200);
        var result = mockMvc.perform(multipart("/api/v1/assets/upload").file(png)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andReturn();

        var assetId = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asText();

        // No auth required for serving files
        mockMvc.perform(get("/api/v1/assets/" + assetId + "/file"))
                .andExpect(status().isOk());
    }

    @Test
    void serveNonexistentAsset_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/assets/" + UUID.randomUUID() + "/file"))
                .andExpect(status().isNotFound());
    }

    // --- Helpers ---

    private MockMultipartFile createPngFile(String name, int width, int height) {
        // Create a minimal valid PNG with ImageIO
        var image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(java.awt.Color.RED);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        try {
            var baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            return new MockMultipartFile("file", name, "image/png", baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Error creating test PNG", e);
        }
    }
}
