package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.auth.domain.BusinessSector;
import com.amg.digitalitzacio.auth.domain.Tenant;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.content.domain.*;
import com.amg.digitalitzacio.social.application.SocialContentGeneratorService;
import com.amg.digitalitzacio.social.application.TelegramMediaUploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentPlannerFlowServiceTest {

    @Mock ContentPlanItemRepository itemRepository;
    @Mock ContentPlanRepository planRepository;
    @Mock TenantRepository tenantRepository;
    @Mock TelegramMediaUploadService mediaUploadService;
    @Mock SocialContentGeneratorService contentGenerator;
    @Mock ContentPlanPublishService publishService;
    @Mock TelegramBotClient telegramBotClient;

    @InjectMocks ContentPlannerFlowService service;

    private final UUID TENANT = UUID.randomUUID();
    private final Long CHAT = 123L;

    private ContentPlanItem awaitingPhotoItem() {
        return ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(TENANT).planId(UUID.randomUUID())
                .pillar(ContentPillar.NOVELTY).briefText("Foto novetat").contentLanguage("ca")
                .networks("INSTAGRAM").status(ContentItemStatus.PHOTO_REQUESTED).build();
    }

    @Test
    void handleIncomingPhoto_pendingItem_generatesCaptionAndAsksApproval() {
        ContentPlanItem item = awaitingPhotoItem();
        when(itemRepository.findByTenantIdAndStatus(TENANT, ContentItemStatus.PHOTO_REQUESTED)).thenReturn(List.of(item));
        when(mediaUploadService.downloadAndUpload("file1", TENANT)).thenReturn("/uploaded.jpg");
        when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(
                Tenant.builder().name("Botiga X").sector(BusinessSector.ESTETICA).build()));
        when(contentGenerator.generateCaption(anyString(), anyString(), anyString())).thenReturn("Caption IA ✨");
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean handled = service.handleIncomingPhoto(CHAT, TENANT, "file1");

        assertThat(handled).isTrue();
        assertThat(item.getStatus()).isEqualTo(ContentItemStatus.AWAITING_APPROVAL);
        assertThat(item.getMediaUrl()).isEqualTo("/uploaded.jpg");
        assertThat(item.getCaption()).isEqualTo("Caption IA ✨");
        verify(telegramBotClient).sendMessageWithButtons(eq(CHAT), anyString(), anyList());
    }

    @Test
    void handleIncomingPhoto_noPendingItem_returnsFalse() {
        when(itemRepository.findByTenantIdAndStatus(TENANT, ContentItemStatus.PHOTO_REQUESTED)).thenReturn(List.of());
        assertThat(service.handleIncomingPhoto(CHAT, TENANT, "file1")).isFalse();
        verifyNoInteractions(mediaUploadService);
    }

    @Test
    void approve_ownedAwaitingItem_publishesAndConfirms() {
        UUID itemId = UUID.randomUUID();
        ContentPlanItem item = ContentPlanItem.builder().id(itemId).tenantId(TENANT)
                .status(ContentItemStatus.AWAITING_APPROVAL).networks("INSTAGRAM").build();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        ContentPlanItem published = ContentPlanItem.builder().id(itemId).tenantId(TENANT)
                .status(ContentItemStatus.PUBLISHED).build();
        when(publishService.publishItem(item)).thenReturn(published);

        service.approve(CHAT, TENANT, itemId);

        verify(publishService).publishItem(item);
        verify(telegramBotClient).sendMessage(eq(CHAT), contains("Publicat"));
    }

    @Test
    void approve_crossTenant_doesNotPublish() {
        UUID itemId = UUID.randomUUID();
        ContentPlanItem otherTenantItem = ContentPlanItem.builder().id(itemId).tenantId(UUID.randomUUID())
                .status(ContentItemStatus.AWAITING_APPROVAL).build();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(otherTenantItem));

        service.approve(CHAT, TENANT, itemId);

        verifyNoInteractions(publishService);
        verify(telegramBotClient).sendMessage(eq(CHAT), contains("ja no està pendent"));
    }

    @Test
    void rewrite_regeneratesCaptionAndAsksApprovalAgain() {
        UUID itemId = UUID.randomUUID();
        ContentPlanItem item = ContentPlanItem.builder().id(itemId).tenantId(TENANT).planId(UUID.randomUUID())
                .pillar(ContentPillar.SHOP).briefText("El local").contentLanguage("ca")
                .status(ContentItemStatus.AWAITING_APPROVAL).build();
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(tenantRepository.findById(TENANT)).thenReturn(Optional.of(Tenant.builder().name("X").build()));
        when(contentGenerator.generateCaption(anyString(), anyString(), anyString())).thenReturn("Nou caption");
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.rewrite(CHAT, TENANT, itemId);

        assertThat(item.getCaption()).isEqualTo("Nou caption");
        verify(telegramBotClient).sendMessageWithButtons(eq(CHAT), anyString(), anyList());
    }
}
