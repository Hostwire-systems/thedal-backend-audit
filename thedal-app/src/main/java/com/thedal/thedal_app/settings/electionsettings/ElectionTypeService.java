package com.thedal.thedal_app.settings.electionsettings;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.settings.electionsettings.dto.ElectionTypeRequest;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ElectionTypeService {
	
	@Autowired
    private ElectionTypeRepository electionTypeRepository;
	@Autowired
    private RequestDetailsService requestDetails;
	
	
	@Transactional
	public ThedalResponse<ElectionType> createElectionType(ElectionTypeRequest electionTypeRequest) {
		
		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account ID not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    log.info("Received request to create election type: {} for account ID: {}", electionTypeRequest.getElectionType(), accountId);

	    if (electionTypeRequest.getElectionType() == null) {
	        log.error("Missing required fields: electionType for account ID: {}", accountId);
	        throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, HttpStatus.BAD_REQUEST);
	    }

	    ElectionType electionType = new ElectionType();
	    electionType.setElectionType(electionTypeRequest.getElectionType());
	    electionType.setAccountId(accountId);

	    electionTypeRepository.save(electionType);
	    electionTypeRepository.flush();

	    log.info("Election Type created successfully: {} for account ID: {}", electionType.getElectionType(), accountId);

	    return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_CREATED, electionType);
	}
	
	@Transactional
	public ElectionType getElectionTypeById(Long id, Long accountId) {
		
	    ElectionType electionType = electionTypeRepository.findByIdAndAccountId(id, accountId)
	            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND));

	    return electionType;
	}
	
	@Transactional
	public List<ElectionType> getAllElectionTypes(Long accountId) {
		
		return electionTypeRepository.findAllByAccountId(accountId);
	}

	
	@Transactional
	public ElectionType updateElectionType(Long id, ElectionTypeRequest electionTypeRequest) {
		
		Long accountId = requestDetails.getCurrentAccountId();  // Retrieve accountId from session/context

	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    // Fetch the existing ElectionType by ID and accountId
	    ElectionType existingElectionType = electionTypeRepository.findByIdAndAccountId(id, accountId)
	            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND));

	    if (electionTypeRequest.getElectionType() == null || electionTypeRequest.getElectionType().isEmpty()) {
	        throw new ThedalException(ThedalError.ELECTION_TYPE_CANNOT_BE_NULL_OR_EMPTY, HttpStatus.BAD_REQUEST);
	    }
	    
	    existingElectionType.setElectionType(electionTypeRequest.getElectionType());
	    
	    electionTypeRepository.save(existingElectionType);
	    electionTypeRepository.flush();

	    return existingElectionType;
	}

	@Transactional
	public ThedalResponse<Void> deleteElectionType(Long id) {
		
		Long accountId = requestDetails.getCurrentAccountId(); 

	    if (accountId == null) {
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }

	    ElectionType electionType = electionTypeRepository.findByIdAndAccountId(id, accountId)
	            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND));

	    electionTypeRepository.delete(electionType);
	    electionTypeRepository.flush();

	    log.info("ElectionType with ID {} for account ID {} has been deleted.", id, accountId);
	    return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_DELETED);
	}
	
	//@Transactional
//	public ThedalResponse<Void> deleteElectionType(Long id) {
//	    if (id == null || id <= 0) {
//	        throw new ThedalException(ThedalError.INVALID_ELECTION_TYPE_ID, HttpStatus.BAD_REQUEST,
//	                "The provided ElectionType ID is invalid.");
//	    }
//
//	    Long accountId = requestDetails.getCurrentAccountId();
//
//	    if (accountId == null) {
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED,
//	                "Account ID is missing. Please log in again.");
//	    }
//
//	    ElectionType electionType = electionTypeRepository.findByIdAndAccountId(id, accountId)
//	            .orElseThrow(() -> new ThedalException(ThedalError.ELECTION_TYPE_NOT_FOUND, HttpStatus.NOT_FOUND,
//	                    String.format("ElectionType with ID %d not found for account ID %d.", id, accountId)));
//
//	    electionTypeRepository.delete(electionType);
//	    electionTypeRepository.flush();
//
//	    log.info("ElectionType with ID {} for account ID {} has been deleted.", id, accountId);
//
//	    return new ThedalResponse<>(ThedalSuccess.ELECTION_TYPE_DELETED);
//	}



}
