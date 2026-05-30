package com.thedal.thedal_app.auth.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OAuthRequestDTO {

    
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 10, message = "Invalid mobile number (length must be 10)")
    @Pattern(regexp = "^\\d+$", message = "Mobile number can contain only numbers")
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
