package com.thedal.thedal_app.voter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.voter.dto.FamilyMappingJobResponseDto;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.election.OtpService;
import com.thedal.thedal_app.dto.FamilyMappingOtpRequestDTO;
import com.thedal.thedal_app.dto.FamilyMappingOtpVerificationDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/voters")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Family Mapping", description = "APIs for running family mapping on voters by house number")
public class FamilyMappingController {

    @Autowired
    private FamilyMappingService familyMappingService;
    
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private OtpService otpService;

    @Operation(
        summary = "Run Family Mapping", 
        description = "Starts an asynchronous family mapping job that groups voters by house number (houseNoEn field) " +
                     "for an entire election. Creates families for any number of voters sharing the same house number. " +
                     "Returns a job ID that can be used to check the progress and status of the family mapping operation."
    )
    @PostMapping("/run-family/election/{electionId}")
    public ResponseEntity<ThedalResponse<FamilyMappingJobResponseDto>> runFamilyMapping(
            @Parameter(description = "The ID of the election to run family mapping for", required = true)
            @PathVariable("electionId") Long electionId) {
        
        log.info("Family mapping request received for electionId: {}", electionId);
        
        try {
            // Get current account ID
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
            
            // Check if bulk upload is in progress for this election
            boolean bulkUploadInProgress = familyMappingService.isBulkUploadInProgress(electionId, accountId);
            if (bulkUploadInProgress) {
                throw new ThedalException(ThedalError.BULK_UPLOAD_IN_PROGRESS, HttpStatus.CONFLICT);
            }
            
            // Start family mapping job
            FamilyMappingJobResponseDto jobResponse = familyMappingService.startFamilyMapping(electionId, accountId);
            
            ThedalResponse<FamilyMappingJobResponseDto> response = new ThedalResponse<>(
                ThedalSuccess.FAMILY_MAPPING_STARTED,
                jobResponse
            );
            
            log.info("Family mapping job started successfully. JobId: {}, ElectionId: {}, AccountId: {}", 
                     jobResponse.getJobId(), electionId, accountId);
            
            return ResponseEntity.ok(response);
            
        } catch (ThedalException e) {
            log.error("Family mapping failed for electionId {}: {}", electionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during family mapping for electionId {}: {}", electionId, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Get Family Mapping Job Status", 
        description = "Retrieves the current status and progress of a family mapping job. " +
                     "Shows total voters processed, families created, current progress percentage, " +
                     "job status (IN_PROGRESS, COMPLETED, FAILED), and detailed timing information."
    )
    @GetMapping("/family-job-status/{jobId}")
    public ResponseEntity<ThedalResponse<FamilyMappingJobResponseDto>> getFamilyMappingJobStatus(
            @Parameter(description = "The ID of the family mapping job to check status for", required = true)
            @PathVariable("jobId") Long jobId) {
        
        log.info("Family mapping status request received for jobId: {}", jobId);
        
        try {
            // Get current account ID
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
            
            // Get job status
            FamilyMappingJobResponseDto jobResponse = familyMappingService.getFamilyMappingJobStatus(jobId, accountId);
            
            ThedalResponse<FamilyMappingJobResponseDto> response = new ThedalResponse<>(
                ThedalSuccess.FAMILY_MAPPING_STATUS_RETRIEVED,
                jobResponse
            );
            
            log.info("Family mapping job status retrieved successfully. JobId: {}, Status: {}, Progress: {}%", 
                     jobId, jobResponse.getStatus(), jobResponse.getProgressPercentage());
            
            return ResponseEntity.ok(response);
            
        } catch (ThedalException e) {
            log.error("Failed to get family mapping job status for jobId {}: {}", jobId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error getting family mapping job status for jobId {}: {}", jobId, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(
        summary = "Check if Family Mapping has been run", 
        description = "Checks if family mapping has ever been executed for the given election. " +
                     "Returns true if family mapping has been run (completed or failed), false if never started. " +
                     "Used by frontend to disable the 'Run Family Mapping' button once it has been executed."
    )
    @GetMapping("/family-mapping-status/election/{electionId}")
    public ResponseEntity<ThedalResponse<Boolean>> checkFamilyMappingStatus(
            @Parameter(description = "The ID of the election to check family mapping status for", required = true)
            @PathVariable("electionId") Long electionId) {
        
        log.info("Family mapping status check request received for electionId: {}", electionId);
        
        try {
            // Get current account ID
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
            
            // Check if family mapping has ever been run for this election
            boolean hasBeenRun = familyMappingService.hasFamilyMappingBeenRun(electionId, accountId);
            
            ThedalResponse<Boolean> response = new ThedalResponse<>(
                ThedalSuccess.FAMILY_MAPPING_STATUS_RETRIEVED,
                hasBeenRun
            );
            
            log.info("Family mapping status checked successfully. ElectionId: {}, HasBeenRun: {}", 
                     electionId, hasBeenRun);
            
            return ResponseEntity.ok(response);
            
        } catch (ThedalException e) {
            log.error("Failed to check family mapping status for electionId {}: {}", electionId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error checking family mapping status for electionId {}: {}", electionId, e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Operation(
        summary = "Request OTP for Family Mapping", 
        description = "Generates and sends an OTP to the specified mobile number for family mapping verification. " +
                     "The OTP is valid for 5 minutes and must be verified before running the family mapping operation."
    )
    @PostMapping("/request-family-mapping-otp")
    public ResponseEntity<ThedalResponse<String>> requestFamilyMappingOtp(
            @Parameter(description = "OTP request details including election ID and mobile number", required = true)
            @RequestBody FamilyMappingOtpRequestDTO otpRequest) {
        
        log.info("Family mapping OTP request received for electionId: {}, mobileNumber: {}", 
                 otpRequest.getElectionId(), otpRequest.getMobileNumber());
        
        try {
            // Get current user ID
            Long userId = requestDetails.getCurrentUserId();
            if (userId == null) {
                throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
            
            // Generate OTP
            FamilyMappingOtp otpEntity = otpService.generateFamilyMappingOtp(
                userId, 
                otpRequest.getElectionId(), 
                otpRequest.getMobileNumber()
            );
            
            // Send OTP via SMS
            otpService.sendOtp(otpRequest.getMobileNumber(), otpEntity.getOtp());
            
            ThedalResponse<String> response = new ThedalResponse<>(
                ThedalSuccess.OTP_SENT,
                "OTP sent to mobile number " + otpRequest.getMobileNumber()
            );
            
            log.info("Family mapping OTP sent successfully to mobile: {}", otpRequest.getMobileNumber());
            return ResponseEntity.ok(response);
            
        } catch (ThedalException e) {
            log.error("Failed to send family mapping OTP for electionId {}: {}", 
                     otpRequest.getElectionId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error sending family mapping OTP for electionId {}: {}", 
                     otpRequest.getElectionId(), e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Operation(
        summary = "Verify OTP and Run Family Mapping", 
        description = "Verifies the OTP and runs family mapping if verification is successful. " +
                     "Starts an asynchronous family mapping job that groups voters by house number for an entire election. " +
                     "Returns a job ID that can be used to check the progress and status of the family mapping operation."
    )
    @PostMapping("/verify-otp-and-run-family-mapping")
    public ResponseEntity<ThedalResponse<FamilyMappingJobResponseDto>> verifyOtpAndRunFamilyMapping(
            @Parameter(description = "OTP verification details including election ID, mobile number, and OTP", required = true)
            @RequestBody FamilyMappingOtpVerificationDTO otpVerification) {
        
        log.info("Family mapping OTP verification request received for electionId: {}, mobileNumber: {}", 
                 otpVerification.getElectionId(), otpVerification.getMobileNumber());
        
        try {
            // Verify OTP
            Long verifiedElectionId = otpService.verifyFamilyMappingOtp(
                otpVerification.getElectionId(),
                otpVerification.getMobileNumber(),
                otpVerification.getOtp()
            );
            
            log.info("Family mapping OTP verified successfully for electionId: {}", verifiedElectionId);
            
            // Get current account ID
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                throw new ThedalException(ThedalError.UNAUTHORIZED_ACCESS, HttpStatus.UNAUTHORIZED);
            }
            
            // Check if bulk upload is in progress for this election
            boolean bulkUploadInProgress = familyMappingService.isBulkUploadInProgress(verifiedElectionId, accountId);
            if (bulkUploadInProgress) {
                throw new ThedalException(ThedalError.BULK_UPLOAD_IN_PROGRESS, HttpStatus.CONFLICT);
            }
            
            // Start family mapping job
            FamilyMappingJobResponseDto jobResponse = familyMappingService.startFamilyMapping(verifiedElectionId, accountId);
            
            ThedalResponse<FamilyMappingJobResponseDto> response = new ThedalResponse<>(
                ThedalSuccess.FAMILY_MAPPING_STARTED,
                jobResponse
            );
            
            log.info("Family mapping job started successfully after OTP verification. JobId: {}, ElectionId: {}, AccountId: {}", 
                     jobResponse.getJobId(), verifiedElectionId, accountId);
            
            return ResponseEntity.ok(response);
            
        } catch (ThedalException e) {
            log.error("Family mapping OTP verification failed for electionId {}: {}", 
                     otpVerification.getElectionId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during family mapping OTP verification for electionId {}: {}", 
                     otpVerification.getElectionId(), e.getMessage(), e);
            throw new ThedalException(ThedalError.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
