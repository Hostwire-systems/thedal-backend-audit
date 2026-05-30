package com.thedal.thedal_app.voter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.dto.BulkUploadMemberStatusDto;
import com.thedal.thedal_app.voter.dto.BulkUploadResponse;
import com.thedal.thedal_app.voter.dto.MemberDTO;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberController {
	
	@Autowired
    private MemberService memberService;	
	@Autowired
    private ElectionRepository electionRepository;
	@Autowired
    private RequestDetailsService requestDetails;  

	private void validateElectionOwnership(Long electionId, Long accountId) {
        Optional<ElectionEntity> electionOpt = electionRepository.findByIdAndAccountId(electionId, accountId);
        if (!electionOpt.isPresent()) {
            log.error("Election ID {} does not belong to Account ID {}", electionId, accountId);
            throw new ThedalException(ThedalError.INVALID_ELECTION, HttpStatus.FORBIDDEN);  
        }
    }
	
	@Operation(summary = "Create a new member", description = "Saves a new member under a specific election")        
	@PostMapping("/election/{electionId}")
	public ThedalResponse<MemberDTO> saveMember(@PathVariable("electionId") Long electionId,
	                                            @RequestBody @Valid MemberDTO memberDto) {
	    
	    memberDto.setElectionId(electionId);
	    ThedalResponse<MemberDTO> savedMember = memberService.saveMember(electionId, memberDto);
	    new ThedalResponse<>(ThedalSuccess.MEMBER_CREATED, savedMember);
	    return savedMember;
	}
	
	@Operation(summary = "Get members by election ID", description = "Retrieves all members under a specific election")
	@GetMapping("/{electionId}")
    public ThedalResponse<List<MemberDTO>> getAllMembers(@PathVariable Long electionId,
    		@RequestParam(required = false) String epicNumber) {
        Long accountId = requestDetails.getCurrentAccountId();
        
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        validateElectionOwnership(electionId, accountId);
        return memberService.getAllMembers(electionId, accountId, epicNumber);
    }
	
//	@Operation(summary = "Get member by membership number and election ID", 
//	           description = "Retrieves a member based on membership number and election ID")
//	@GetMapping("/{electionId}/{membershipNo}")
//	public ThedalResponse<MemberDTO> getMemberByMembershipNoAndElectionId(
//	        @PathVariable Long electionId, 
//	        //@PathVariable String membershipNo,
//	        @RequestParam(required = false) String membershipNo,
//	        @RequestParam(required = false) String epicNumber) {
//	    
//	    Long accountId = requestDetails.getCurrentAccountId();
//	    
//	    if (accountId == null) {
//	        log.error("Account ID not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//	    
//	    validateElectionOwnership(electionId, accountId);
//	    return memberService.getMemberByMembershipNoAndElectionId(electionId, membershipNo, accountId, epicNumber);
//	}
	@Operation(
		    summary = "Get member by membership number or EPIC number and election ID",
		    description = "Retrieves a member based on membership number or EPIC number and election ID"
		)
		@GetMapping("/{electionId}/members-data")
		public ThedalResponse<MemberDTO> getMemberByMembershipNoOrEpicNumber(
		        @PathVariable Long electionId,
		        @RequestParam(required = false) String membershipNo,
		        @RequestParam(required = false) String epicNumber) {

		    Long accountId = requestDetails.getCurrentAccountId();
		    if (accountId == null) {
		        log.error("Account ID not found, unauthorized access.");
		        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
		    }

		    validateElectionOwnership(electionId, accountId);

		    return memberService.getMemberByMembershipNoOrEpicNumber(electionId, membershipNo, epicNumber, accountId);
		}


	
	@Operation(summary = "Update members by election ID and memberId", description = "Retrieves all members under a specific election")
	@PutMapping("/{electionId}/{memberId}")
	public ThedalResponse<MemberDTO> updateMember(
	        @PathVariable Long electionId,
	        @PathVariable Long memberId,
	        @RequestBody MemberDTO memberDTO) {

	    Long accountId = requestDetails.getCurrentAccountId();
	    
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    validateElectionOwnership(electionId, accountId);
	    return memberService.updateMember(electionId, memberId, accountId, memberDTO);
	}

	@Operation(summary = "Delete members by election ID", description = "Delete all members and delete multiple members option")
	@DeleteMapping("/election/{electionId}")
    public ThedalResponse<String> deleteMembers(
            @PathVariable Long electionId,
            @RequestParam(value = "memberIds", required = false) List<Long> memberIds) {

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        memberService.deleteMembers(electionId, accountId, memberIds);
        return new ThedalResponse<>(ThedalSuccess.MEMBER_DELETED);
    }
	
	@Operation(summary = "Upload bulk member Data", description = "Upload bulk member data using xlsx or csv files")    
	@PostMapping(value = "/{electionId}/upload", consumes = "multipart/form-data")
	public ResponseEntity<ThedalResponse<BulkUploadResponse>> uploadMembersFromXlsxOrCsv(
	        @PathVariable("electionId") Long electionId,
	        @RequestParam("file") MultipartFile file) throws IOException {
	    ThedalResponse<BulkUploadResponse> response = memberService.uploadMembersFromXlsxOrCsv(file, electionId);
	    return ResponseEntity.ok(response);
	}
	
	@Operation(summary = "Get Member Bulk Upload Status", description = "Retrieve the status of a member bulk upload using its ID and election ID.")
    @GetMapping("/{electionId}/{bulkUploadId}/status")
    public ResponseEntity<BulkUploadMemberStatusDto> getBulkUploadMemberStatus(           
            @PathVariable("electionId") Long electionId,
            @PathVariable("bulkUploadId") Long bulkUploadId) {
        BulkUploadMemberStatusDto statusDto = memberService.getBulkUploadMemberStatus(bulkUploadId, electionId);
        return ResponseEntity.ok(statusDto);
    }

}
