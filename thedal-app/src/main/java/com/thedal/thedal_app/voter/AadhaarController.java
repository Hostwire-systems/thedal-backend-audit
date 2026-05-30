package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.voter.dto.AadhaarDTO;
import com.thedal.thedal_app.voter.dto.AadhaarOtpRequestDTO;
import com.thedal.thedal_app.voter.dto.AadhaarOtpVerifyDTO;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/aadhaar")
@RequiredArgsConstructor
public class AadhaarController {

	@Autowired
    private AadhaarService aadhaarService; 

    @PostMapping("/election/{electionId}")
    @Operation(summary = "Create Aadhaar under election")
    public ThedalResponse<AadhaarEntity> save(@PathVariable Long electionId,
                                              @RequestBody @Valid AadhaarDTO dto) {
        AadhaarEntity saved = aadhaarService.saveAadhaar(electionId, dto);
        return new ThedalResponse<>(ThedalSuccess.AADHAAR_CREATED, saved);
    }
//	@PostMapping("/election/{electionId}")
//	public ThedalResponse<AadhaarEntity> save(@PathVariable Long electionId,
//	                                          @RequestBody @Valid AadhaarDTO dto) {
//	    try {
//	        AadhaarEntity saved = aadhaarService.saveAadhaar(electionId, dto);
//	        return new ThedalResponse<>(ThedalSuccess.AADHAAR_CREATED, saved);
//	    } catch (IllegalArgumentException e) {
//	        return new ThedalResponse<>(ThedalError.AADHAAR_ALREADY_EXISTS);
//	    }
//	}

    @GetMapping("/{id}/election/{electionId}")
    @Operation(summary = "Fetch Aadhaar by ID and election")
    public ThedalResponse<AadhaarEntity> getById(@PathVariable Long id,
                                                 @PathVariable Long electionId) {
        AadhaarEntity aadhaar = aadhaarService.getById(id, electionId);
        return new ThedalResponse<>(ThedalSuccess.AADHAAR_FETCHED, aadhaar);
    }
    
    @GetMapping("/election/{electionId}")
    @Operation(summary = "Fetch all Aadhaar records for an election")
    public ThedalResponse<List<AadhaarEntity>> getAllByElectionId(@PathVariable Long electionId) {
        List<AadhaarEntity> aadhaarList = aadhaarService.getAllByElectionId(electionId);
        return new ThedalResponse<>(ThedalSuccess.AADHAAR_FETCHED, aadhaarList);
    }

    @DeleteMapping("/{id}/election/{electionId}")
    @Operation(summary = "Delete Aadhaar by ID and election")
    public ThedalResponse<String> delete(@PathVariable Long id,
                                         @PathVariable Long electionId) {
        aadhaarService.deleteById(id, electionId);
        return new ThedalResponse<>(ThedalSuccess.AADHAAR_DELETED);
    }
    
    @PostMapping("/otp")
    @Operation(summary = "Request OTP for Aadhaar verification")
    public ThedalResponse<Map<String, Object>> requestOtp(@RequestBody @Valid AadhaarOtpRequestDTO dto) {
        Map<String, Object> response = aadhaarService.requestAadhaarOtp(dto.getAadhaarNumber());
        return new ThedalResponse<>(ThedalSuccess.OTP_REQUESTED, response);
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP and get Aadhaar details")
    public ThedalResponse<AadhaarEntity> verifyOtp(@RequestBody @Valid AadhaarOtpVerifyDTO dto) {
        AadhaarEntity savedEntity = aadhaarService.verifyAadhaarOtp(dto.getOtp(), dto.getRefId());
        return new ThedalResponse<>(ThedalSuccess.AADHAAR_VERIFIED, savedEntity);
    }
    
}