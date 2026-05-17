package com.amg.digitalitzacio.agents.application;

import com.amg.digitalitzacio.agents.domain.Agent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentRegistry {

    private final List<Agent> agents;
    private final Map<String, Agent> slugIndex = new HashMap<>();

    @PostConstruct
    void init() {
        for (var agent : agents) {
            var slug = agent.getServiceSlug();
            slugIndex.put(slug, agent);
            log.info("Agent registrat: {} (slug={})", agent.getDisplayName(), slug);
        }
    }

    public Optional<Agent> findBySlug(String slug) {
        return Optional.ofNullable(slugIndex.get(slug));
    }

    public boolean hasAgent(String slug) {
        return slugIndex.containsKey(slug);
    }

    public List<Agent> getAllAgents() {
        return List.copyOf(agents);
    }
}
