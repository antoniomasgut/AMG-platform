package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.AgentMode;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.TenantAIConfig;
import com.amg.digitalitzacio.agents.domain.TenantAIConfigRepository;
import com.amg.digitalitzacio.agents.domain.TenantChatLink;
import com.amg.digitalitzacio.agents.domain.TenantChatLinkRepository;
import com.amg.digitalitzacio.agents.application.channel.EmailChannel;
import com.amg.digitalitzacio.shared.ai.AIProvider;
import com.amg.digitalitzacio.shared.ai.AIProviderRouter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InboundAssistServiceTest {

    @Mock TenantChatLinkRepository chatLinkRepository;
    @Mock ConversationalAgentService agentService;
    @Mock ConversationService conversationService;
    @Mock TenantAIConfigRepository aiConfigRepository;
    @Mock TelegramBotClient telegramBotClient;
    @Mock EmailChannel emailChannel;
    @Mock com.amg.digitalitzacio.agents.application.channel.WhatsAppChannel whatsAppChannel;
    @Mock com.amg.digitalitzacio.agents.application.channel.WhatsAppMetaChannel whatsAppMetaChannel;
    @Mock AIProviderRouter aiRouter;
    @Mock AIProvider aiProvider;
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    InboundAssistService service;
    final Map<String, String> store = new HashMap<>();

    final UUID TENANT = UUID.randomUUID();
    final Long CHAT = 123L;

    @BeforeEach
    void setUp() {
        service = new InboundAssistService(chatLinkRepository, agentService, conversationService,
                aiConfigRepository, telegramBotClient, emailChannel, whatsAppChannel, whatsAppMetaChannel,
                aiRouter, redis, new ObjectMapper());
        when(aiConfigRepository.findById(any())).thenReturn(Optional.of(TenantAIConfig.defaultFor(TENANT)));
        when(redis.opsForValue()).thenReturn(valueOps);
        doAnswer(i -> { store.put(i.getArgument(0), i.getArgument(1)); return null; })
                .when(valueOps).set(anyString(), anyString(), anyLong(), any());
        when(valueOps.get(anyString())).thenAnswer(i -> store.get(i.getArgument(0)));
        when(redis.hasKey(anyString())).thenAnswer(i -> store.containsKey(i.getArgument(0)));
        when(redis.delete(anyString())).thenAnswer(i -> store.remove(i.getArgument(0)) != null);
    }

    private TenantChatLink hybridLink() {
        TenantChatLink l = mock(TenantChatLink.class);
        when(l.getIsActive()).thenReturn(true);
        when(l.getAgentMode()).thenReturn(AgentMode.HYBRID);
        when(l.getTelegramChatId()).thenReturn(CHAT);
        return l;
    }

    /** Fa un intake HYBRID i retorna l'id del context creat. */
    private String intakeAndGetId() {
        TenantChatLink link = hybridLink();
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.of(link));
        when(agentService.generateDmDraft(eq(TENANT), anyString(), eq(ConversationChannel.EMAIL), anyString()))
                .thenReturn("Esborrany IA");
        boolean handled = service.tryIntake(TENANT, ConversationChannel.EMAIL, "client@x.com", "Client", "Hola?");
        assertThat(handled).isTrue();
        return store.keySet().stream().filter(k -> k.startsWith("ia:ctx:")).findFirst().orElseThrow().substring("ia:ctx:".length());
    }

    @Test
    void tryIntake_hybrid_storesContextAndSendsButtons() {
        intakeAndGetId();
        verify(telegramBotClient).sendMessageWithButtons(eq(CHAT), anyString(), anyList());
        assertThat(store.keySet()).anyMatch(k -> k.startsWith("ia:ctx:"));
    }

    @Test
    void tryIntake_autoMode_returnsFalse() {
        TenantChatLink l = mock(TenantChatLink.class);
        when(l.getIsActive()).thenReturn(true);
        when(l.getAgentMode()).thenReturn(AgentMode.AUTO);
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.of(l));
        assertThat(service.tryIntake(TENANT, ConversationChannel.EMAIL, "a@b.com", null, "Hi")).isFalse();
        verifyNoInteractions(agentService);
    }

    @Test
    void tryIntake_noChatLink_returnsFalse() {
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        assertThat(service.tryIntake(TENANT, ConversationChannel.EMAIL, "a@b.com", null, "Hi")).isFalse();
    }

    @Test
    void approve_sendsEmailWithDraft() {
        String id = intakeAndGetId();
        String result = service.approve(CHAT, id);
        verify(emailChannel).sendMessage(eq("client@x.com"), anyString(), eq("Esborrany IA"), any(), any(), any());
        verify(conversationService).finalizeSentReply(eq(TENANT), eq("client@x.com"), eq(ConversationChannel.EMAIL), eq("Esborrany IA"));
        assertThat(result).contains("enviada");
    }

    @Test
    void approve_fromDifferentChat_notAccessible() {
        String id = intakeAndGetId();
        String result = service.approve(999L, id);
        assertThat(result).contains("No accessible");
        verifyNoInteractions(emailChannel);
    }

    @Test
    void manual_sendsTypedText() {
        String id = intakeAndGetId();
        service.startManual(CHAT, id);
        assertThat(service.hasAwait(CHAT)).isTrue();
        String result = service.submitAwaitText(CHAT, "La meva resposta");
        verify(emailChannel).sendMessage(eq("client@x.com"), anyString(), eq("La meva resposta"), any(), any(), any());
        verify(conversationService).finalizeSentReply(eq(TENANT), eq("client@x.com"), eq(ConversationChannel.EMAIL), eq("La meva resposta"));
        assertThat(result).contains("enviada");
    }

    @Test
    void refine_regeneratesAndResendsButtons() {
        when(aiRouter.defaultModel()).thenReturn("model");
        when(aiRouter.forModel("model")).thenReturn(aiProvider);
        when(aiProvider.chat(anyString(), anyList(), anyString())).thenReturn("Esborrany més curt");
        String id = intakeAndGetId();

        service.startRefine(CHAT, id);
        String result = service.submitAwaitText(CHAT, "més curta");

        assertThat(result).isNull(); // ja s'ha reenviat l'esborrany amb botons
        // s'ha reenviat un missatge amb botons (1 al intake + 1 al refine)
        verify(telegramBotClient, times(2)).sendMessageWithButtons(eq(CHAT), anyString(), anyList());
        verifyNoInteractions(emailChannel); // encara no s'envia; espera aprovació
        // el context s'ha actualitzat amb el nou esborrany
        String result2 = service.approve(CHAT, id);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailChannel).sendMessage(anyString(), anyString(), body.capture(), any(), any(), any());
        assertThat(body.getValue()).isEqualTo("Esborrany més curt");
    }

    @Test
    void approve_whatsappMeta_sendsViaGraphApi() {
        TenantChatLink link = mock(TenantChatLink.class);
        when(link.getIsActive()).thenReturn(true);
        when(link.getAgentMode()).thenReturn(AgentMode.HYBRID);
        when(link.getTelegramChatId()).thenReturn(CHAT);
        when(link.getWhatsappMetaPhoneNumberId()).thenReturn("PHONE_ID_123");
        when(chatLinkRepository.findByTenantId(TENANT)).thenReturn(Optional.of(link));
        when(agentService.generateDmDraft(eq(TENANT), anyString(), eq(ConversationChannel.WHATSAPP_META), anyString()))
                .thenReturn("Esborrany WA");

        boolean handled = service.tryIntake(TENANT, ConversationChannel.WHATSAPP_META, "+34600111222", null, "Hola?");
        assertThat(handled).isTrue();
        String id = store.keySet().stream().filter(k -> k.startsWith("ia:ctx:")).findFirst().orElseThrow().substring("ia:ctx:".length());

        String result = service.approve(CHAT, id);
        verify(whatsAppMetaChannel).sendMessage(eq("PHONE_ID_123"), eq("+34600111222"), eq("Esborrany WA"));
        verify(conversationService).finalizeSentReply(eq(TENANT), eq("+34600111222"), eq(ConversationChannel.WHATSAPP_META), eq("Esborrany WA"));
        assertThat(result).contains("WhatsApp");
    }

    @Test
    void submitAwaitText_noPending_returnsNull() {
        assertThat(service.submitAwaitText(CHAT, "text")).isNull();
    }
}
