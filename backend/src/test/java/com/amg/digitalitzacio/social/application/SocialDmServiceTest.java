package com.amg.digitalitzacio.social.application;

import com.amg.digitalitzacio.agents.application.ConversationalAgentService;
import com.amg.digitalitzacio.agents.application.TelegramBotClient;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.social.domain.SocialMetaConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialDmServiceTest {

    @Mock SocialMetaConfigRepository metaConfigRepo;
    @Mock SocialFeatureService featureService;
    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock TelegramBotClient telegramBotClient;
    @Mock ConversationalAgentService agentService;
    @Mock MetaMessagingChannel messagingChannel;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final long OWNER_CHAT = 111L;
    private static final long ATTACKER_CHAT = 999L;
    private static final String CTX_ID = "abc12345";

    private SocialDmService service() {
        return new SocialDmService(metaConfigRepo, featureService, chatLinkRepository,
                telegramBotClient, agentService, messagingChannel, redis, objectMapper);
    }

    private void stubContext() {
        when(redis.opsForValue()).thenReturn(valueOps);
        var ctx = objectMapper.createObjectNode();
        ctx.put("tenantId", TENANT_ID.toString());
        ctx.put("chatId", OWNER_CHAT);
        ctx.put("channel", ConversationChannel.INSTAGRAM.name());
        ctx.put("recipientId", "recipient-1");
        ctx.put("draft", "Resposta preparada");
        try {
            when(valueOps.get("dm:ctx:" + CTX_ID)).thenReturn(objectMapper.writeValueAsString(ctx));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Test
    void approveDraft_ownerChat_sends() {
        stubContext();
        when(messagingChannel.sendMessage(eq(TENANT_ID), eq("recipient-1"), anyString())).thenReturn(true);

        String result = service().approveDraft(OWNER_CHAT, CTX_ID);

        assertThat(result).contains("enviat");
        verify(messagingChannel).sendMessage(TENANT_ID, "recipient-1", "Resposta preparada");
    }

    @Test
    void approveDraft_foreignChat_rejectedNoSend() {
        stubContext();

        String result = service().approveDraft(ATTACKER_CHAT, CTX_ID);

        assertThat(result).contains("no és accessible");
        verifyNoInteractions(messagingChannel);
    }

    @Test
    void startManualReply_foreignChat_doesNotArmPending() {
        stubContext();

        service().startManualReply(ATTACKER_CHAT, CTX_ID);

        verify(valueOps, never()).set(startsWith("dm:pending:"), anyString(), anyLong(), any());
    }

    @Test
    void startManualReply_ownerChat_armsPending() {
        stubContext();

        service().startManualReply(OWNER_CHAT, CTX_ID);

        verify(valueOps).set(eq("dm:pending:" + OWNER_CHAT), eq(CTX_ID), anyLong(), any());
    }

    @Test
    void submitManualReply_foreignContext_rejectedNoSend() {
        when(redis.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("dm:pending:" + ATTACKER_CHAT)).thenReturn(CTX_ID);
        var ctx = objectMapper.createObjectNode();
        ctx.put("tenantId", TENANT_ID.toString());
        ctx.put("chatId", OWNER_CHAT);
        ctx.put("recipientId", "recipient-1");
        try {
            when(valueOps.get("dm:ctx:" + CTX_ID)).thenReturn(objectMapper.writeValueAsString(ctx));
        } catch (Exception e) { throw new RuntimeException(e); }

        String result = service().submitManualReply(ATTACKER_CHAT, "text arbitrari");

        assertThat(result).contains("no és accessible");
        verifyNoInteractions(messagingChannel);
    }
}
