package com.thedal.thedal_app.sirreport;

import com.thedal.thedal_app.sirreport.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import com.github.pjfanning.xlsx.StreamingReader;
import org.springframework.context.ApplicationContext;

/**
 * Separate component for async processing to ensure @Async works properly.
 * Spring @Async doesn't work when called from the same class due to proxy limitations.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SirReportAsyncProcessor {
    
    private final ApplicationContext applicationContext;
    
    // Get proxy reference to this bean for proper transaction propagation
    private SirReportAsyncProcessor self() {
        return applicationContext.getBean(SirReportAsyncProcessor.class);
    }
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private final SirReportJobRepository jobRepository;
    private final SirReportAdditionRepository additionRepository;
    private final SirReportDeletionRepository deletionRepository;
    private final SirReportShiftRepository shiftRepository;
    private final ExcelReaderService excelReaderService;
    private final SirTempBaseVoterRepository tempBaseVoterRepository;
    private final SirTempNewVoterRepository tempNewVoterRepository;
    private final PostgresCopyBulkLoader bulkLoader;
    
    private static final int BATCH_SIZE = 1000;
    
    /**
     * Async method using database streaming to avoid OOM issues.
     * Always uses database-based comparison regardless of file size.
     * No @Transactional here to allow REQUIRES_NEW methods to work properly.
     */
    @Async
    public void processComparisonAsync(UUID jobId, File baseFile, String baseFileName, 
                                      File newFile, String newFileName, File tempDir) {
        log.info("Starting comparison for job: {}", jobId);
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("Using database streaming comparison (memory-safe) for job: {}", jobId);
            processComparisonStreaming(jobId, baseFile, baseFileName, newFile, newFileName);
            
            long totalTime = System.currentTimeMillis() - startTime;
            log.info("Completed comparison for job: {} in {} ms", jobId, totalTime);
            
        } catch (Exception e) {
            log.error("Error in comparison for job: {}", jobId, e);
            self().updateJobFailed(jobId, "Comparison failed: " + e.getMessage());
        } finally {
            // Clean up temporary files
            cleanupTempFiles(baseFile, newFile, tempDir);
        }
    }
    
    /**
     * Add cleanup method for temporary files
     */
    private void cleanupTempFiles(File baseFile, File newFile, File tempDir) {
        try {
            if (baseFile != null && baseFile.exists()) {
                Files.deleteIfExists(baseFile.toPath());
                log.debug("Deleted temp base file: {}", baseFile.getAbsolutePath());
            }
            if (newFile != null && newFile.exists()) {
                Files.deleteIfExists(newFile.toPath());
                log.debug("Deleted temp new file: {}", newFile.getAbsolutePath());
            }
            if (tempDir != null && tempDir.exists()) {
                Files.deleteIfExists(tempDir.toPath());
                log.debug("Deleted temp directory: {}", tempDir.getAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup temporary files", e);
        }
    }
    
    /**
     * Ultra-optimized database comparison using PostgreSQL COPY + parallel queries
     */
    private void processComparisonStreaming(UUID jobId, File baseFile, String baseFileName,
                                           File newFile, String newFileName) throws Exception {
        long totalStart = System.currentTimeMillis();
        
        // Clean up any old temp data (needs transaction for @Modifying queries)
        self().cleanupTempTablesInitial(jobId);
        
        // PHASE 1: Ultra-fast bulk load using PostgreSQL COPY (2-3 min per file)
        self().updateJobProgress(jobId, 5, "Converting base file to CSV format...");
        long baseStart = System.currentTimeMillis();
        String baseCsv = bulkLoader.convertExcelToCsv(baseFile, jobId, excelReaderService);
        log.info("Base file CSV conversion took {} ms", System.currentTimeMillis() - baseStart);
        
        self().updateJobProgress(jobId, 15, "Bulk loading base file...");
        baseStart = System.currentTimeMillis();
        long baseCount = bulkLoader.bulkLoadFromCsv(baseCsv, "sir_temp_base_voters", 1000000);
        log.info("Base file bulk load took {} ms for {} rows", System.currentTimeMillis() - baseStart, baseCount);
        
        self().updateJobProgress(jobId, 30, "Converting new file to CSV format...");
        long newStart = System.currentTimeMillis();
        String newCsv = bulkLoader.convertExcelToCsv(newFile, jobId, excelReaderService);
        log.info("New file CSV conversion took {} ms", System.currentTimeMillis() - newStart);
        
        self().updateJobProgress(jobId, 45, "Bulk loading new file...");
        newStart = System.currentTimeMillis();
        long newCount = bulkLoader.bulkLoadFromCsv(newCsv, "sir_temp_new_voters", 1000000);
        log.info("New file bulk load took {} ms for {} rows", System.currentTimeMillis() - newStart, newCount);
        
        // Ensure join indexes exist to avoid long-running sequential scans
        self().ensureTempIndexes();
        self().analyzeTempTables();

        // PHASE 2: SQL comparisons (separate transactions per step to avoid long-running single TX)
        self().updateJobProgress(jobId, 60, String.format("Comparing %,d base vs %,d new records...", baseCount, newCount));
        long compareStart = System.currentTimeMillis();
        
        long additions = self().findAdditionsUsingSql(jobId);
        log.info("Additions found: {}", additions);
        
        long deletions = self().findDeletionsUsingSql(jobId);
        log.info("Deletions found: {}", deletions);
        
        long shifts = self().findShiftsUsingSql(jobId);
        log.info("Shifts found: {}", shifts);
        
        log.info("All comparisons completed in {} ms", System.currentTimeMillis() - compareStart);
        
        self().updateJobProgress(jobId, 95, String.format("Found: %,d additions, %,d deletions, %,d shifts", 
            additions, deletions, shifts));
        
        // PHASE 3: Cleanup temp tables
        self().cleanupTempTables(jobId);
        
        long totalTime = System.currentTimeMillis() - totalStart;
        self().updateJobCompleted(jobId, additions, deletions, shifts, baseCount, newCount,
            String.format("Completed in %d min %d sec", totalTime / 60000, (totalTime % 60000) / 1000));
    }
    
    /**
     * Initial cleanup at start of job (separate method for transaction isolation)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupTempTablesInitial(UUID jobId) {
        log.debug("Initial cleanup of temp tables for job: {}", jobId);
        tempBaseVoterRepository.deleteByJobId(jobId);
        tempNewVoterRepository.deleteByJobId(jobId);
    }

    /**
     * Ensure supporting indexes on temp tables to make joins fast per job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureTempIndexes() {
        // Indexes on job_id + epic_number greatly accelerate JOIN lookups
        entityManager.createNativeQuery("CREATE INDEX IF NOT EXISTS idx_sir_temp_base_job_epic ON sir_temp_base_voters(job_id, epic_number)").executeUpdate();
        entityManager.createNativeQuery("CREATE INDEX IF NOT EXISTS idx_sir_temp_new_job_epic ON sir_temp_new_voters(job_id, epic_number)").executeUpdate();
    }

    /**
     * Analyze temp tables so planner picks the indexes we just created
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void analyzeTempTables() {
        entityManager.createNativeQuery("ANALYZE sir_temp_base_voters").executeUpdate();
        entityManager.createNativeQuery("ANALYZE sir_temp_new_voters").executeUpdate();
    }
    
    
    /**
     * Cleanup temporary tables for this job
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void cleanupTempTables(UUID jobId) {
        log.info("Cleaning up temp tables for job: {}", jobId);
        tempBaseVoterRepository.deleteByJobId(jobId);
        tempNewVoterRepository.deleteByJobId(jobId);
        log.info("Temp tables cleaned up");
    }
    
    /**
     * In-memory comparison removed - always use database streaming to avoid OOM
     */
    /**
     * In-memory comparison removed - always use database streaming to avoid OOM
     */
    
    /**
     * Finalize job with counts
     */
    private void finalizeJob(UUID jobId, int baseCount, int newCount, int additions, int deletions, int shifts) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setTotalBaseRecords(baseCount);
            job.setTotalNewRecords(newCount);
            job.setAdditionsCount(additions);
            job.setDeletionsCount(deletions);
            job.setShiftsCount(shifts);
            job.setStatus(SirReportStatus.COMPLETED);
            job.setProgress(100);
            job.setMessage("Comparison completed successfully");
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);
        });
    }
    
    /**
     * Update job progress - uses REQUIRES_NEW to commit immediately
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobProgress(UUID jobId, int progress, String message) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setProgress(progress);
            job.setMessage(message);
            jobRepository.saveAndFlush(job); // Flush to ensure immediate commit
            log.info("Job {} progress: {}% - {}", jobId, progress, message);
        });
    }
    
    /**
     * Find additions using SQL JOIN (voters in new but not in base)
     * Stores only epic_number and part_no for lightweight comparison
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 600)
    public long findAdditionsUsingSql(UUID jobId) {
        log.info("Finding additions using SQL for job: {}", jobId);
        // Increase statement timeout for long-running insert
        entityManager.createNativeQuery("SET LOCAL statement_timeout = 600000").executeUpdate();
        
        // Insert full voter details from new file
        String sql = """
            INSERT INTO sir_report_additions (job_id, epic_number, part_no, voter_name_en, serial_no, section_no, house_no_en, age, gender)
            SELECT 
                n.job_id, n.epic_number, n.part_no, n.voter_name_en, n.serial_no, n.section_no, n.house_no_en, n.age, n.gender
            FROM sir_temp_new_voters n
            LEFT JOIN sir_temp_base_voters b ON n.job_id = b.job_id AND n.epic_number = b.epic_number
            WHERE n.job_id = :jobId AND b.epic_number IS NULL
            """;
        
        int count = entityManager.createNativeQuery(sql)
                .setParameter("jobId", jobId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        log.info("Inserted {} additions", count);
        return count;
    }
    
    /**
     * Find deletions using SQL JOIN (voters in base but not in new)
     * Stores only epic_number and part_no for lightweight comparison
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 600)
    public long findDeletionsUsingSql(UUID jobId) {
        log.info("Finding deletions using SQL for job: {}", jobId);
        entityManager.createNativeQuery("SET LOCAL statement_timeout = 600000").executeUpdate();
        
        // Insert full voter details from base file
        String sql = """
            INSERT INTO sir_report_deletions (job_id, epic_number, part_no, voter_name_en, serial_no, section_no, house_no_en, age, gender)
            SELECT 
                b.job_id, b.epic_number, b.part_no, b.voter_name_en, b.serial_no, b.section_no, b.house_no_en, b.age, b.gender
            FROM sir_temp_base_voters b
            LEFT JOIN sir_temp_new_voters n ON b.job_id = n.job_id AND b.epic_number = n.epic_number
            WHERE b.job_id = :jobId AND n.epic_number IS NULL
            """;
        
        int count = entityManager.createNativeQuery(sql)
                .setParameter("jobId", jobId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        log.info("Inserted {} deletions", count);
        return count;
    }
    
    /**
     * Find shifts using SQL JOIN (voters in both with different part numbers)
     * Stores only epic_number, old_part_no, and new_part_no for lightweight comparison
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 600)
    public long findShiftsUsingSql(UUID jobId) {
        log.info("Finding shifts using SQL for job: {}", jobId);
        entityManager.createNativeQuery("SET LOCAL statement_timeout = 600000").executeUpdate();
        
        // Insert full voter details with old and new part numbers
        String sql = """
            INSERT INTO sir_report_shifts (job_id, epic_number, old_part_no, new_part_no, voter_name_en, serial_no, section_no, house_no_en, age, gender)
            SELECT 
                n.job_id, n.epic_number, b.part_no, n.part_no, n.voter_name_en, n.serial_no, n.section_no, n.house_no_en, n.age, n.gender
            FROM sir_temp_new_voters n
            INNER JOIN sir_temp_base_voters b ON n.job_id = b.job_id AND n.epic_number = b.epic_number
            WHERE n.job_id = :jobId AND n.part_no != b.part_no
            """;
        
        int count = entityManager.createNativeQuery(sql)
                .setParameter("jobId", jobId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        
        log.info("Inserted {} shifts", count);
        return count;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobCompleted(UUID jobId, long additions, long deletions, long shifts, 
                                    long baseCount, long newCount, String message) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus(SirReportStatus.COMPLETED);
            job.setProgress(100);
            job.setMessage(message);
            job.setCompletedAt(LocalDateTime.now());
            
            // Set counts from parameters (counts already calculated before cleanup)
            job.setAdditionsCount(Math.toIntExact(additions));
            job.setDeletionsCount(Math.toIntExact(deletions));
            job.setShiftsCount(Math.toIntExact(shifts));
            job.setTotalBaseRecords(Math.toIntExact(baseCount));
            job.setTotalNewRecords(Math.toIntExact(newCount));
            
            jobRepository.saveAndFlush(job);
            log.info("Job {} completed: {}", jobId, message);
        });
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateJobFailed(UUID jobId, String errorMessage) {
        jobRepository.findByJobId(jobId).ifPresent(job -> {
            job.setStatus(SirReportStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setMessage("Comparison failed");
            jobRepository.saveAndFlush(job);
            log.error("Job {} failed: {}", jobId, errorMessage);
        });
        
        // Clean up temp data (nested transaction for cleanup)
        try {
            self().cleanupTempTables(jobId);
        } catch (Exception e) {
            log.error("Error cleaning up temp data for failed job {}", jobId, e);
        }
    }
}
