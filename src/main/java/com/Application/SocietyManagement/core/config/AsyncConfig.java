package com.Application.SocietyManagement.core.config;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Ensures TenantContext and SecurityContext seamlessly propagate across
 * asynchronous @Async threads and task executors, preventing context evaporation.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("society-async-");
        executor.setTaskDecorator(runnable -> {
            String tenantId = TenantContext.getSocietyId();
            SecurityContext securityContext = SecurityContextHolder.getContext();
            return () -> {
                try {
                    TenantContext.setSocietyId(tenantId);
                    SecurityContextHolder.setContext(securityContext);
                    runnable.run();
                } finally {
                    TenantContext.clear();
                    SecurityContextHolder.clearContext();
                }
            };
        });
        executor.initialize();
        return executor;
    }
}
