package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.shared.events.ServiceVerifiedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentLifecycleManager {

    private final AgentRegistry agentRegistry;

    @TransactionalEventListener
    public void onServiceVerified(ServiceVerifiedEvent event) {
        agentRegistry.findBySlug(event.serviceSlug()).ifPresentOrElse(
                agent -> {
                    log.info("Activant agent {} per al tenant {}", agent.getDisplayName(), event.tenantId());
                    agent.onActivate(event.tenantId());
                },
                () -> log.debug("No s'ha trobat agent per al slug {} (ignorat)", event.serviceSlug())
        );
    }
}
