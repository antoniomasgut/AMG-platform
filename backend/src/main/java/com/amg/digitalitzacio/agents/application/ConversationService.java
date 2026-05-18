package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.Conversation;
import com.amg.digitalitzacio.agents.domain.ConversationChannel;
import com.amg.digitalitzacio.agents.domain.ConversationRepository;
import com.amg.digitalitzacio.agents.domain.ConversationRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PATTERN = "conv:%s:%s:%s";
    private static final long REDIS_TTL_HOURS = 48;
    private static final int MAX_HISTORY_SIZE = 20;

    public List<Conversation> loadHistory(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        String redisKey = String.format(REDIS_KEY_PATTERN, tenantId, channel.name(), customerIdentifier);

        try {
            String cachedData = stringRedisTemplate.opsForValue().get(redisKey);
            if (cachedData != null) {
                List<Conversation> cached = objectMapper.readValue(cachedData,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Conversation.class));
                log.debug("Loaded conversation history from Redis for tenant={}, customer={}, channel={}",
                    tenantId, customerIdentifier, channel);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Redis cache read failed for key: {}", redisKey, e);
        }

        List<Conversation> conversations = loadFromDatabase(tenantId, customerIdentifier, channel);

        try {
            String serializedData = objectMapper.writeValueAsString(conversations);
            stringRedisTemplate.opsForValue().set(redisKey, serializedData, REDIS_TTL_HOURS, TimeUnit.HOURS);
            log.debug("Cached conversation history to Redis for tenant={}, customer={}, channel={}",
                tenantId, customerIdentifier, channel);
        } catch (Exception e) {
            log.warn("Redis cache write failed for key: {}", redisKey, e);
        }

        return conversations;
    }

    @Transactional
    public void save(UUID tenantId, String customerIdentifier, ConversationChannel channel,
                     ConversationRole role, String content, boolean pendingApproval) {
        Conversation conversation = Conversation.builder()
                .tenantId(tenantId)
                .customerIdentifier(customerIdentifier)
                .channel(channel)
                .role(role)
                .content(content)
                .pendingApproval(pendingApproval)
                .build();

        conversationRepository.save(conversation);
        invalidateCache(tenantId, customerIdentifier, channel);

        log.debug("Saved conversation: tenant={}, customer={}, channel={}, role={}, pending={}",
            tenantId, customerIdentifier, channel, role, pendingApproval);
    }

    private List<Conversation> loadFromDatabase(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        List<Conversation> conversations = conversationRepository
                .findTop20ByTenantIdAndCustomerIdentifierAndChannelOrderByCreatedAtDesc(
                        tenantId, customerIdentifier, channel);

        // Reverse to get chronological order (oldest first)
        Collections.reverse(conversations);
        return conversations;
    }

    private void invalidateCache(UUID tenantId, String customerIdentifier, ConversationChannel channel) {
        String redisKey = String.format(REDIS_KEY_PATTERN, tenantId, channel.name(), customerIdentifier);
        try {
            stringRedisTemplate.delete(redisKey);
            log.debug("Invalidated cache for key: {}", redisKey);
        } catch (Exception e) {
            log.warn("Redis cache invalidation failed for key: {}", redisKey, e);
        }
    }
}
