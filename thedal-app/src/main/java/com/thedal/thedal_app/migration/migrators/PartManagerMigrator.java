package com.thedal.thedal_app.migration.migrators;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrator for PartManager entities from PostgreSQL to MongoDB
 */
@Component
@Slf4j
public class PartManagerMigrator {

    @Autowired
    private PartManagerRepository partManagerRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Migrate all PartManager entities for a specific account
     * @param accountId Account ID to migrate
     * @param batchSize Number of records to process in each batch
     * @param parallelism Number of parallel threads (not used in this implementation)
     * @return Number of migrated records
     */
    @Transactional(readOnly = true)
    public long migratePartManagers(Long accountId, int batchSize, int parallelism) {
        log.info("Starting PartManager migration for accountId: {}", accountId);
        
        AtomicLong totalMigrated = new AtomicLong(0);
        int pageNumber = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<PartManager> partManagerPage = partManagerRepository.findByAccountIdOptimized(accountId, pageable);
                
                if (partManagerPage.hasContent()) {
                    List<PartManager> partManagers = partManagerPage.getContent();
                    
                    // Bulk insert to MongoDB
                    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PartManager.class);
                    bulkOps.insert(partManagers);
                    bulkOps.execute();
                    
                    totalMigrated.addAndGet(partManagers.size());
                    log.debug("Migrated batch of {} PartManager records for accountId: {}", 
                             partManagers.size(), accountId);
                }
                
                hasMore = partManagerPage.hasNext();
                pageNumber++;
                
            } catch (Exception e) {
                log.error("Error migrating PartManager batch for accountId: {}, page: {}", 
                         accountId, pageNumber, e);
                throw new RuntimeException("Failed to migrate PartManager data", e);
            }
        }
        
        log.info("Completed PartManager migration for accountId: {}. Total migrated: {}", 
                accountId, totalMigrated.get());
        return totalMigrated.get();
    }

    /**
     * Migrate all PartManager entities across all accounts
     * @param batchSize Number of records to process in each batch
     * @param parallelism Number of parallel threads (not used in this implementation)
     * @return Number of migrated records
     */
    @Transactional(readOnly = true)
    public long migrateAllPartManagers(int batchSize, int parallelism) {
        log.info("Starting global PartManager migration");
        
        AtomicLong totalMigrated = new AtomicLong(0);
        int pageNumber = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<PartManager> partManagerPage = partManagerRepository.findAll(pageable);
                
                if (partManagerPage.hasContent()) {
                    List<PartManager> partManagers = partManagerPage.getContent();
                    
                    // Bulk insert to MongoDB
                    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, PartManager.class);
                    bulkOps.insert(partManagers);
                    bulkOps.execute();
                    
                    totalMigrated.addAndGet(partManagers.size());
                    log.debug("Migrated batch of {} PartManager records", partManagers.size());
                }
                
                hasMore = partManagerPage.hasNext();
                pageNumber++;
                
            } catch (Exception e) {
                log.error("Error migrating PartManager batch, page: {}", pageNumber, e);
                throw new RuntimeException("Failed to migrate PartManager data", e);
            }
        }
        
        log.info("Completed global PartManager migration. Total migrated: {}", totalMigrated.get());
        return totalMigrated.get();
    }

    /**
     * Clear all PartManager data from MongoDB
     */
    public void clearPartManagerData() {
        log.info("Clearing all PartManager data from MongoDB");
        mongoTemplate.remove(new Query(), PartManager.class);
        log.info("Cleared all PartManager data from MongoDB");
    }

    /**
     * Clear PartManager data for a specific account from MongoDB
     * @param accountId Account ID to clear
     */
    public void clearPartManagerDataByAccount(Long accountId) {
        log.info("Clearing PartManager data for accountId: {} from MongoDB", accountId);
        Query query = new Query(Criteria.where("accountId").is(accountId));
        mongoTemplate.remove(query, PartManager.class);
        log.info("Cleared PartManager data for accountId: {} from MongoDB", accountId);
    }

    /**
     * Get count of PartManager records for an account
     * @param accountId Account ID
     * @return Count of records
     */
    public long getPartManagerCount(Long accountId) {
        return partManagerRepository.countByAccountId(accountId);
    }

    /**
     * Get total count of PartManager records
     * @return Total count of records
     */
    public long getTotalPartManagerCount() {
        return partManagerRepository.count();
    }
}
