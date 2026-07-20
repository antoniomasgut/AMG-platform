package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialPost;
import com.amg.digitalitzacio.social.domain.SocialPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialSchedulerJobTest {

    @Mock SocialPostRepository postRepository;
    @Mock SocialPublisherOrchestrator orchestrator;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TelegramBotClient telegramBotClient;

    private static final UUID TENANT_ID = UUID.randomUUID();

    private SocialSchedulerJob job() {
        return new SocialSchedulerJob(postRepository, orchestrator, chatLinkRepository, telegramBotClient);
    }

    private SocialPost duePost() {
        return SocialPost.builder()
                .id(UUID.randomUUID()).tenantId(TENANT_ID)
                .network("INSTAGRAM").postType("PHOTO")
                .caption("Post programat").status("SCHEDULED")
                .build();
    }

    private void stubChatLink() {
        var link = new TenantChatLink();
        link.setTenantId(TENANT_ID);
        link.setTelegramChatId(555L);
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.of(link));
    }

    @Test
    void postProgramatQueFallaAvisaElTenant() {
        var post = duePost();
        when(postRepository.findDueScheduled(any())).thenReturn(List.of(post));
        stubChatLink();
        doAnswer(inv -> {
            SocialPost p = inv.getArgument(0);
            p.setStatus("FAILED");
            p.setErrorMessage("Meta API error: token caducat");
            return null;
        }).when(orchestrator).publishNow(any());

        job().publishScheduledPosts();

        verify(telegramBotClient).sendMessage(eq(555L),
            argThat(msg -> msg.contains("No s'ha pogut publicar")
                        && msg.contains("Instagram")
                        && msg.contains("token caducat")));
    }

    @Test
    void postPublicatCorrectamentNoAvisa() {
        var post = duePost();
        when(postRepository.findDueScheduled(any())).thenReturn(List.of(post));
        doAnswer(inv -> {
            ((SocialPost) inv.getArgument(0)).setStatus("PUBLISHED");
            return null;
        }).when(orchestrator).publishNow(any());

        job().publishScheduledPosts();

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void fallaSenseChatLinkNoPeta() {
        var post = duePost();
        when(postRepository.findDueScheduled(any())).thenReturn(List.of(post));
        when(chatLinkRepository.findByTenantId(TENANT_ID)).thenReturn(Optional.empty());
        doAnswer(inv -> {
            ((SocialPost) inv.getArgument(0)).setStatus("FAILED");
            return null;
        }).when(orchestrator).publishNow(any());

        job().publishScheduledPosts();

        verify(telegramBotClient, never()).sendMessage(anyLong(), anyString());
    }
}
