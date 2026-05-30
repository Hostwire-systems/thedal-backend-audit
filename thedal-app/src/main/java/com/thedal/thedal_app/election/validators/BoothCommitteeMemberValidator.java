package com.thedal.thedal_app.election.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

import com.thedal.thedal_app.election.dtos.BoothCommitteeMemberDTO;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;

import lombok.extern.slf4j.Slf4j;

/**
 * Validator for Booth Committee Members
 * Validates array size, name, designation, and mobile number
 */
@Slf4j
public class BoothCommitteeMemberValidator {
    
    private static final int MAX_COMMITTEE_MEMBERS = 15;
    private static final int MIN_NAME_LENGTH = 1;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MIN_DESIGNATION_LENGTH = 1;
    private static final int MAX_DESIGNATION_LENGTH = 50;
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^[0-9]{10}$");
    
    /**
     * Validate booth committee members array
     * @param members List of committee members
     * @param electionId Election ID for logging
     * @param accountId Account ID for logging
     * @throws ThedalException if validation fails
     */
    public static void validate(List<BoothCommitteeMemberDTO> members, Long electionId, Long accountId) {
        if (members == null) {
            return; // Null is allowed (will default to empty array)
        }
        
        // Validate array size
        if (members.size() > MAX_COMMITTEE_MEMBERS) {
            log.error("Too many booth committee members: {} (max: {}), electionId: {}, accountId: {}", 
                members.size(), MAX_COMMITTEE_MEMBERS, electionId, accountId);
            throw new ThedalException(
                ThedalError.INVALID_FILE_DATA, 
                HttpStatus.BAD_REQUEST,
                "Booth committee members cannot exceed " + MAX_COMMITTEE_MEMBERS + " entries"
            );
        }
        
        // Validate each member
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            BoothCommitteeMemberDTO member = members.get(i);
            validateMember(member, i, errors);
        }
        
        // Throw exception with all validation errors
        if (!errors.isEmpty()) {
            String errorMessage = "Booth committee members validation failed:\n" + String.join("\n", errors);
            log.error("Validation errors: {}, electionId: {}, accountId: {}", errorMessage, electionId, accountId);
            throw new ThedalException(
                ThedalError.INVALID_FILE_DATA, 
                HttpStatus.BAD_REQUEST,
                errorMessage
            );
        }
    }
    
    /**
     * Validate individual committee member
     */
    private static void validateMember(BoothCommitteeMemberDTO member, int index, List<String> errors) {
        String prefix = "Entry at index " + index + ": ";
        
        // Validate name
        if (member.getName() == null || member.getName().trim().isEmpty()) {
            errors.add(prefix + "name is required");
        } else {
            String name = member.getName().trim();
            if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
                errors.add(prefix + "name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters");
            } else if (!NAME_PATTERN.matcher(name).matches()) {
                errors.add(prefix + "name must contain only letters, numbers, and spaces");
            }
        }
        
        // Validate designation
        if (member.getDesignation() == null || member.getDesignation().trim().isEmpty()) {
            errors.add(prefix + "designation is required");
        } else {
            String designation = member.getDesignation().trim();
            if (designation.length() < MIN_DESIGNATION_LENGTH || designation.length() > MAX_DESIGNATION_LENGTH) {
                errors.add(prefix + "designation must be between " + MIN_DESIGNATION_LENGTH + " and " + MAX_DESIGNATION_LENGTH + " characters");
            }
        }
        
        // Validate mobile number (optional)
        if (member.getMobileNumber() != null && !member.getMobileNumber().isEmpty()) {
            String mobile = member.getMobileNumber().trim();
            if (!MOBILE_PATTERN.matcher(mobile).matches()) {
                errors.add(prefix + "mobile number must be exactly 10 digits");
            }
        }
    }
    
    /**
     * Sanitize member data (trim whitespace, handle empty strings)
     */
    public static void sanitize(List<BoothCommitteeMemberDTO> members) {
        if (members == null) {
            return;
        }
        
        for (BoothCommitteeMemberDTO member : members) {
            if (member.getName() != null) {
                member.setName(member.getName().trim());
            }
            if (member.getDesignation() != null) {
                member.setDesignation(member.getDesignation().trim());
            }
            if (member.getMobileNumber() != null) {
                String mobile = member.getMobileNumber().trim();
                member.setMobileNumber(mobile.isEmpty() ? null : mobile);
            }
        }
    }
}
