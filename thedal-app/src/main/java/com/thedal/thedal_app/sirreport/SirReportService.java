package com.thedal.thedal_app.sirreport;

import com.thedal.thedal_app.sirreport.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SirReportService {
    
    private final SirReportJobRepository jobRepository;
    private final SirReportAdditionRepository additionRepository;
    private final SirReportDeletionRepository deletionRepository;
    private final SirReportShiftRepository shiftRepository;
    private final ExcelReaderService excelReaderService;
    private final SirReportAsyncProcessor asyncProcessor;
    private final SirReportExportJobRepository exportJobRepository;
    private final SirReportExportAsyncProcessor exportAsyncProcessor;
    
    /**
     * Start async comparison process
     */
    public SirReportUploadResponse startComparison(
            MultipartFile baseFile, 
            MultipartFile newFile, 
            Long accountId, 
            Long electionId) {
        
        try {
            // Create job entry
            SirReportJobEntity job = new SirReportJobEntity();
            job.setAccountId(accountId);
            job.setElectionId(electionId);
            job.setBaseFileName(baseFile.getOriginalFilename());
            job.setNewFileName(newFile.getOriginalFilename());
            job.setStatus(SirReportStatus.PROCESSING);
            job.setMessage("Comparison started");
            job.setProgress(0);
            
            job = jobRepository.saveAndFlush(job); // Flush to ensure immediate visibility
            
            // Save files to temporary storage (avoid loading entire files into memory)
            // Files will be cleaned up by async processor after processing
            Path tempDir = Files.createTempDirectory("sir-comparison-");
            Path baseFilePath = tempDir.resolve("base-" + baseFile.getOriginalFilename());
            Path newFilePath = tempDir.resolve("new-" + newFile.getOriginalFilename());
            
            try {
                Files.copy(baseFile.getInputStream(), baseFilePath, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(newFile.getInputStream(), newFilePath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                // Clean up on error
                try {
                    Files.deleteIfExists(baseFilePath);
                    Files.deleteIfExists(newFilePath);
                    Files.deleteIfExists(tempDir);
                } catch (IOException cleanupEx) {
                    log.warn("Failed to cleanup temp files", cleanupEx);
                }
                throw new RuntimeException("Failed to save uploaded files", e);
            }
            
            // Start async processing using separate component
            // This ensures @Async works properly (can't call @Async method from same class)
            asyncProcessor.processComparisonAsync(
                job.getJobId(), 
                baseFilePath.toFile(), 
                baseFile.getOriginalFilename(),
                newFilePath.toFile(), 
                newFile.getOriginalFilename(),
                tempDir.toFile()
            );
            
            log.info("Started async comparison job: {}", job.getJobId());
            
            return SirReportUploadResponse.builder()
                    .jobId(job.getJobId())
                    .status(SirReportStatus.PROCESSING)
                    .message("Comparison started successfully")
                    .build();
        } catch (Exception e) {
            log.error("Error starting comparison", e);
            throw new RuntimeException("Failed to start comparison: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get job status
     */
    public SirReportStatusResponse getStatus(UUID jobId) {
        SirReportJobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        return SirReportStatusResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .progress(job.getProgress())
                .message(job.getMessage())
                .build();
    }
    
    /**
     * Get summary
     */
    public SirReportSummaryResponse getSummary(UUID jobId) {
        SirReportJobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        SirReportSummaryResponse.SummaryData summary = null;
        if (job.getStatus() == SirReportStatus.COMPLETED) {
            summary = SirReportSummaryResponse.SummaryData.builder()
                    .totalBaseRecords(job.getTotalBaseRecords())
                    .totalNewRecords(job.getTotalNewRecords())
                    .additions(job.getAdditionsCount())
                    .deletions(job.getDeletionsCount())
                    .shifts(job.getShiftsCount())
                    .build();
        }
        
        return SirReportSummaryResponse.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .summary(summary)
                .processedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .build();
    }
    
    /**
     * Get details by type
     */
    public Page<?> getDetails(UUID jobId, String type, Pageable pageable) {
        // Verify job exists
        jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        switch (type.toUpperCase()) {
            case "ADDITIONS":
                Page<SirReportAdditionEntity> additions = additionRepository.findByJobId(jobId, pageable);
                return additions.map(this::toVoterRecordDto);
                
            case "DELETIONS":
                Page<SirReportDeletionEntity> deletions = deletionRepository.findByJobId(jobId, pageable);
                return deletions.map(this::toVoterRecordDto);
                
            case "SHIFTS":
                Page<SirReportShiftEntity> shifts = shiftRepository.findByJobId(jobId, pageable);
                return shifts.map(this::toShiftRecordDto);
                
            default:
                throw new IllegalArgumentException("Invalid type: " + type);
        }
    }
    
    /**
     * List all comparisons
     */
    public Page<SirReportListItem> listComparisons(Long accountId, Long electionId, Pageable pageable) {
        Page<SirReportJobEntity> jobs;
        
        if (electionId != null) {
            jobs = jobRepository.findByAccountIdAndElectionIdOrderByCreatedAtDesc(accountId, electionId, pageable);
        } else {
            jobs = jobRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable);
        }
        
        return jobs.map(job -> SirReportListItem.builder()
                .jobId(job.getJobId())
                .baseFileName(job.getBaseFileName())
                .newFileName(job.getNewFileName())
                .status(job.getStatus())
                .additions(job.getAdditionsCount())
                .deletions(job.getDeletionsCount())
                .shifts(job.getShiftsCount())
                .createdAt(job.getCreatedAt())
                .completedAt(job.getCompletedAt())
                .build());
    }
    
    /**
     * Delete comparison
     */
    @Transactional
    public void deleteComparison(UUID jobId) {
        SirReportJobEntity job = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));
        
        additionRepository.deleteByJobId(jobId);
        deletionRepository.deleteByJobId(jobId);
        shiftRepository.deleteByJobId(jobId);
        jobRepository.delete(job);
    }
    
    // Helper methods
    private VoterRecordDto toVoterRecordDto(SirReportAdditionEntity entity) {
        return VoterRecordDto.builder()
                .epicNumber(entity.getEpicNumber())
                .partNo(entity.getPartNo())
                .voterNameEn(entity.getVoterNameEn())
                .serialNo(entity.getSerialNo())
                .sectionNo(entity.getSectionNo())
                .houseNoEn(entity.getHouseNoEn())
                .age(entity.getAge())
                .gender(entity.getGender())
                .build();
    }
    
    private VoterRecordDto toVoterRecordDto(SirReportDeletionEntity entity) {
        return VoterRecordDto.builder()
                .epicNumber(entity.getEpicNumber())
                .partNo(entity.getPartNo())
                .voterNameEn(entity.getVoterNameEn())
                .serialNo(entity.getSerialNo())
                .sectionNo(entity.getSectionNo())
                .houseNoEn(entity.getHouseNoEn())
                .age(entity.getAge())
                .gender(entity.getGender())
                .build();
    }
    
    private ShiftRecordDto toShiftRecordDto(SirReportShiftEntity entity) {
        return ShiftRecordDto.builder()
                .epicNumber(entity.getEpicNumber())
                .oldPartNo(entity.getOldPartNo())
                .newPartNo(entity.getNewPartNo())
                .voterNameEn(entity.getVoterNameEn())
                .serialNo(entity.getSerialNo())
                .sectionNo(entity.getSectionNo())
                .houseNoEn(entity.getHouseNoEn())
                .age(entity.getAge())
                .gender(entity.getGender())
                .build();
    }
    
    /**
     * Initiate export for a specific type (ADDITIONS, DELETIONS, SHIFTS)
     */
    public SirReportExportInitiateResponse initiateExport(
            UUID jobId, 
            String type, 
            String format,
            Long accountId,
            Long electionId) {
        
        // Verify SIR job exists
        SirReportJobEntity sirJob = jobRepository.findByJobId(jobId)
                .orElseThrow(() -> new RuntimeException("SIR job not found: " + jobId));
        
        if (sirJob.getStatus() != SirReportStatus.COMPLETED) {
            throw new RuntimeException("SIR comparison not completed yet");
        }
        
        // Parse export type and format
        SirReportExportType exportType;
        SirReportExportFormat exportFormat;
        
        try {
            exportType = SirReportExportType.valueOf(type.toUpperCase());
            exportFormat = SirReportExportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid type or format");
        }
        
        // Create export job
        SirReportExportJob exportJob = SirReportExportJob.builder()
                .jobId(jobId)
                .accountId(accountId)
                .electionId(electionId)
                .exportType(exportType)
                .format(exportFormat)
                .status("PROCESSING")
                .message("Export initiated")
                .timeStarted(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24)) // 24 hour expiration
                .build();
        
        exportJob = exportJobRepository.save(exportJob);
        
        // Start async processing
        exportAsyncProcessor.processExportAsync(exportJob.getId());
        
        log.info("Started export job {} for SIR job {}, type: {}, format: {}", 
                exportJob.getId(), jobId, type, format);
        
        return SirReportExportInitiateResponse.builder()
                .exportJobId(exportJob.getId())
                .status("PROCESSING")
                .message("Export started successfully")
                .build();
    }
    
    /**
     * Get export job status
     */
    public SirReportExportStatusResponse getExportStatus(Long exportJobId) {
        SirReportExportJob exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new RuntimeException("Export job not found: " + exportJobId));
        
        return SirReportExportStatusResponse.builder()
                .exportJobId(exportJob.getId())
                .status(exportJob.getStatus())
                .message(exportJob.getMessage())
                .downloadUrl(exportJob.getAwsS3DownloadUrl())
                .recordCount(exportJob.getRecordCount())
                .timeStarted(exportJob.getTimeStarted())
                .timeCompleted(exportJob.getTimeCompleted())
                .expiresAt(exportJob.getExpiresAt())
                .build();
    }
}

