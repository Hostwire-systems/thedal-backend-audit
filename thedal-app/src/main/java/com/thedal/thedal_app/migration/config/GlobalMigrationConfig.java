package com.thedal.thedal_app.migration.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * Configuration for global migration execution
 */
@Configuration
@EnableAsync
@Slf4j
public class GlobalMigrationConfig {

    @Value("${migration.global.thread-pool.core-size:2}")
    private int corePoolSize;
    
    @Value("${migration.global.thread-pool.max-size:5}")
    private int maxPoolSize;
    
    @Value("${migration.global.thread-pool.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${migration.global.thread-pool.keep-alive:60}")
    private int keepAliveSeconds;
    
    @Bean(name = "globalMigrationExecutor")
    public Executor globalMigrationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("GlobalMigration-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        
        log.info("Global Migration Thread Pool configured: core={}, max={}, queue={}", 
                corePoolSize, maxPoolSize, queueCapacity);
        
        return executor;
    }
}
