package com.thedal.thedal_app.profileAPI.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class BasicProfileRequest {

	@JsonIgnore
	private Long id;

	@NotBlank(message = "Full name cannot be empty")
    //@NotNull(message = "Full name is required")
    private String fullName;

	@NotBlank(message = "Email cannot be empty")
    @Email(message = "Email should be valid")
    //@NotNull(message = "Email is required")
    private String email;

	@NotBlank(message = "Mobile number cannot be empty")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    //@NotNull(message = "Mobile number is required")
    private String mobileNumber;

	@Schema(description = "Alternate email address (optional)", example = "john.alternate@example.com")
    @Email(message = "Alternate email should be valid")
    private String alternateEmailId;

	@Schema(description = "Alternate mobile number with optional country code (optional)", example = "+0987654321")
    @Pattern(regexp = "\\+?[0-9]{10,15}", message = "Alternate mobile number should be valid and include country code if required")
    private String alternateMobileNumber;

    @NotBlank(message = "Organization name is required")
    private String organizationName;

	
}
