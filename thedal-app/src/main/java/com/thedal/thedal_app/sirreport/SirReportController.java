package com.thedal.thedal_app.sirreport;

import com.thedal.thedal_app.sirreport.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/voter/sir-report")
@RequiredArgsConstructor
@Slf4j
public class SirReportController {
    
    private final SirReportService sirReportService;
    private final SirReportJobRepository jobRepository;
    private final com.thedal.thedal_app.general.RequestDetailsService requestDetailsService;
    
    /**
     * Upload and compare two voter Excel files
     * POST /api/voter/sir-report/compare
     */
    @PostMapping("/compare")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<SirReportUploadResponse> compareVoterFiles(
            @RequestParam("baseFile") MultipartFile baseFile,
            @RequestParam("newFile") MultipartFile newFile,
            @RequestParam(value = "electionId", required = false) Long electionId,
            HttpServletRequest request) {
        
        try {
            // Validate files
            if (baseFile.isEmpty() || newFile.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(SirReportUploadResponse.builder()
                                .message("Both base file and new file are required")
                                .build());
            }
            
            // Validate file types
            String baseFileName = baseFile.getOriginalFilename();
            String newFileName = newFile.getOriginalFilename();
            
            if (baseFileName == null || newFileName == null ||
                (!baseFileName.endsWith(".xlsx") && !baseFileName.endsWith(".xls")) ||
                (!newFileName.endsWith(".xlsx") && !newFileName.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                        .body(SirReportUploadResponse.builder()
                                .message("Only Excel files (.xlsx, .xls) are supported")
                                .build());
            }
            
            // Get account ID from request (assuming it's set by security filter)
            Long accountId = (Long) request.getAttribute("accountId");
            if (accountId == null) {
                accountId = 1L; // Default for testing
            }
            
            log.info("Starting SIR report comparison. Base: {}, New: {}, Account: {}, Election: {}", 
                    baseFileName, newFileName, accountId, electionId);
            
            SirReportUploadResponse response = sirReportService.startComparison(
                    baseFile, newFile, accountId, electionId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error starting comparison", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SirReportUploadResponse.builder()
                            .message("Error starting comparison: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Get status of a comparison job
     * GET /api/voter/sir-report/{jobId}/status
     */
    @GetMapping("/{jobId}/status")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<SirReportStatusResponse> getStatus(@PathVariable UUID jobId) {
        try {
            SirReportStatusResponse response = sirReportService.getStatus(jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting status for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SirReportStatusResponse.builder()
                            .jobId(jobId)
                            .message("Job not found or error: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Get summary of a comparison
     * GET /api/voter/sir-report/{jobId}/summary
     */
    @GetMapping("/{jobId}/summary")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<SirReportSummaryResponse> getSummary(@PathVariable UUID jobId) {
        try {
            SirReportSummaryResponse response = sirReportService.getSummary(jobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting summary for job: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SirReportSummaryResponse.builder()
                            .jobId(jobId)
                            .errorMessage("Job not found or error: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Get detailed records by type (ADDITIONS, DELETIONS, SHIFTS)
     * GET /api/voter/sir-report/{jobId}/details?type=ADDITIONS&page=0&size=50
     */
    @GetMapping("/{jobId}/details")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<?> getDetails(
            @PathVariable UUID jobId,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<?> data = sirReportService.getDetails(jobId, type, pageable);
            
            return ResponseEntity.ok(data);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid type. Use: ADDITIONS, DELETIONS, or SHIFTS");
        } catch (Exception e) {
            log.error("Error getting details for job: {}, type: {}", jobId, type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error getting details: " + e.getMessage());
        }
    }
    
    /**
     * List all comparisons
     * GET /api/voter/sir-report/list?page=0&size=20&electionId=123
     */
    @GetMapping("/list")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<Page<SirReportListItem>> listComparisons(
            @RequestParam(value = "electionId", required = false) Long electionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        
        try {
            // Get account ID from request
            Long accountId = (Long) request.getAttribute("accountId");
            if (accountId == null) {
                accountId = 1L; // Default for testing
            }
            
            Pageable pageable = PageRequest.of(page, size);
            Page<SirReportListItem> comparisons = sirReportService.listComparisons(accountId, electionId, pageable);
            
            return ResponseEntity.ok(comparisons);
            
        } catch (Exception e) {
            log.error("Error listing comparisons", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Delete a comparison
     * DELETE /api/voter/sir-report/{jobId}
     */
    @DeleteMapping("/{jobId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN')")
    public ResponseEntity<String> deleteComparison(@PathVariable UUID jobId) {
        try {
            sirReportService.deleteComparison(jobId);
            return ResponseEntity.ok("Comparison deleted successfully");
        } catch (Exception e) {
            log.error("Error deleting comparison: {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting comparison: " + e.getMessage());
        }
    }
    
    /**
     * Initiate export for a specific type
     * POST /api/voter/sir-report/{jobId}/export/initiate?type=ADDITIONS&format=EXCEL
     */
    @PostMapping("/{jobId}/export/initiate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<SirReportExportInitiateResponse> initiateExport(
            @PathVariable UUID jobId,
            @RequestParam String type,
            @RequestParam String format,
            HttpServletRequest request) {
        try {
            Long accountId = requestDetailsService.getCurrentAccountId();
            
            // Get election ID from the SIR job
            SirReportJobEntity sirJob = jobRepository.findByJobId(jobId)
                    .orElseThrow(() -> new RuntimeException("SIR job not found"));
            
            SirReportExportInitiateResponse response = sirReportService.initiateExport(
                    jobId, type, format, accountId, sirJob.getElectionId());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating export for job: {}, type: {}, format: {}", jobId, type, format, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SirReportExportInitiateResponse.builder()
                            .message("Failed to initiate export: " + e.getMessage())
                            .build());
        }
    }
    
    /**
     * Get export job status
     * GET /api/voter/sir-report/export/{exportJobId}/status
     */
    @GetMapping("/export/{exportJobId}/status")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'ADMIN', 'VOLUNTEER')")
    public ResponseEntity<SirReportExportStatusResponse> getExportStatus(@PathVariable Long exportJobId) {
        try {
            SirReportExportStatusResponse response = sirReportService.getExportStatus(exportJobId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting export status for job: {}", exportJobId, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(SirReportExportStatusResponse.builder()
                            .exportJobId(exportJobId)
                            .message("Export job not found or error: " + e.getMessage())
                            .build());
        }
    }
}
