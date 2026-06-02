package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.ChannelUsageLog;
import com.amg.digitalitzacio.agents.domain.ChannelUsageLogRepository;
import com.amg.digitalitzacio.agents.domain.TokenUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChannelUsageService {

    public static final String WHATSAPP      = "WHATSAPP";
    public static final String WHATSAPP_META = "WHATSAPP_META";
    public static final String TELEGRAM      = "TELEGRAM";
    public static final String EMAIL         = "EMAIL";
    public static final String CHAT          = "CHAT";

    private final ChannelUsageLogRepository usageLogRepository;
    private final TokenUsageLogRepository   tokenUsageLogRepository;

    @Transactional
    public void record(UUID tenantId, String channel) {
        if (tenantId == null) return;
        usageLogRepository.save(ChannelUsageLog.of(tenantId, channel));
    }

    @Transactional(readOnly = true)
    public UsageStatsResponse getStats(UUID tenantId, Instant from, Instant to) {
        return new UsageStatsResponse(
                usageLogRepository.countByTenantAndChannel(tenantId, WHATSAPP,      from, to),
                usageLogRepository.countByTenantAndChannel(tenantId, WHATSAPP_META, from, to),
                usageLogRepository.countByTenantAndChannel(tenantId, TELEGRAM,      from, to),
                usageLogRepository.countByTenantAndChannel(tenantId, EMAIL,         from, to),
                usageLogRepository.countByTenantAndChannel(tenantId, CHAT,          from, to),
                tokenUsageLogRepository.sumTokensBetween(tenantId, from, to),
                from, to
        );
    }

    @Transactional(readOnly = true)
    public UsageStatsResponse getGlobalStats(Instant from, Instant to) {
        return new UsageStatsResponse(
                usageLogRepository.countByChannel(WHATSAPP,      from, to),
                usageLogRepository.countByChannel(WHATSAPP_META, from, to),
                usageLogRepository.countByChannel(TELEGRAM,      from, to),
                usageLogRepository.countByChannel(EMAIL,         from, to),
                usageLogRepository.countByChannel(CHAT,          from, to),
                tokenUsageLogRepository.sumAllTokensSince(from),
                from, to
        );
    }

    public record UsageStatsResponse(
            long whatsappMessages,
            long whatsappMetaMessages,
            long telegramMessages,
            long emailMessages,
            long chatMessages,
            long aiTokens,
            Instant from,
            Instant to
    ) {}
}
