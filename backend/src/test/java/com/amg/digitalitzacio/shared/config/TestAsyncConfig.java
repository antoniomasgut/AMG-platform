package com.amg.digitalitzacio.shared.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Substitueix l'executor asíncron per un de síncron durant els tests.
 * Evita race conditions en tests que usen @Async i @Transactional alhora.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean
    @Primary
    public TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }
}
