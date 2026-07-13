package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.assets.application.AssetOrchestrator;
import com.amg.digitalitzacio.auth.domain.TenantRepository;
import com.amg.digitalitzacio.content.api.dto.CreatePlanRequest;
import com.amg.digitalitzacio.content.api.dto.UpdateItemRequest;
import com.amg.digitalitzacio.content.domain.*;
import com.amg.digitalitzacio.shared.exception.ResourceNotFoundException;
import com.amg.digitalitzacio.shared.security.UserPrincipal;
import com.amg.digitalitzacio.social.domain.SocialMetaConfig;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentPlanServiceTest {

    @Mock ContentPlanRepository planRepository;
    @Mock ContentPlanItemRepository itemRepository;
    @Mock TenantRepository tenantRepository;
    @Mock SocialMetaConfigRepository metaConfigRepository;
    @Mock AssetOrchestrator assetOrchestrator;

    @InjectMocks ContentPlanService service;

    private final UUID TENANT = UUID.randomUUID();
    private final UUID OTHER_TENANT = UUID.randomUUID();
    private final UserPrincipal admin = new UserPrincipal(UUID.randomUUID(), "a@amg.com", "SUPER_ADMIN", null);
    private final UserPrincipal client = new UserPrincipal(UUID.randomUUID(), "c@x.com", "CLIENT", TENANT);

    private void stubSaves() {
        when(planRepository.save(any())).thenAnswer(inv -> {
            ContentPlan p = inv.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.findByPlanIdOrderByWeekNumberAsc(any())).thenReturn(List.of());
    }

    @Test
    void createPlanWithGenerate_creates4ItemsWithRotatedPillars() {
        stubSaves();
        when(tenantRepository.existsById(TENANT)).thenReturn(true);
        when(planRepository.findByTenantIdAndPeriod(TENANT, "2026-08")).thenReturn(Optional.empty());
        when(metaConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());

        service.createPlan(TENANT, new CreatePlanRequest("2026-08", null, true, null), admin);

        ArgumentCaptor<ContentPlanItem> cap = ArgumentCaptor.forClass(ContentPlanItem.class);
        verify(itemRepository, times(4)).save(cap.capture());
        assertThat(cap.getAllValues()).extracting(ContentPlanItem::getPillar)
                .containsExactly(ContentPillar.NOVELTY, ContentPillar.COMBINE,
                                 ContentPillar.SHOP, ContentPillar.SOCIAL_PROOF);
        assertThat(cap.getAllValues()).allSatisfy(i -> {
            assertThat(i.getStatus()).isEqualTo(ContentItemStatus.PLANNED);
            assertThat(i.getBriefText()).isNotBlank();
            assertThat(i.getPhotoDeadline()).isNotNull();
        });
    }

    @Test
    void createPlan_inheritsTenantDefaultLanguage() {
        stubSaves();
        when(tenantRepository.existsById(TENANT)).thenReturn(true);
        when(planRepository.findByTenantIdAndPeriod(any(), any())).thenReturn(Optional.empty());
        when(metaConfigRepository.findByTenantId(TENANT)).thenReturn(
                Optional.of(SocialMetaConfig.builder().tenantId(TENANT).defaultContentLanguage("es").build()));

        service.createPlan(TENANT, new CreatePlanRequest("2026-08", null, false, null), admin);

        ArgumentCaptor<ContentPlan> cap = ArgumentCaptor.forClass(ContentPlan.class);
        verify(planRepository).save(cap.capture());
        assertThat(cap.getValue().getContentLanguage()).isEqualTo("es");
    }

    @Test
    void createPlan_explicitLanguageOverridesDefault() {
        stubSaves();
        when(tenantRepository.existsById(TENANT)).thenReturn(true);
        when(planRepository.findByTenantIdAndPeriod(any(), any())).thenReturn(Optional.empty());

        service.createPlan(TENANT, new CreatePlanRequest("2026-08", "en", false, null), admin);

        ArgumentCaptor<ContentPlan> cap = ArgumentCaptor.forClass(ContentPlan.class);
        verify(planRepository).save(cap.capture());
        assertThat(cap.getValue().getContentLanguage()).isEqualTo("en");
    }

    @Test
    void createPlan_duplicatePeriod_throws() {
        when(tenantRepository.existsById(TENANT)).thenReturn(true);
        when(planRepository.findByTenantIdAndPeriod(TENANT, "2026-08"))
                .thenReturn(Optional.of(ContentPlan.builder().build()));

        assertThatThrownBy(() -> service.createPlan(TENANT, new CreatePlanRequest("2026-08", null, false, null), admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPlan_invalidPeriodFormat_throws() {
        when(tenantRepository.existsById(TENANT)).thenReturn(true);
        assertThatThrownBy(() -> service.createPlan(TENANT, new CreatePlanRequest("agost", null, false, null), admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPlan_crossTenantClient_throwsNotFound() {
        when(planRepository.findById(any())).thenReturn(
                Optional.of(ContentPlan.builder().id(UUID.randomUUID()).tenantId(OTHER_TENANT).build()));
        assertThatThrownBy(() -> service.getPlan(UUID.randomUUID(), client))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItem_crossTenantClient_throwsNotFound() {
        when(itemRepository.findById(any())).thenReturn(
                Optional.of(ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(OTHER_TENANT).build()));
        assertThatThrownBy(() -> service.updateItem(UUID.randomUUID(),
                new UpdateItemRequest(null, "x", null, null, null, null, null, null), client))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPending_returnsOnlyPhotoRequestedWithoutMedia() {
        ContentPlanItem noPhoto = ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(TENANT)
                .pillar(ContentPillar.NOVELTY).status(ContentItemStatus.PHOTO_REQUESTED).build();
        ContentPlanItem withPhoto = ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(TENANT)
                .pillar(ContentPillar.SHOP).status(ContentItemStatus.PHOTO_REQUESTED).mediaUrl("/x.jpg").build();
        when(itemRepository.findByTenantIdAndStatus(TENANT, ContentItemStatus.PHOTO_REQUESTED))
                .thenReturn(List.of(noPhoto, withPhoto));

        var pending = service.getPending(TENANT, client);
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).id()).isEqualTo(noPhoto.getId());
    }

    @Test
    void setDefaultLanguage_invalid_throws() {
        assertThatThrownBy(() -> service.setDefaultLanguage(TENANT, "fr", admin))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setDefaultLanguage_valid_upsertsConfig() {
        when(metaConfigRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(metaConfigRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = service.setDefaultLanguage(TENANT, "de", admin);

        assertThat(result).isEqualTo("de");
        ArgumentCaptor<SocialMetaConfig> cap = ArgumentCaptor.forClass(SocialMetaConfig.class);
        verify(metaConfigRepository).save(cap.capture());
        assertThat(cap.getValue().getDefaultContentLanguage()).isEqualTo("de");
    }

    @Test
    void activate_setsOtherActivePlansToDone() {
        UUID planId = UUID.randomUUID();
        ContentPlan target = ContentPlan.builder().id(planId).tenantId(TENANT).status(ContentPlanStatus.DRAFT).build();
        ContentPlan otherActive = ContentPlan.builder().id(UUID.randomUUID()).tenantId(TENANT).status(ContentPlanStatus.ACTIVE).build();
        when(planRepository.findById(planId)).thenReturn(Optional.of(target));
        when(planRepository.findByTenantIdAndStatus(TENANT, ContentPlanStatus.ACTIVE)).thenReturn(List.of(otherActive));
        when(planRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.findByPlanIdOrderByWeekNumberAsc(any())).thenReturn(List.of());

        service.activate(planId, admin);

        assertThat(otherActive.getStatus()).isEqualTo(ContentPlanStatus.DONE);
        assertThat(target.getStatus()).isEqualTo(ContentPlanStatus.ACTIVE);
    }
}
