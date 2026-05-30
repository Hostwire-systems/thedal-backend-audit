package com.thedal.thedal_app.voter.familyexport;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class FamilyVoterCardExportController {

    private final FamilyVoterCardExportJobRepository jobRepo;
    private final FamilyVoterCardExportAsyncService asyncService;
    private final ElectionRepository electionRepository;

    @PostMapping("/families/{familyId}/election/{electionId}/voter-cards/export-jobs")
    public ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> createJob(@PathVariable("familyId") String familyIdRaw,
                                                                              @PathVariable Long electionId,
                                                                              @RequestParam Long accountId,
                                                                              @RequestParam(required = false) Integer partNo,
                                                                              @RequestParam(required = false, defaultValue = "2") Integer columns,
                                                                              @RequestParam(required = false, defaultValue = "family") String orderBy) {
        return createExportJob(familyIdRaw, electionId, accountId, partNo, columns, orderBy, FamilyVoterCardExportJob.ExportType.PDF);
    }

    @PostMapping("/family-export/excel")
    public ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> createExcelExportJob(
                                                                              @RequestBody FamilyExcelExportRequest request) {
        // Validate exportType
        if (!"family".equals(request.getExportType()) && !"part".equals(request.getExportType())) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        
        // Validate required parameters based on exportType
        if ("family".equals(request.getExportType()) && (request.getFamilyId() == null || request.getFamilyId().isEmpty())) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        if ("part".equals(request.getExportType()) && request.getPartNo() == null) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        
        // For part export, use a placeholder family ID
        String familyIdToUse = "part".equals(request.getExportType()) ? UUID.randomUUID().toString() : request.getFamilyId();
        Integer partNoToUse = "part".equals(request.getExportType()) ? request.getPartNo() : null;
        String orderBy = request.getOrderBy() != null ? request.getOrderBy() : "family";
        
        return createExportJob(familyIdToUse, request.getElectionId(), request.getAccountId(), partNoToUse, 2, orderBy, FamilyVoterCardExportJob.ExportType.EXCEL);
    }

    @GetMapping("/family-export/excel/{jobId}/status")
    public ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> getExcelJobStatus(@PathVariable Long jobId,
                                                                                       @RequestParam Long accountId) {
        FamilyVoterCardExportJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, null));
        }
        
        // Verify account ownership through election
        ElectionEntity election = electionRepository.findById(job.getElectionId()).orElse(null);
        if (election == null || !election.getAccountId().equals(accountId)) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, null));
        }
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, job));
    }

    private ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> createExportJob(
                                                                              String familyIdRaw,
                                                                              Long electionId,
                                                                              Long accountId,
                                                                              Integer partNo,
                                                                              Integer columns,
                                                                              String orderBy,
                                                                              FamilyVoterCardExportJob.ExportType exportType) {
        // Basic validation
        if (columns != null && columns != 2 && columns != 3) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        if (partNo != null && partNo <= 0) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        // Validate orderBy parameter
        if (orderBy != null && !orderBy.equals("family") && !orderBy.equals("serial")) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        
        // SECURITY FIX: Get the correct account ID from the election instead of trusting client parameter
        ElectionEntity election = electionRepository.findById(electionId).orElse(null);
        if (election == null) {
            log.error("FamilyExportController: Election {} not found", electionId);
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.ELECTION_NOT_FOUND));
        }
        
        Long correctAccountId = election.getAccountId();
        if (!correctAccountId.equals(accountId)) {
            log.warn("FamilyExportController: Client passed accountId={} but election {} belongs to accountId={}. Using correct accountId.", 
                    accountId, electionId, correctAccountId);
        }
        
        UUID familyId;
        if (partNo != null) {
            // For part-wide export we allow a non-UUID placeholder; generate UUID if invalid
            try { familyId = UUID.fromString(familyIdRaw); } catch (IllegalArgumentException ex) { familyId = UUID.randomUUID(); }
        } else {
            try { familyId = UUID.fromString(familyIdRaw); } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
            }
        }
        FamilyVoterCardExportJob job = new FamilyVoterCardExportJob();
        job.setFamilyId(familyId);
        job.setElectionId(electionId);
        job.setAccountId(correctAccountId); // Use the correct account ID from the election
        if (partNo != null) job.setPartNo(partNo);
        job.setColumns(columns != null && columns == 3 ? 3 : 2);
        job.setOrderBy(orderBy != null ? orderBy : "family"); // Set ordering preference
        job.setExportType(exportType); // Set export type (PDF or EXCEL)
        job = jobRepo.save(job);
        
        log.info("FamilyExportController: Created export job {} for familyId={}, electionId={}, accountId={}, exportType={}", 
                job.getId(), familyId, electionId, correctAccountId, exportType);
        
        asyncService.execute(job.getId());
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, job));
    }

    @GetMapping("/families/{familyId}/election/{electionId}/voter-cards/export-jobs/{jobId}")
    public ResponseEntity<ThedalResponse<FamilyVoterCardExportJob>> getJob(@PathVariable("familyId") String familyIdRaw,
                                                                           @PathVariable Long electionId,
                                                                           @PathVariable Long jobId,
                                                                           @RequestParam Long accountId) {
        // SECURITY FIX: Get the correct account ID from the election
        ElectionEntity election = electionRepository.findById(electionId).orElse(null);
        if (election == null) {
            log.error("FamilyExportController: Election {} not found for getJob", electionId);
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.ELECTION_NOT_FOUND));
        }
        
        Long correctAccountId = election.getAccountId();
        
        UUID familyId;
        try { familyId = UUID.fromString(familyIdRaw); } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        FamilyVoterCardExportJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null || !job.getFamilyId().equals(familyId) || !job.getElectionId().equals(electionId) || !job.getAccountId().equals(correctAccountId)) {
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, null));
        }
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, job));
    }

    @GetMapping("/families/{familyId}/election/{electionId}/voter-cards/export-jobs")
    public ResponseEntity<ThedalResponse<Page<FamilyVoterCardExportJob>>> listJobs(@PathVariable("familyId") String familyIdRaw,
                                                                                   @PathVariable Long electionId,
                                                                                   @RequestParam Long accountId,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size) {
        // SECURITY FIX: Get the correct account ID from the election
        ElectionEntity election = electionRepository.findById(electionId).orElse(null);
        if (election == null) {
            log.error("FamilyExportController: Election {} not found for listJobs", electionId);
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.ELECTION_NOT_FOUND));
        }
        
        Long correctAccountId = election.getAccountId();
        
        UUID familyId;
        try { familyId = UUID.fromString(familyIdRaw); } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.INVALID_INPUT));
        }
        Page<FamilyVoterCardExportJob> jobs = jobRepo.findByAccountIdAndElectionIdAndFamilyId(
            correctAccountId, electionId, familyId, 
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, jobs));
    }

    @GetMapping("/family-export/jobs")
    public ResponseEntity<ThedalResponse<Page<FamilyVoterCardExportJob>>> listAllExportJobs(
                                                                                   @RequestParam Long electionId,
                                                                                   @RequestParam Long accountId,
                                                                                   @RequestParam(required = false) String exportType, // "PDF" or "EXCEL"
                                                                                   @RequestParam(required = false) Integer partNo,
                                                                                   @RequestParam(defaultValue = "0") int page,
                                                                                   @RequestParam(defaultValue = "10") int size) {
        // Verify election belongs to account
        ElectionEntity election = electionRepository.findById(electionId).orElse(null);
        if (election == null) {
            log.error("FamilyExportController: Election {} not found for listAllExportJobs", electionId);
            return ResponseEntity.badRequest().body(new ThedalResponse<>(ThedalError.ELECTION_NOT_FOUND));
        }
        
        Long correctAccountId = election.getAccountId();
        if (!correctAccountId.equals(accountId)) {
            log.warn("FamilyExportController: Account mismatch for listAllExportJobs");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FamilyVoterCardExportJob> jobs;
        
        // Filter by export type if provided
        if (exportType != null && (exportType.equals("PDF") || exportType.equals("EXCEL"))) {
            FamilyVoterCardExportJob.ExportType type = FamilyVoterCardExportJob.ExportType.valueOf(exportType);
            jobs = jobRepo.findByAccountIdAndElectionIdAndExportType(correctAccountId, electionId, type, pageable);
        } 
        // Filter by part number if provided
        else if (partNo != null) {
            jobs = jobRepo.findByAccountIdAndElectionIdAndPartNo(correctAccountId, electionId, partNo, pageable);
        }
        // List all jobs for the election
        else {
            jobs = jobRepo.findByAccountIdAndElectionId(correctAccountId, electionId, pageable);
        }
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, jobs));
    }
}
