package com.thedal.thedal_app.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequestDto {

    @NotBlank(message = "First Name is required")
    private String firstName;
    
    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotBlank(message = "Mobile number is required")
    @Size(min = 10, max = 10, message = "Invalid mobile number (length must be 10)")
    @Pattern(regexp = "^\\d+$", message = "Mobile number can contain only numbers")
    private String mobile;

    @NotNull(message = "Role is required")
    private int roleID;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;
}
