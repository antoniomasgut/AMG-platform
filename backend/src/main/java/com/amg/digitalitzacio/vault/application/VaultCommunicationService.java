package com.amg.digitalitzacio.vault.application;

import com.amg.digitalitzacio.vault.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VaultCommunicationService {

    private final CommunicationRequestRepository requestRepository;
    private final List<CommunicationSender> senders;

    private Map<CommunicationChannel, CommunicationSender> senderMap;

    @PostConstruct
    void init() {
        senderMap = senders.stream()
            .collect(Collectors.toMap(CommunicationSender::supportedChannel, s -> s));
    }

    @Transactional
    public CommunicationRequest send(UUID tenantId, UUID tenantServiceId, CommunicationChannel channel,
                                     String recipient, String subject, String body,
                                     RequestType requestType, UUID fieldId, Instant expiresAt) {
        var sender = senderMap.get(channel);
        if (sender == null) {
            log.warn("No sender for channel {}, falling back to EMAIL", channel);
            sender = senderMap.get(CommunicationChannel.EMAIL);
        }

        var request = CommunicationRequest.builder()
            .tenantId(tenantId)
            .tenantServiceId(tenantServiceId)
            .channel(sender != null ? sender.supportedChannel() : CommunicationChannel.EMAIL)
            .recipient(recipient)
            .subject(subject)
            .body(body)
            .status(CommunicationStatus.SENT)
            .requestType(requestType)
            .fieldId(fieldId)
            .expiresAt(expiresAt)
            .sentAt(Instant.now())
            .retryCount(0)
            .maxRetries(3)
            .build();

        boolean delivered = false;
        if (sender != null) {
            delivered = sender.send(recipient, subject, body);
        }

        if (delivered) {
            request.setStatus(CommunicationStatus.DELIVERED);
        }
        return requestRepository.save(request);
    }

    @Transactional
    public void processResponse(UUID requestId, String responseData) {
        var request = requestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Communication request not found: " + requestId));

        request.setResponseData(responseData);
        request.setStatus(CommunicationStatus.RESPONDED);
        request.setRespondedAt(Instant.now());
        requestRepository.save(request);

        log.info("Communication request {} responded: {}", requestId, responseData);
    }

    @Transactional
    public List<CommunicationRequest> retryExpired() {
        var expired = requestRepository.findExpiredForRetry(Instant.now(), PageRequest.of(0, 100));
        for (var req : expired) {
            req.setRetryCount(req.getRetryCount() + 1);
            req.setExpiresAt(Instant.now().plus(java.time.Duration.ofDays(1)));

            var sender = senderMap.get(req.getChannel());
            if (sender != null) {
                try {
                    boolean delivered = sender.send(req.getRecipient(), req.getSubject(), req.getBody());
                    if (delivered) {
                        req.setStatus(CommunicationStatus.DELIVERED);
                        req.setSentAt(Instant.now());
                        log.info("Retry {} for request {} delivered", req.getRetryCount(), req.getId());
                    } else {
                        req.setStatus(CommunicationStatus.SENT);
                        log.warn("Retry {} for request {} failed", req.getRetryCount(), req.getId());
                    }
                } catch (Exception e) {
                    req.setStatus(CommunicationStatus.FAILED);
                    log.error("Retry {} for request {} error: {}", req.getRetryCount(), req.getId(), e.getMessage());
                }
            } else {
                req.setStatus(CommunicationStatus.FAILED);
            }

            if (req.getRetryCount() >= req.getMaxRetries()) {
                req.setStatus(CommunicationStatus.FAILED);
                log.warn("Request {} exhausted retries, marking FAILED", req.getId());
            }

            requestRepository.save(req);
        }
        return expired;
    }
}
