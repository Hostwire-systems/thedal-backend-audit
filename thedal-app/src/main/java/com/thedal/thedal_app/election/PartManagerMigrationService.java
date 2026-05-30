package com.thedal.thedal_app.election;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.migration.MigrationJob;
import com.thedal.thedal_app.migration.MigrationJobStatus;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PartManagerMigrationService {

    @Autowired
    private PartManagerRepository partManagerRepository;

    @Autowired
    private PartManagerMongoRepository partManagerMongoRepository;

    // Migration job tracking
    private final Map<String, MigrationJob> migrationJobs = new ConcurrentHashMap<>();

    /**
     * Start a new migration job
     */
    public String startMigration(Long accountId, Long electionId, int batchSize) {
        String jobId = "partmgr_" + System.currentTimeMillis() + "_" + accountId + "_" + electionId;
        
        log.info("Starting PartManager migration job: {}", jobId);
        
        // Start the async migration
        migratePartManagersAsync(accountId, electionId, jobId);
        
        return jobId;
    }

    @Async
    public void migratePartManagersAsync(Long accountId, Long electionId, String jobId) {
        MigrationJob job = new MigrationJob(jobId, accountId, electionId, "PartManager Migration");
        migrationJobs.put(jobId, job);

        try {
            log.info("Job {}: Starting PartManager migration for accountId={}, electionId={}", jobId, accountId, electionId);
            
            // Get total count
            long totalCount = partManagerRepository.countByAccountIdAndElectionId(accountId, electionId);
            job.setTotalRecords(totalCount);
            
            if (totalCount == 0) {
                log.info("Job {}: No PartManagers found for migration", jobId);
                job.setStatus(MigrationJobStatus.COMPLETED);
                job.setEndTime(System.currentTimeMillis());
                return;
            }

            log.info("Job {}: Found {} PartManagers to migrate", jobId, totalCount);
            
            // Process in batches
            int batchSize = 100;
            int page = 0;
            AtomicLong migratedCount = new AtomicLong(0);

            while (!job.isCancelled()) {
                Pageable pageable = PageRequest.of(page, batchSize);
                List<PartManager> batch = getPartManagerBatch(accountId, electionId, pageable);
                
                if (batch.isEmpty()) {
                    break; // No more data
                }

                try {
                    // Convert to MongoDB entities
                    List<PartManagerMongo> mongoEntities = batch.stream()
                            .map(PartManagerMongo::new)
                            .collect(Collectors.toList());

                    // Save to MongoDB
                    partManagerMongoRepository.saveAll(mongoEntities);
                    
                    migratedCount.addAndGet(batch.size());
                    job.setProcessedRecords(migratedCount.get());
                    
                    log.info("Job {}: Migrated batch of {} PartManagers. Total: {}/{}", 
                            jobId, batch.size(), migratedCount.get(), totalCount);
                    
                } catch (Exception e) {
                    log.error("Job {}: Error migrating PartManager batch at page {}: {}", 
                             jobId, page, e.getMessage(), e);
                }

                page++;
            }

            if (job.isCancelled()) {
                log.info("Job {}: PartManager migration cancelled", jobId);
                job.setStatus(MigrationJobStatus.CANCELLED);
            } else {
                log.info("Job {}: PartManager migration completed. Migrated {}/{} records", 
                        jobId, migratedCount.get(), totalCount);
                job.setStatus(MigrationJobStatus.COMPLETED);
            }

        } catch (Exception e) {
            log.error("Job {}: PartManager migration failed: {}", jobId, e.getMessage(), e);
            job.setStatus(MigrationJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            job.setEndTime(System.currentTimeMillis());
        }
    }

    @Transactional(readOnly = true)
    protected List<PartManager> getPartManagerBatch(Long accountId, Long electionId, Pageable pageable) {
        return partManagerRepository.findByAccountIdAndElectionIdOptimized(accountId, electionId, pageable).getContent();
    }

    public MigrationJob getMigrationJob(String jobId) {
        return migrationJobs.get(jobId);
    }

    public void cancelMigration(String jobId) {
        MigrationJob job = migrationJobs.get(jobId);
        if (job != null) {
            job.cancel();
            log.info("Job {}: PartManager migration cancellation requested", jobId);
        }
    }

    public Map<String, Object> getMigrationStats(Long accountId, Long electionId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            if (electionId != null) {
                long postgresCount = partManagerRepository.countByAccountIdAndElectionId(accountId, electionId);
                long mongoCount = partManagerMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size();
                
                stats.put("postgresCount", postgresCount);
                stats.put("mongoCount", mongoCount);
                stats.put("migrationNeeded", postgresCount > mongoCount);
                stats.put("migrationComplete", postgresCount == mongoCount && postgresCount > 0);
            } else {
                // For all elections under this account - simplified stats
                List<PartManagerMongo> allMongo = partManagerMongoRepository.findByAccountId(accountId);
                stats.put("mongoCount", allMongo.size());
                stats.put("message", "Specify electionId for detailed comparison");
            }
            
        } catch (Exception e) {
            log.error("Error getting PartManager migration stats: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
}
