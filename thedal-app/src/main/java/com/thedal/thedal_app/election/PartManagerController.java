package com.thedal.thedal_app.election;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.election.dtos.PartManagerDTO;
import com.thedal.thedal_app.election.dtos.PartManagerExportRequest;
import com.thedal.thedal_app.election.dtos.PartManagerExportResponse;
import com.thedal.thedal_app.election.dtos.PartManagerExportStatusResponse;
import com.thedal.thedal_app.election.dtos.PartManagerResponseDTO;
import com.thedal.thedal_app.election.dtos.PartManagerVulnerabilityReorderRequest;
import com.thedal.thedal_app.election.dtos.PartManagerVulnerabilityResponseDTO;
import com.thedal.thedal_app.election.dtos.PartManagerVulnerabilityUpdateDTO;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/elections/partmanager")
@Slf4j
public class PartManagerController {

@Autowired
 private RequestDetailsService requestDetails;
 @Autowired
 private PartMangerService partManagerService;
 @Autowired
 private ElectionRepository electionRepository;

 private void validateElectionOwnership(Long electionId, Long accountId) {
     Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
     if (!electionOpt.isPresent()) {
         log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
         throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
     }
 } 

    @PostMapping(value = "/{electionId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ThedalResponse<PartManagerResponseDTO>> createPartManagerMultipart(
            @PathVariable("electionId") Long electionId, 
            @RequestPart(value = "partManagerData") String partManagerDataJson,
            @RequestPart(value = "partImage", required = false) MultipartFile partImage) throws IOException {
        
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        PartManagerDTO dto = mapper.readValue(partManagerDataJson, PartManagerDTO.class);
        
        log.info("Received partmanager: {}, with image: {}", dto, partImage != null ? partImage.getOriginalFilename() : "none");

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        PartManagerResponseDTO partmanagerReponse = partManagerService.savePartManager(electionId, accountId, dto, partImage);
        ThedalResponse<PartManagerResponseDTO> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_CREATED, partmanagerReponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{electionId}", consumes = {"application/json"})
    public ResponseEntity<ThedalResponse<PartManagerResponseDTO>> createPartManagerJson(
            @PathVariable("electionId") Long electionId, 
            @RequestBody PartManagerDTO dto) throws IOException {
        
        log.info("Received partmanager: {}", dto);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        PartManagerResponseDTO partmanagerReponse = partManagerService.savePartManager(electionId, accountId, dto, null);
        ThedalResponse<PartManagerResponseDTO> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_CREATED, partmanagerReponse);
        return ResponseEntity.ok(response);
    }

    // @GetMapping("/{electionId}/{partManagerId}")
    // public ResponseEntity<PartManagerResponseDTO> getPartManager(@PathVariable("electionId") Long electionId, @PathVariable("partManagerId") Long partManagerId) {
    //     PartManagerResponseDTO partManagerDTO = partManagerService.getPartManagers(electionId, partManagerId,partManagerId);
    //     return ResponseEntity.ok(partManagerDTO);
    // }

    @PutMapping(value = "/{electionId}/{partManagerId}", consumes = {"multipart/form-data"})
    public ResponseEntity<ThedalResponse<PartManagerResponseDTO>> updatePartManagerMultipart(
            @PathVariable("electionId") Long electionId, 
            @PathVariable("partManagerId") Long partManagerId, 
            @RequestPart(value = "partManagerData") String partManagerDataJson,
            @RequestPart(value = "partImage", required = false) MultipartFile partImage) throws IOException {
        
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        PartManagerDTO dto = mapper.readValue(partManagerDataJson, PartManagerDTO.class);
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
    
        PartManagerResponseDTO updatedPartManager = partManagerService.updatePartManager(electionId, accountId, partManagerId, dto, partImage);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.PARTMANAGER_UPDATED, updatedPartManager));
    }

    @PutMapping(value = "/{electionId}/{partManagerId}", consumes = {"application/json"})
    public ResponseEntity<ThedalResponse<PartManagerResponseDTO>> updatePartManagerJson(
            @PathVariable("electionId") Long electionId, 
            @PathVariable("partManagerId") Long partManagerId, 
            @RequestBody PartManagerDTO dto) throws IOException {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
    
        PartManagerResponseDTO updatedPartManager = partManagerService.updatePartManager(electionId, accountId, partManagerId, dto, null);
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.PARTMANAGER_UPDATED, updatedPartManager));
    }

    @GetMapping("/{electionId}/{partId}")
    public ResponseEntity<PartManagerResponseDTO> getPartManagerByPartId(
            @PathVariable("electionId") Long electionId,
            @PathVariable("partId") Long partId) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        PartManagerResponseDTO response = partManagerService.getPartManagerByElectionIdAndPartId(electionId, accountId, partId);
        return ResponseEntity.ok(response);
    }



    @GetMapping("/{electionId}")
    public ResponseEntity<?> getAllPartManagers(@PathVariable("electionId") Long electionId,
    		@RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        Long accountId = requestDetails.getCurrentAccountId();
        Long userId = requestDetails.getCurrentUserId();
        
        log.info("=== PARTMANAGER CONTROLLER DEBUG ===");
        log.info("getAllPartManagers called - electionId: {}, accountId: {}, userId: {}, page: {}, size: {}", 
            electionId, accountId, userId, page, size);
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
     // If pagination parameters are provided, return paginated response
        if (page != null && size != null) {
            log.info("Using paginated response");
            Page<PartManagerResponseDTO> paginatedResult = partManagerService.getPartManagersPaginated(electionId, accountId, page, size);
            return ResponseEntity.ok(paginatedResult);
        }

        log.info("Using non-paginated response - calling partManagerService.getPartManagers");
        List<PartManagerResponseDTO> partManagers = partManagerService.getPartManagers(electionId,accountId);
        log.info("Controller received {} part managers from service", partManagers.size());
        return ResponseEntity.ok(partManagers);
    }
   
    @DeleteMapping("/{electionId}/{partManagerId}")
    @Transactional
    public ResponseEntity<ThedalResponse<Void>> deletePartManager(
        @PathVariable("electionId") Long electionId,
        @PathVariable("partManagerId") Long partManagerId) {

    Long accountId = requestDetails.getCurrentAccountId();
    if (accountId == null) {
        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
    }

    partManagerService.deletePartManager(electionId, accountId, partManagerId);
    return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.PARTMANAGER_DELETED, null));
  }
    
    @DeleteMapping("/election/{electionId}")
    public ThedalResponse<Void> deletePartManagers(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "partManagerIds", required = false) List<Long> partManagerIds) {

        // Convert null partManagerIds to an empty list
        List<Long> partManagerIdList = (partManagerIds != null && !partManagerIds.isEmpty()) 
                ? partManagerIds 
                : Collections.emptyList();

        return partManagerService.deletePartManagers(electionId, partManagerIdList);
    }

    

//@Operation(summary = "Upload bulk PartManager Data", description = "Upload bulk PartManager data using xlsx or csv files.")
//@PostMapping(value = "/election/{electionId}/bulk-upload", consumes = "multipart/form-data")
//public ResponseEntity<ThedalResponse<Void>> uploadPartManagersFromXlsxOrCsv(
//    @RequestParam("file") MultipartFile file,
//    @PathVariable Long electionId) throws IOException {
//
//    // Call the service to handle the file processing and upload
//    ThedalResponse<Void> response = partManagerService.bulkUploadPartManagersFromXlsxOrCsv(file, electionId);
//    return ResponseEntity.ok(response);
//}
    @Operation(summary = "Upload bulk PartManager Data", description = "Upload bulk PartManager data using xlsx or csv files.")
    @PostMapping(value = "/election/{electionId}/bulk-upload", consumes = "multipart/form-data")
    public ResponseEntity<ThedalResponse<PartManagerBulkUploadEntity>> uploadPartManagersFromXlsxOrCsv(
        @RequestParam("file") MultipartFile file,
        @PathVariable Long electionId) throws IOException {

        // Check file size (optional, aligning with Voter API)
        if (file.getSize() > 100 * 1024 * 1024) { // 100 MB
            throw new ThedalException(ThedalError.FILE_TOO_LARGE, HttpStatus.BAD_REQUEST);
        }

        // Call the service to handle the file processing and upload
        ThedalResponse<PartManagerBulkUploadEntity> response = partManagerService.bulkUploadPartManagersFromXlsxOrCsv(file, electionId);
        return ResponseEntity.ok(response);
    }

    ////////////////////////////////////////////
    
//    @PostMapping("/vulnerability/{electionId}")
//    public ResponseEntity<ThedalResponse<PartManagerVulnerabilityResponseDTO>> createPartManager1(
//            @PathVariable("electionId") Long electionId,
//            @RequestBody PartManagerDTO partManagerDTO) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//        validateElectionOwnership(electionId, accountId);
//
//        PartManagerVulnerabilityResponseDTO partManagerResponse = partManagerService.createPartManager1(accountId, electionId, partManagerDTO);
//        ThedalResponse<PartManagerVulnerabilityResponseDTO> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_CREATED, partManagerResponse);
//        return ResponseEntity.ok(response);
//    }

    @GetMapping("/vulnerability/{electionId}")
    public ResponseEntity<ThedalResponse<Page<PartManagerVulnerabilityResponseDTO>>> getPartManagersByElectionId(
            @PathVariable("electionId") Long electionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }
            validateElectionOwnership(electionId, accountId);

            // Remove sorting from Pageable since our native query already handles sorting properly
            Pageable pageable = PageRequest.of(page, size);
            Page<PartManagerVulnerabilityResponseDTO> partManagers = partManagerService.findAllByElectionIdAndAccountId(electionId, accountId, pageable);

            ThedalResponse<Page<PartManagerVulnerabilityResponseDTO>> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_FETCHED, partManagers);
            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching part managers for election ID {}: {}", electionId, e.getMessage());
            Page<PartManagerVulnerabilityResponseDTO> emptyPage = Page.empty();
            ThedalResponse<Page<PartManagerVulnerabilityResponseDTO>> errorResponse = new ThedalResponse<>(ThedalError.PARTMANAGER_FETCH_FAILED, emptyPage);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @GetMapping("/vulnerability/{electionId}/{partNo}")
    public ResponseEntity<ThedalResponse<PartManagerVulnerabilityResponseDTO>> getPartManagerByPartNo(
            @PathVariable("electionId") Long electionId,
            @PathVariable("partNo") String partNo) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);

        try {
            PartManagerVulnerabilityResponseDTO partManager = partManagerService.findByElectionIdAndPartNoAndAccountId(electionId, partNo, accountId);
            ThedalResponse<PartManagerVulnerabilityResponseDTO> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_FETCHED, partManager);
            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching part manager with partNo {} for election ID {}: {}", partNo, electionId, e.getMessage());
            ThedalResponse<PartManagerVulnerabilityResponseDTO> errorResponse = new ThedalResponse<>(ThedalError.PARTMANAGER_FETCH_FAILED, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
    }

    @PutMapping("/vulnerability/{electionId}/{partNo}")
    public ResponseEntity<ThedalResponse<PartManagerVulnerabilityResponseDTO>> updatePartManagerVulnerability(
            @PathVariable("electionId") Long electionId,
            @PathVariable("partNo") String partNo,
            @RequestBody PartManagerVulnerabilityUpdateDTO updateDTO) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);

        try {
            PartManagerVulnerabilityResponseDTO partManagerResponse = partManagerService.updateVulnerabilityByElectionIdAndPartNo(electionId, partNo, updateDTO, accountId);
            ThedalResponse<PartManagerVulnerabilityResponseDTO> response = new ThedalResponse<>(ThedalSuccess.PARTMANAGER_VULNERABILITY_UPDATED, partManagerResponse);
            return ResponseEntity.ok(response);
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating part manager with partNo {} for election ID {}: {}", partNo, electionId, e.getMessage());
            ThedalResponse<PartManagerVulnerabilityResponseDTO> errorResponse = new ThedalResponse<>(ThedalError.PARTMANAGER_UPDATE_FAILED, null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/partnumbers/{electionId}")
    public ThedalResponse<Page<String>> getAllPartNumbers(
            @PathVariable("electionId") Long electionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        log.info("Fetching part numbers for electionId: {}, page: {}, size: {}", electionId, page, size);
        return partManagerService.getAllPartNumbers(electionId, accountId, PageRequest.of(page, size));
    }
    
    @Operation(summary = "Vulnerability reorder API", description = "Reorder part managers for vulnerability page based on partNo")
    @PutMapping("/vulnerability/{electionId}/reorder")
    public ResponseEntity<ThedalResponse<String>> reorderVulnerabilityPartManagers(
            @PathVariable Long electionId,
            @RequestBody List<PartManagerVulnerabilityReorderRequest> reorderRequests) {
        try {
            Long accountId = requestDetails.getCurrentAccountId();
            if (accountId == null) {
                log.error("Account ID not found, unauthorized access.");
                throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
            }

            partManagerService.updateVulnerabilityPartManagerOrder(reorderRequests, accountId, electionId);
            return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.PARTMANAGER_ORDER_UPDATED));
        } catch (ThedalException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error reordering vulnerability part managers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ThedalResponse<>(ThedalError.PARTMANAGER_ORDER_UPDATE_FAILED));
        }
    }
    
    // ==================== PART MANAGER EXPORT ENDPOINTS ====================
    
    @Operation(summary = "Export part managers to PDF or Excel", 
               description = "Initiates an async export job for all part managers in an election. Supports PDF and Excel formats.",
               tags = {"Part Manager Export"})
    @PostMapping("/{electionId}/export")
    public ResponseEntity<ThedalResponse<PartManagerExportResponse>> exportPartManagers(
            @PathVariable("electionId") Long electionId,
            @RequestBody PartManagerExportRequest request) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, accountId);
        
        log.info("Initiating part manager export for electionId: {}, accountId: {}, format: {}", 
                electionId, accountId, request.getFormat());
        
        PartManagerExportResponse response = partManagerService.initiatePartManagerExport(
                electionId, accountId, request.getFormat());
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.EXPORT_INITIATED, response));
    }
    
    @Operation(summary = "Get part manager export job status", 
               description = "Retrieves the current status of a part manager export job including download URL when completed.",
               tags = {"Part Manager Export"})
    @GetMapping("/{electionId}/export/status/{jobId}")
    public ResponseEntity<ThedalResponse<PartManagerExportStatusResponse>> getExportStatus(
            @PathVariable("electionId") Long electionId,
            @PathVariable("jobId") Long jobId) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, accountId);
        
        log.info("Getting export status for jobId: {}, electionId: {}", jobId, electionId);
        
        PartManagerExportStatusResponse response = partManagerService.getExportStatus(jobId, accountId);
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }
    
    @Operation(summary = "Download part manager export file", 
               description = "Returns the download URL for a completed part manager export job.",
               tags = {"Part Manager Export"})
    @GetMapping("/{electionId}/export/download/{jobId}")
    public ResponseEntity<ThedalResponse<String>> downloadExport(
            @PathVariable("electionId") Long electionId,
            @PathVariable("jobId") Long jobId) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, accountId);
        
        PartManagerExportStatusResponse status = partManagerService.getExportStatus(jobId, accountId);
        
        if (!"COMPLETED".equals(status.getStatus())) {
            throw new ThedalException(ThedalError.EXPORT_NOT_READY, HttpStatus.BAD_REQUEST);
        }
        
        if (status.getAwsS3DownloadUrl() == null && status.getLocalFilePath() == null) {
            throw new ThedalException(ThedalError.EXPORT_FILE_NOT_FOUND, HttpStatus.NOT_FOUND);
        }
        
        String downloadUrl = status.getAwsS3DownloadUrl() != null ? 
                status.getAwsS3DownloadUrl() : status.getLocalFilePath();
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, downloadUrl));
    }
    
    @Operation(summary = "Get all part manager export jobs", 
               description = "Retrieves paginated list of all part manager export jobs for an election with optional status filter.",
               tags = {"Part Manager Export"})
    @GetMapping("/{electionId}/exports")
    public ResponseEntity<ThedalResponse<Page<PartManagerExportStatusResponse>>> getAllExports(
            @PathVariable("electionId") Long electionId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        validateElectionOwnership(electionId, accountId);
        
        log.info("Getting all export jobs for electionId: {}, status: {}, page: {}, size: {}", 
                electionId, status, page, size);
        
        LocalDateTime start = null;
        LocalDateTime end = null;
        
        if (startDate != null && !startDate.isEmpty()) {
            start = LocalDateTime.parse(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            end = LocalDateTime.parse(endDate);
        }
        
        Page<PartManagerExportStatusResponse> response = partManagerService.getExportJobs(
                electionId, accountId, status, start, end, PageRequest.of(page, size));
        
        return ResponseEntity.ok(new ThedalResponse<>(ThedalSuccess.SUCCESS, response));
    }

}
