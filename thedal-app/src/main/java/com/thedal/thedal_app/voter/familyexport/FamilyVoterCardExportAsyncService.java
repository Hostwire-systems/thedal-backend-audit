package com.thedal.thedal_app.voter.familyexport;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import com.thedal.thedal_app.voter.VoterServiceImpl;
import com.thedal.thedal_app.export.FamilyVoterCardHtmlPdfRenderer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyVoterCardExportAsyncService {

    private final FamilyVoterCardExportJobRepository jobRepo;
    private final VoterRepo voterRepo;
    private final VoterServiceImpl voterService;
    private final AwsFileUpload awsFileUpload;
    private final FamilyVoterCardHtmlPdfRenderer htmlPdfRenderer;
    private final FamilyExcelGenerator excelGenerator;

    private static final String BUCKET = "thedalnew"; // TODO externalize if needed
    private static final String PREFIX_PDF = "exports/family-voter-cards/";
    private static final String PREFIX_EXCEL = "exports/family-excel/";
    private static final int MAX_EXPORT_SIZE = 2000; // Maximum voters allowed in single export

    @Async
    // Removed @Transactional to prevent long-running database connections for large exports
    public void execute(Long jobId) {
        FamilyVoterCardExportJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != FamilyVoterCardExportJob.Status.PENDING) return;
        job.setStatus(FamilyVoterCardExportJob.Status.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepo.save(job);
        try {
            List<VoterEntity> voters;
            if (job.getPartNo() != null) {
                // Part-wide export with family ordering and page breaks
                log.debug("FamilyCardExport: Fetching part-wide voters with family grouping accountId={}, electionId={}, partNo={}, jobId={}",
                        job.getAccountId(), job.getElectionId(), job.getPartNo(), job.getId());
                
                // Check part size before processing to prevent memory issues
                long partSize = voterRepo.countByAccountIdAndElectionIdAndPartNo(
                        job.getAccountId(), job.getElectionId(), job.getPartNo());
                if (partSize > MAX_EXPORT_SIZE) {
                    throw new RuntimeException("Part too large for export: " + partSize + " voters. Maximum allowed: " + MAX_EXPORT_SIZE);
                }
                
                voters = voterRepo.findByAccountIdAndElectionIdAndPartNo(
                        job.getAccountId(), job.getElectionId(), job.getPartNo());
            } else {
                // Family-specific export - check family size first
                log.debug("FamilyCardExport: Checking family size for familyId={} jobId={}",
                        job.getFamilyId(), job.getId());
                        
                long familySize = voterRepo.countByFamilyIdAndElectionIdAndAccountId(
                        job.getFamilyId(), job.getElectionId(), job.getAccountId());
                if (familySize > MAX_EXPORT_SIZE) {
                    throw new RuntimeException("Family too large for export: " + familySize + " members. Maximum allowed: " + MAX_EXPORT_SIZE);
                }
                
                // Family-specific export - use exact same approach as working member API but with size validation
                log.debug("FamilyCardExport: Family size {} is within limits, fetching family members via VoterService API accountId={}, electionId={}, familyId={}, jobId={}",
                        familySize, job.getAccountId(), job.getElectionId(), job.getFamilyId(), job.getId());
                
                try {
                    // Use smaller page size to reduce memory pressure (reduced from 10000 to 2000 for performance)
                    org.springframework.data.domain.Pageable largePage = org.springframework.data.domain.PageRequest.of(0, 2000);
                    org.springframework.data.domain.Sort sort = org.springframework.data.domain.Sort.by("voterId");
                    org.springframework.data.domain.Pageable pageableWithSort = org.springframework.data.domain.PageRequest.of(0, 2000, sort);
                    
                    com.thedal.thedal_app.voter.dto.FamilyMembersResponseDTO response = voterService.getFamilyMembers(
                            job.getAccountId(), job.getElectionId(), job.getFamilyId().toString(), "voterId", "asc", pageableWithSort);
                    
                    // Create a mutable copy of the list to allow sorting
                    voters = new java.util.ArrayList<>(response.getMembers());
                    
                    if (!voters.isEmpty()) {
                        log.info("FamilyCardExport: Successfully fetched {} family members via VoterService API for familyId={} jobId={}", 
                                voters.size(), job.getFamilyId(), job.getId());
                    }
                } catch (Exception voterServiceEx) {
                    log.warn("FamilyCardExport: VoterService API failed for familyId={} jobId={} error={}, falling back to direct repository", 
                            job.getFamilyId(), job.getId(), voterServiceEx.getMessage());
                    voters = new java.util.ArrayList<>();
                }
                
                // Fallback to direct repository queries if VoterService approach fails
                if (voters.isEmpty()) {
                    log.warn("FamilyCardExport: VoterService API returned empty. Attempting repository fallbacks for familyId={} jobId={}",
                            job.getFamilyId(), job.getId());
                    
                    // Use paginated query like member API but with smaller page size (reduced from 10000 to 2000 for performance)
                    org.springframework.data.domain.Pageable largePage = org.springframework.data.domain.PageRequest.of(0, 2000);
                    org.springframework.data.domain.Page<VoterEntity> votersPage = voterRepo.findFamilyMembers(
                            job.getAccountId(), job.getElectionId(), job.getFamilyId(), largePage);
                    voters = votersPage.getContent();

                    if (voters.isEmpty()) {
                        // Fallback to non-paginated version with different ordering
                        try {
                            voters = voterRepo.findFamilyMembers(job.getAccountId(), job.getElectionId(), job.getFamilyId());
                            if (voters.isEmpty()) {
                                // Final fallback to alternative method signature
                                voters = voterRepo.findByFamilyIdAndElectionIdAndAccountId(job.getFamilyId(), job.getElectionId(), job.getAccountId());
                            }
                        } catch (Exception fallbackEx) {
                            log.error("FamilyCardExport: All fallback family member queries failed for familyId={} jobId={} error={}", job.getFamilyId(), job.getId(), fallbackEx.getMessage());
                        }
                    }
                }
            }

            // Retry mechanism for potential race conditions (e.g., family mapping just committed after job started)
            if (voters.isEmpty() && job.getPartNo() == null) {
                final int maxAttempts = 3;
                final long backoffMillis = 400L;
                for (int attempt = 1; attempt <= maxAttempts && voters.isEmpty(); attempt++) {
                    try {
                        Thread.sleep(backoffMillis);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    log.debug("FamilyCardExport: Retry {} fetching family members familyId={} jobId={}", attempt, job.getFamilyId(), job.getId());
                    try {
                        voters = voterRepo.findFamilyMembers(job.getAccountId(), job.getElectionId(), job.getFamilyId());
                        if (voters.isEmpty()) {
                            voters = voterRepo.findByFamilyIdAndElectionIdAndAccountId(job.getFamilyId(), job.getElectionId(), job.getAccountId());
                        }
                    } catch (Exception retryEx) {
                        log.warn("FamilyCardExport: Retry {} failed for familyId={} jobId={} error={}", attempt, job.getFamilyId(), job.getId(), retryEx.getMessage());
                    }
                }
            }

            // Sort based on orderBy preference
            String orderBy = job.getOrderBy() != null ? job.getOrderBy() : "family";
            if ("serial".equals(orderBy)) {
                // Sort by serial number for continuous voter list order
                log.debug("FamilyCardExport: Sorting by serial number for jobId={}", job.getId());
                voters.sort(Comparator.comparing((VoterEntity v) -> v.getSerialNo() == null ? 0L : v.getSerialNo())
                        .thenComparing((VoterEntity v) -> v.getPageNumber() == null ? 0 : v.getPageNumber())
                        .thenComparing(VoterEntity::getVoterId, Comparator.nullsLast(Comparator.naturalOrder())));
            } else {
                // Sort by family sequence number (default/existing behavior)
                log.debug("FamilyCardExport: Sorting by family sequence number for jobId={}", job.getId());
                voters.sort(Comparator.comparing((VoterEntity v) -> v.getFamilySequenceNumber() == null ? 0 : v.getFamilySequenceNumber())
                        .thenComparing(VoterEntity::getVoterId, Comparator.nullsLast(Comparator.naturalOrder())));
            }

            if (voters.isEmpty()) {
                String msg = (job.getPartNo() != null)
                        ? "No voters found for part " + job.getPartNo() + " (accountId=" + job.getAccountId() + ", electionId=" + job.getElectionId() + ")"
                        : "No voters found for family " + job.getFamilyId() + " after retries (accountId=" + job.getAccountId() + ", electionId=" + job.getElectionId() + ")";
                log.error("FamilyCardExport: {}. Marking job FAILED without throwing exception. jobId={}", msg, job.getId());
                job.setStatus(FamilyVoterCardExportJob.Status.FAILED);
                job.setErrorMessage(msg);
                jobRepo.save(job);
                return; // graceful exit
            }
            
            // Log progress for large datasets
            if (voters.size() > 500) {
                log.info("FamilyCardExport: Processing large dataset with {} voters for jobId={}", voters.size(), job.getId());
                updateJobProgress(job, "Processing " + voters.size() + " voters for " + job.getExportType() + " generation");
            }
            
            // Generate based on export type
            if (job.getExportType() == FamilyVoterCardExportJob.ExportType.EXCEL) {
                generateAndUploadExcel(job, voters);
            } else {
                generateAndUploadPdf(job, voters);
            }
            
            job.setStatus(FamilyVoterCardExportJob.Status.COMPLETED);
        } catch (Exception ex) {
            log.error("Family voter card export job {} failed", jobId, ex);
            job.setStatus(FamilyVoterCardExportJob.Status.FAILED);
            job.setErrorMessage(ex.getMessage());
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            jobRepo.save(job);
        }
    }

    /**
     * Generate and upload PDF file
     */
    private void generateAndUploadPdf(FamilyVoterCardExportJob job, List<VoterEntity> voters) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean useHtml = true; // feature flag placeholder; externalize via config if needed
        String orderBy = job.getOrderBy() != null ? job.getOrderBy() : "family";
        
        if (useHtml) {
            // Map voters into simple value maps expected by renderer
            List<java.util.Map<String, Object>> models = voters.stream().map(v -> {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                put(m, "serialNo", v.getSerialNo());
                put(m, "partNo", v.getPartNo());
                put(m, "voterFnameEn", v.getVoterFnameEn());
                put(m, "voterLnameEn", v.getVoterLnameEn());
                put(m, "voterFnameL1", v.getVoterFnameL1());
                put(m, "voterLnameL1", v.getVoterLnameL1());
                put(m, "rlnFnameEn", v.getRlnFnameEn());
                put(m, "rlnLnameEn", v.getRlnLnameEn());
                put(m, "rlnFnameL1", v.getRlnFnameL1());
                put(m, "rlnLnameL1", v.getRlnLnameL1());
                put(m, "rlnType", v.getRlnType());
                put(m, "age", v.getAge());
                put(m, "gender", v.getGender());
                put(m, "fullAddress", v.getFullAddress());
                put(m, "mobileNo", v.getMobileNo());
                put(m, "familyCount", v.getFamilyCount());
                // Add family ID for grouping
                put(m, "familyId", v.getFamilyId() != null ? v.getFamilyId().toString() : null);
                // Newly added mappings for HTML renderer features
                put(m, "photoUrl", v.getPhotoUrl()); // photo display
                put(m, "star", v.getStarNumber());   // star indicator (Boolean)
                put(m, "epicId", v.getEpicNumber()); // EPIC label under photo
                if (v.getCaste() != null) {
                    try {
                        var caste = v.getCaste();
                        java.util.Map<String,Object> casteMap = new java.util.HashMap<>();
                        casteMap.put("casteName", caste.getCasteName());
                        casteMap.put("id", caste.getId());
                        m.put("caste", casteMap);
                    } catch (Exception ignore) { /* defensive: skip if lazy */ }
                }
                if (v.getParty() != null) {
                    try {
                        var party = v.getParty();
                        java.util.Map<String,Object> partyMap = new java.util.HashMap<>();
                        partyMap.put("partyName", party.getPartyName());
                        partyMap.put("partyShortName", party.getPartyShortName());
                        partyMap.put("partyImage", party.getPartyImage());
                        partyMap.put("id", party.getId());
                        m.put("party", partyMap);
                    } catch (Exception ignore) { /* skip if lazy */ }
                }
                if (v.getReligion() != null) {
                    try {
                        var rel = v.getReligion();
                        java.util.Map<String,Object> relMap = new java.util.HashMap<>();
                        relMap.put("religionName", rel.getReligionName());
                        relMap.put("religionImage", rel.getReligionImage());
                        relMap.put("id", rel.getId());
                        m.put("religion", relMap);
                    } catch (Exception ignore) { }
                }
                if (v.getAvailability1() != null) {
                    try {
                        var av = v.getAvailability1();
                        java.util.Map<String,Object> avMap = new java.util.HashMap<>();
                        avMap.put("availabilityName", av.getAvailabilityName());
                        avMap.put("availabilityImage", av.getAvailabilityImage());
                        avMap.put("description", av.getDescription());
                        avMap.put("id", av.getId());
                        m.put("availability", avMap);
                    } catch (Exception ignore) { }
                }
                return m;
            }).toList();
            
            // Choose rendering method based on orderBy preference (reuse orderBy variable from earlier)
            byte[] pdf;
            
            if (job.getPartNo() != null && "family".equals(orderBy)) {
                // Part export with family grouping and page breaks between families
                log.debug("FamilyCardExport: Rendering with family page breaks for jobId={}", job.getId());
                pdf = htmlPdfRenderer.renderWithFamilyPageBreaks(models, job.getColumns() == null ? 2 : job.getColumns());
            } else {
                // Regular continuous render (for single family or serial number ordering)
                log.debug("FamilyCardExport: Rendering continuous (no family page breaks) for jobId={}", job.getId());
                pdf = htmlPdfRenderer.render(models, job.getColumns() == null ? 2 : job.getColumns());
            }
            baos.write(pdf);
        } else {
            FamilyVoterCardPdfWriter.write(voters, baos);
        }
        
        // Write to temp file for upload convenience (existing upload util expects File)
        File tmp = File.createTempFile("family_cards_", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(baos.toByteArray());
        }
        String key = buildKey(job.getElectionId(), job.getFamilyId(), "pdf");
        String url = awsFileUpload.uploadToAWSForvoter(tmp, key, BUCKET);
        job.setS3Key(key);
        job.setS3Url(url);
        job.setRowCount((long) voters.size());
        tmp.delete(); // Clean up temp file
    }

    /**
     * Generate and upload Excel file
     */
    private void generateAndUploadExcel(FamilyVoterCardExportJob job, List<VoterEntity> voters) throws Exception {
        log.info("FamilyExcelExport: Generating Excel file with {} voters for jobId={}", voters.size(), job.getId());
        
        // Create temp file
        File tmp = File.createTempFile("family_excel_", ".xlsx");
        
        try {
            // Generate Excel file
            excelGenerator.generateExcel(voters, tmp);
            
            // Upload to S3
            String key = buildKey(job.getElectionId(), job.getFamilyId(), "xlsx");
            String url = awsFileUpload.uploadToAWSForvoter(tmp, key, BUCKET);
            
            job.setS3Key(key);
            job.setS3Url(url);
            job.setRowCount((long) voters.size());
            
            log.info("FamilyExcelExport: Successfully uploaded Excel file to S3 for jobId={}, url={}", job.getId(), url);
        } finally {
            // Clean up temp file
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    private String buildKey(Long electionId, UUID familyId, String extension) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String prefix = "xlsx".equals(extension) ? PREFIX_EXCEL : PREFIX_PDF;
        return prefix + "e" + electionId + "_family" + familyId + "_" + ts + "." + extension;
    }

    /**
     * Updates job progress with status message for user feedback
     */
    private void updateJobProgress(FamilyVoterCardExportJob job, String message) {
        job.setErrorMessage(message);
        jobRepo.save(job);
    }

    private void put(java.util.Map<String, Object> map, String key, Object value) {
        if (value != null) map.put(key, value);
    }
}
