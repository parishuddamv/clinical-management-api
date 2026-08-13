package com.clinicos.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for non-blocking operations.
 * Enables asynchronous execution of long-running tasks.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Configure thread pool for async operations
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Core threads that are always kept alive
        executor.setCorePoolSize(10);
        // Maximum threads in the pool
        executor.setMaxPoolSize(20);
        // Queue capacity for pending tasks
        executor.setQueueCapacity(500);
        // Thread name prefix
        executor.setThreadNamePrefix("async-task-");
        // Wait for tasks to complete before shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // Maximum wait time for task completion
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Configure thread pool for notification operations
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

