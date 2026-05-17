package com.amg.digitalitzacio.agents.application.scheduler;

import com.amg.digitalitzacio.agents.application.AgentRegistry;
import com.amg.digitalitzacio.agents.domain.ScheduledAgentTaskRepository;
import com.amg.digitalitzacio.agents.domain.ScheduledTaskStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentTaskScheduler {

    private final ScheduledAgentTaskRepository taskRepository;
    private final AgentRegistry agentRegistry;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void processPendingTasks() {
        var now = Instant.now();
        var pendingTasks = taskRepository.findByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                ScheduledTaskStatus.PENDING, now);

        for (var task : pendingTasks) {
            try {
                agentRegistry.findBySlug(task.getAgentSlug()).ifPresentOrElse(
                        agent -> {
                            log.debug("Executant tasca {} per a agent {} (tenant {})",
                                    task.getId(), task.getAgentSlug(), task.getTenantId());
                            agent.executeTask(task.getTenantId(), task.getTaskType(), task.getPayload());
                            task.setStatus(ScheduledTaskStatus.EXECUTED);
                            task.setExecutedAt(Instant.now());
                        },
                        () -> {
                            log.warn("Agent {} no trobat per a la tasca {}, marcant com a FALLIDA",
                                    task.getAgentSlug(), task.getId());
                            task.setStatus(ScheduledTaskStatus.FAILED);
                            task.setErrorMessage("Agent not found: " + task.getAgentSlug());
                        }
                );
            } catch (Exception e) {
                log.error("Error executant tasca {}: {}", task.getId(), e.getMessage());
                task.setStatus(ScheduledTaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
        }

        if (!pendingTasks.isEmpty()) {
            taskRepository.saveAll(pendingTasks);
            log.info("Processades {} tasques pendents", pendingTasks.size());
        }
    }
}
