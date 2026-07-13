package com.amg.digitalitzacio.content.application;

import com.amg.digitalitzacio.content.domain.ContentItemStatus;
import com.amg.digitalitzacio.content.domain.ContentPlanItem;
import com.amg.digitalitzacio.content.domain.ContentPlanItemRepository;
import com.amg.digitalitzacio.social.application.SocialPublisherOrchestrator;
import com.amg.digitalitzacio.social.domain.SocialPost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContentPlanPublishServiceTest {

    @Mock SocialPublisherOrchestrator orchestrator;
    @Mock ContentPlanItemRepository itemRepository;
    @InjectMocks ContentPlanPublishService service;

    private ContentPlanItem item(String networks) {
        return ContentPlanItem.builder().id(UUID.randomUUID()).tenantId(UUID.randomUUID())
                .networks(networks).caption("hola").mediaUrl("/x.jpg")
                .status(ContentItemStatus.PHOTO_RECEIVED).build();
    }

    @Test
    void allNetworksOk_itemPublished() {
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> { ((SocialPost) inv.getArgument(0)).setStatus("PUBLISHED"); return null; })
                .when(orchestrator).publishNow(any());

        ContentPlanItem it = item("INSTAGRAM,FACEBOOK");
        service.publishItem(it);

        assertThat(it.getStatus()).isEqualTo(ContentItemStatus.PUBLISHED);
        verify(orchestrator, times(2)).publishNow(any());
    }

    @Test
    void oneNetworkFails_itemFailedWithError() {
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doAnswer(inv -> {
            SocialPost p = inv.getArgument(0);
            if ("GOOGLE_BUSINESS".equals(p.getNetwork())) { p.setStatus("FAILED"); p.setErrorMessage("sense location"); }
            else p.setStatus("PUBLISHED");
            return null;
        }).when(orchestrator).publishNow(any());

        ContentPlanItem it = item("INSTAGRAM,GOOGLE_BUSINESS");
        service.publishItem(it);

        assertThat(it.getStatus()).isEqualTo(ContentItemStatus.FAILED);
        assertThat(it.getError()).isEqualTo("sense location");
    }

    @Test
    void noNetworks_failedWithoutPublishing() {
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ContentPlanItem it = item(null);
        service.publishItem(it);
        assertThat(it.getStatus()).isEqualTo(ContentItemStatus.FAILED);
        verifyNoInteractions(orchestrator);
    }
}
