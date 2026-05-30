package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for Booth Committee Member
 * Each part can have up to 15 committee members
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoothCommitteeMemberDTO {
    
    /**
     * Name of committee member
     * Required, 1-100 characters, alphanumeric with spaces
     */
    @NotBlank(message = "Committee member name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s]+$", message = "Name must contain only letters, numbers, and spaces")
    private String name;
    
    /**
     * Designation of committee member (e.g., President, Secretary)
     * Required, 1-50 characters
     */
    @NotBlank(message = "Committee member designation is required")
    @Size(min = 1, max = 50, message = "Designation must be between 1 and 50 characters")
    private String designation;
    
    /**
     * Mobile number of committee member
     * Optional, must be exactly 10 digits if provided
     */
    @Pattern(regexp = "^[0-9]{10}$|^$", message = "Mobile number must be exactly 10 digits")
    private String mobileNumber;
    
    /**
     * Trim whitespace from name
     */
    public void setName(String name) {
        this.name = name != null ? name.trim() : null;
    }
    
    /**
     * Trim whitespace from designation
     */
    public void setDesignation(String designation) {
        this.designation = designation != null ? designation.trim() : null;
    }
    
    /**
     * Trim whitespace from mobile number and treat empty string as null
     */
    public void setMobileNumber(String mobileNumber) {
        if (mobileNumber != null) {
            String trimmed = mobileNumber.trim();
            this.mobileNumber = trimmed.isEmpty() ? null : trimmed;
        } else {
            this.mobileNumber = null;
        }
    }
}
