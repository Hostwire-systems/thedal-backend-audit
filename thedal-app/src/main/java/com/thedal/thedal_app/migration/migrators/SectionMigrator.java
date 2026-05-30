package com.thedal.thedal_app.migration.migrators;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

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

import com.thedal.thedal_app.settings.electionsettings.SectionEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionMongo;
import com.thedal.thedal_app.settings.electionsettings.SectionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Migrator for Section entities from PostgreSQL to MongoDB
 */
@Component
@Slf4j
public class SectionMigrator {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Migrate all Section entities for a specific account
     * @param accountId Account ID to migrate
     * @param batchSize Number of records to process in each batch
     * @param parallelism Number of parallel threads (not used in this implementation)
     * @return Number of migrated records
     */
    @Transactional(readOnly = true)
    public long migrateSections(Long accountId, int batchSize, int parallelism) {
        log.info("Starting Section migration for accountId: {}", accountId);
        
        AtomicLong totalMigrated = new AtomicLong(0);
        int pageNumber = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                
                // Get sections for this account
                Page<SectionEntity> sectionPage = sectionRepository.findAll(pageable);
                
                if (sectionPage.hasContent()) {
                    // Filter by accountId and convert to MongoDB entities
                    List<SectionMongo> sectionMongos = sectionPage.getContent().stream()
                            .filter(section -> section.getAccountId().equals(accountId))
                            .map(SectionMongo::new)
                            .collect(Collectors.toList());
                    
                    if (!sectionMongos.isEmpty()) {
                        // Bulk insert to MongoDB
                        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SectionMongo.class);
                        bulkOps.insert(sectionMongos);
                        bulkOps.execute();
                        
                        totalMigrated.addAndGet(sectionMongos.size());
                        log.debug("Migrated batch of {} Section records for accountId: {}", 
                                 sectionMongos.size(), accountId);
                    }
                }
                
                hasMore = sectionPage.hasNext();
                pageNumber++;
                
            } catch (Exception e) {
                log.error("Error migrating Section batch for accountId: {}, page: {}", 
                         accountId, pageNumber, e);
                throw new RuntimeException("Failed to migrate Section data", e);
            }
        }
        
        log.info("Completed Section migration for accountId: {}. Total migrated: {}", 
                accountId, totalMigrated.get());
        return totalMigrated.get();
    }

    /**
     * Migrate all Section entities across all accounts
     * @param batchSize Number of records to process in each batch
     * @param parallelism Number of parallel threads (not used in this implementation)
     * @return Number of migrated records
     */
    @Transactional(readOnly = true)
    public long migrateAllSections(int batchSize, int parallelism) {
        log.info("Starting global Section migration");
        
        AtomicLong totalMigrated = new AtomicLong(0);
        int pageNumber = 0;
        boolean hasMore = true;
        
        while (hasMore) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<SectionEntity> sectionPage = sectionRepository.findAll(pageable);
                
                if (sectionPage.hasContent()) {
                    List<SectionMongo> sectionMongos = sectionPage.getContent().stream()
                            .map(SectionMongo::new)
                            .collect(Collectors.toList());
                    
                    // Bulk insert to MongoDB
                    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, SectionMongo.class);
                    bulkOps.insert(sectionMongos);
                    bulkOps.execute();
                    
                    totalMigrated.addAndGet(sectionMongos.size());
                    log.debug("Migrated batch of {} Section records", sectionMongos.size());
                }
                
                hasMore = sectionPage.hasNext();
                pageNumber++;
                
            } catch (Exception e) {
                log.error("Error migrating Section batch, page: {}", pageNumber, e);
                throw new RuntimeException("Failed to migrate Section data", e);
            }
        }
        
        log.info("Completed global Section migration. Total migrated: {}", totalMigrated.get());
        return totalMigrated.get();
    }

    /**
     * Clear all Section data from MongoDB
     */
    public void clearSectionData() {
        log.info("Clearing all Section data from MongoDB");
        mongoTemplate.remove(new Query(), SectionMongo.class);
        log.info("Cleared all Section data from MongoDB");
    }

    /**
     * Clear Section data for a specific account from MongoDB
     * @param accountId Account ID to clear
     */
    public void clearSectionDataByAccount(Long accountId) {
        log.info("Clearing Section data for accountId: {} from MongoDB", accountId);
        Query query = new Query(Criteria.where("accountId").is(accountId));
        mongoTemplate.remove(query, SectionMongo.class);
        log.info("Cleared Section data for accountId: {} from MongoDB", accountId);
    }

    /**
     * Get count of Section records for an account
     * @param accountId Account ID
     * @return Count of records
     */
    public long getSectionCount(Long accountId) {
        return sectionRepository.findAll().stream()
                .filter(section -> section.getAccountId().equals(accountId))
                .count();
    }

    /**
     * Get total count of Section records
     * @return Total count of records
     */
    public long getTotalSectionCount() {
        return sectionRepository.count();
    }
}
