package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.google.domain.GoogleModuleConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import com.amg.digitalitzacio.vault.application.VaultEncryption;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialPublisherOrchestratorTest {

    @Mock StringRedisTemplate redis;
    @Mock @SuppressWarnings("unchecked") ValueOperations<String, String> opsForValue;
    @Mock TelegramBotClient telegramBotClient;
    @Mock TenantRepository tenantRepository;
    @Mock SocialMetaConfigRepository metaConfigRepo;
    @Mock SocialPostRepository postRepository;
    @Mock GoogleModuleConfigRepository googleConfigRepo;
    @Mock VaultEncryption vaultEncryption;
    @Mock SocialContentGeneratorService contentGenerator;
    @Mock InstagramPublisherService instagramPublisher;
    @Mock FacebookPublisherService facebookPublisher;
    @Mock GoogleBusinessPublisherService googleBusinessPublisher;
    @Mock LinkedInPublisherService linkedInPublisher;
    @Mock TelegramMediaUploadService telegramMediaUploadService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String VIDEO_URL = "https://cdn.example.com/clip.mp4?token=abc";
    private static final String PHOTO_URL = "https://cdn.example.com/foto.jpg?token=abc";

    private SocialPublisherOrchestrator orchestrator() {
        return new SocialPublisherOrchestrator(redis, objectMapper, telegramBotClient,
                tenantRepository, metaConfigRepo, postRepository, googleConfigRepo,
                vaultEncryption, contentGenerator, instagramPublisher, facebookPublisher,
                googleBusinessPublisher, linkedInPublisher, telegramMediaUploadService);
    }

    private void stubMetaConfig() {
        var mc = SocialMetaConfig.builder()
                .tenantId(TENANT_ID)
                .facebookPageId("fb-page-1")
                .instagramAccountId("ig-acc-1")
                .pageAccessTokenEncrypted("enc")
                .build();
        when(metaConfigRepo.findByTenantId(TENANT_ID)).thenReturn(Optional.of(mc));
        when(vaultEncryption.decrypt("enc")).thenReturn("token");
    }

    private SocialPost post(String network, String postType, String mediaUrl) {
        return SocialPost.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .network(network)
                .postType(postType)
                .caption("caption")
                .mediaUrl(mediaUrl)
                .status("SCHEDULED")
                .build();
    }

    @Test
    void reelInstagramEncaminaAPublishReel() {
        stubMetaConfig();
        when(instagramPublisher.publishReel(any(), any(), any(), any())).thenReturn("ext-1");

        var p = post("INSTAGRAM", "REEL", VIDEO_URL);
        orchestrator().publishNow(p);

        verify(instagramPublisher).publishReel("ig-acc-1", "token", VIDEO_URL, "caption");
        verify(instagramPublisher, never()).publishFeedPhoto(any(), any(), any(), any());
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void storyInstagramDetectaVideoPerExtensio() {
        stubMetaConfig();
        when(instagramPublisher.publishStory(any(), any(), any(), anyBoolean())).thenReturn("ext-2");

        orchestrator().publishNow(post("INSTAGRAM", "STORY", VIDEO_URL));

        // .mp4 abans de la query string → isVideo = true
        verify(instagramPublisher).publishStory("ig-acc-1", "token", VIDEO_URL, true);
    }

    @Test
    void storyInstagramFotoNoEsVideo() {
        stubMetaConfig();
        when(instagramPublisher.publishStory(any(), any(), any(), anyBoolean())).thenReturn("ext-3");

        orchestrator().publishNow(post("INSTAGRAM", "STORY", PHOTO_URL));

        verify(instagramPublisher).publishStory("ig-acc-1", "token", PHOTO_URL, false);
    }

    @Test
    void reelFacebookEncaminaAPublishVideo() {
        stubMetaConfig();
        when(facebookPublisher.publishVideo(any(), any(), any(), any())).thenReturn("ext-4");

        var p = post("FACEBOOK", "REEL", VIDEO_URL);
        orchestrator().publishNow(p);

        verify(facebookPublisher).publishVideo("fb-page-1", "token", VIDEO_URL, "caption");
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void storyFacebookFotoEncaminaAPhotoStory() {
        stubMetaConfig();
        when(facebookPublisher.publishPhotoStory(any(), any(), any())).thenReturn("ext-5");

        orchestrator().publishNow(post("FACEBOOK", "STORY", PHOTO_URL));

        verify(facebookPublisher).publishPhotoStory("fb-page-1", "token", PHOTO_URL);
    }

    @Test
    void storyFacebookVideoNoSuportadaMarcaFailed() {
        stubMetaConfig();

        var p = post("FACEBOOK", "STORY", VIDEO_URL);
        orchestrator().publishNow(p);

        assertThat(p.getStatus()).isEqualTo("FAILED");
        assertThat(p.getErrorMessage()).contains("stories de vídeo");
        verify(facebookPublisher, never()).publishPhotoStory(any(), any(), any());
    }

    @Test
    void reelGoogleBusinessNoSuportatMarcaFailed() {
        var p = post("GOOGLE_BUSINESS", "REEL", VIDEO_URL);
        orchestrator().publishNow(p);

        assertThat(p.getStatus()).isEqualTo("FAILED");
        verifyNoInteractions(googleBusinessPublisher);
    }

    @Test
    void reelSenseMediaFalla() {
        stubMetaConfig();

        var p = post("INSTAGRAM", "REEL", null);
        orchestrator().publishNow(p);

        assertThat(p.getStatus()).isEqualTo("FAILED");
        verify(instagramPublisher, never()).publishReel(any(), any(), any(), any());
    }

    @Test
    void fotoInstagramSegueixFuncionant() {
        stubMetaConfig();
        when(instagramPublisher.publishFeedPhoto(any(), any(), any(), any())).thenReturn("ext-6");

        var p = post("INSTAGRAM", "PHOTO", PHOTO_URL);
        orchestrator().publishNow(p);

        verify(instagramPublisher).publishFeedPhoto("ig-acc-1", "token", PHOTO_URL, "caption");
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void textFacebookSenseMediaPublicaText() {
        stubMetaConfig();
        when(facebookPublisher.publishText(any(), any(), any())).thenReturn("ext-7");

        var p = post("FACEBOOK", "TEXT", null);
        orchestrator().publishNow(p);

        verify(facebookPublisher).publishText("fb-page-1", "token", "caption");
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void captionAmbLinkRepUtmDeLaXarxa() {
        stubMetaConfig();
        when(facebookPublisher.publishText(any(), any(), any())).thenReturn("ext-8");

        var p = post("FACEBOOK", "TEXT", null);
        p.setCaption("Reserva a https://canarebecca.webs.amgdl.com ara");
        orchestrator().publishNow(p);

        verify(facebookPublisher).publishText(eq("fb-page-1"), eq("token"),
            eq("Reserva a https://canarebecca.webs.amgdl.com?utm_source=facebook&utm_medium=social&utm_campaign=amg_social ara"));
    }

    @Test
    void carruselInstagramEncaminaAPublishCarousel() {
        stubMetaConfig();
        when(instagramPublisher.publishCarousel(any(), any(), any(), any())).thenReturn("ext-9");

        var p = post("INSTAGRAM", "CAROUSEL",
            "https://cdn.example.com/1.jpg|https://cdn.example.com/2.jpg|https://cdn.example.com/3.jpg");
        orchestrator().publishNow(p);

        verify(instagramPublisher).publishCarousel(
            eq("ig-acc-1"), eq("token"),
            eq(java.util.List.of("https://cdn.example.com/1.jpg",
                                 "https://cdn.example.com/2.jpg",
                                 "https://cdn.example.com/3.jpg")),
            anyString());
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void carruselSenseMediaFalla() {
        stubMetaConfig();

        var p = post("INSTAGRAM", "CAROUSEL", null);
        orchestrator().publishNow(p);

        assertThat(p.getStatus()).isEqualTo("FAILED");
        verify(instagramPublisher, never()).publishCarousel(any(), any(), any(), any());
    }

    @Test
    void linkFacebookEncaminaAPublishLink() {
        stubMetaConfig();
        when(facebookPublisher.publishLink(any(), any(), any(), any())).thenReturn("ext-10");

        var p = post("FACEBOOK", "LINK", "https://amgdl.com");
        orchestrator().publishNow(p);

        verify(facebookPublisher).publishLink(eq("fb-page-1"), eq("token"),
            eq("https://amgdl.com"), anyString());
        assertThat(p.getStatus()).isEqualTo("PUBLISHED");
    }

    // ─── P25: avís caption massa llarg per xarxa ─────────────────────────────

    @Test
    void captionExcessivamentLlargAInstagram_avisaAvansDeMedia() {
        String captionLlarg = "A".repeat(2201); // > 2200 limit IG
        Long chatId = 210L;
        stubFlowDraft(chatId, new java.util.HashMap<>(java.util.Map.of(
            "tenantId", TENANT_ID.toString(),
            "step", "AWAIT_CAPTION",
            "ig", "1",
            "postType", "TEXT"
        )));

        orchestrator().handleStep(chatId, captionLlarg, null, null);

        // Ha d'avisar del límit de caràcters
        verify(telegramBotClient, atLeastOnce()).sendMessage(eq(chatId),
            argThat(msg -> msg.contains("2.200") || msg.contains("2200")));
    }

    // ─── P20: editar caption a AWAIT_CONFIRM ─────────────────────────────────

    private void stubFlowDraft(Long chatId, Map<String, String> fields) {
        try {
            var draft = new java.util.HashMap<>(fields);
            String json = objectMapper.writeValueAsString(draft);
            when(redis.opsForValue()).thenReturn(opsForValue);
            when(opsForValue.get("social:draft:" + chatId)).thenReturn(json);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void handleConfirmEditar_permetModificarCaptionSenseCancellar() {
        Long chatId = 200L;
        stubFlowDraft(chatId, Map.of(
            "tenantId", TENANT_ID.toString(),
            "step", "AWAIT_CONFIRM",
            "ig", "1",
            "postType", "PHOTO",
            "caption", "Caption original",
            "mediaUrl", PHOTO_URL
        ));

        orchestrator().handleStep(chatId, "EDITAR", null, null);

        verify(telegramBotClient).sendMessage(eq(chatId),
            argThat(msg -> msg.contains("Caption original")));
        verify(redis, never()).delete(any(String.class));
        verify(postRepository, never()).save(any());
        var captor = ArgumentCaptor.forClass(String.class);
        verify(opsForValue).set(eq("social:draft:" + chatId), captor.capture(), anyLong(), any());
        assertThat(captor.getValue()).contains("AWAIT_CAPTION");
    }

    // ─── P21: recuperació de draft pendent ───────────────────────────────────

    @Test
    void startFlowAmbDraftPendent_mostraOpcionsResume() {
        Long chatId = 201L;
        when(redis.hasKey("social:draft:" + chatId)).thenReturn(true);
        stubFlowDraft(chatId, Map.of(
            "tenantId", TENANT_ID.toString(),
            "step", "AWAIT_CAPTION",
            "ig", "1",
            "postType", "PHOTO"
        ));

        orchestrator().startFlow(TENANT_ID, chatId);

        verify(telegramBotClient).sendMessage(eq(chatId),
            argThat(msg -> msg.contains("CONTINUA") && msg.contains("DESCARTAR")));
        verify(tenantRepository, never()).findById(any());
    }

    @Test
    void handleResumeContinua_restoreElPasAnterior() {
        Long chatId = 202L;
        stubFlowDraft(chatId, Map.of(
            "tenantId", TENANT_ID.toString(),
            "step", "AWAIT_RESUME",
            "prevStep", "AWAIT_CAPTION",
            "ig", "1",
            "postType", "PHOTO"
        ));

        orchestrator().handleStep(chatId, "CONTINUA", null, null);

        verify(telegramBotClient).sendMessage(eq(chatId),
            argThat(msg -> msg.contains("Reprenent")));
        var captor = ArgumentCaptor.forClass(String.class);
        verify(opsForValue).set(eq("social:draft:" + chatId), captor.capture(), anyLong(), any());
        assertThat(captor.getValue()).contains("AWAIT_CAPTION");
        assertThat(captor.getValue()).doesNotContain("AWAIT_RESUME");
        assertThat(captor.getValue()).doesNotContain("prevStep");
    }

    @Test
    void handleResumeDescartar_eliminaElDraft() {
        Long chatId = 203L;
        stubFlowDraft(chatId, Map.of(
            "tenantId", TENANT_ID.toString(),
            "step", "AWAIT_RESUME",
            "prevStep", "AWAIT_MEDIA",
            "ig", "1",
            "postType", "PHOTO"
        ));

        orchestrator().handleStep(chatId, "DESCARTAR", null, null);

        verify(redis).delete("social:draft:" + chatId);
        verify(telegramBotClient).sendMessage(eq(chatId),
            argThat(msg -> msg.contains("/publica")));
    }
}
