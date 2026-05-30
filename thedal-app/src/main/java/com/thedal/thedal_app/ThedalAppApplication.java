package com.thedal.thedal_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;
import java.util.TimeZone;
import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
@Slf4j
@EnableScheduling 
public class ThedalAppApplication {

	public static void main(String[] args) {
		System.out.println("Started Thedal Now");
		
		// FORCE timezone to UTC before anything else happens
		System.setProperty("user.timezone", "UTC");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
		System.out.println("Timezone set to: " + TimeZone.getDefault().getID());
		
		// Also try to override any PostgreSQL timezone issues
		System.setProperty("spring.jpa.properties.hibernate.jdbc.time_zone", "UTC");
		
		System.setProperty("java.net.preferIPv4Stack", "true");
		SpringApplication.run(ThedalAppApplication.class, args);
		//log.info("******** Backend Service has started *********");
	}
	
	private static void logMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / 1024 / 1024; 
        long totalMemory = runtime.totalMemory() / 1024 / 1024;
        long freeMemory = runtime.freeMemory() / 1024 / 1024;
        long usedMemory = totalMemory - freeMemory;

        log.info("Memory Stats - Max: {} MB, Total: {} MB, Used: {} MB, Free: {} MB", 
                 maxMemory, totalMemory, usedMemory, freeMemory);
    }

//	@Bean
//	public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
//	    return new MongoTransactionManager(dbFactory);
//	}
	
	/**
	 * Thread pool executor for async aggregation jobs.
	 * Optimized for parallel processing of election parts.
	 */
	@Bean(name = "aggregationExecutor")
	public Executor aggregationExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);  // 10 parallel threads
		executor.setMaxPoolSize(15);   // Max 15 threads under load
		executor.setQueueCapacity(100); // Queue up to 100 tasks
		executor.setThreadNamePrefix("aggregation-");
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60);
		executor.initialize();
		log.info("Initialized aggregationExecutor with corePoolSize=10, maxPoolSize=15");
		return executor;
	}
	
}