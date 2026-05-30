package com.thedal.thedal_app.familycaptain.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.voter.Address;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveFamilyCaptainDetailsDTO {

    @NotBlank(message = "First name is required")
    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @Email(message = "Invalid email format")
    @JsonProperty("email")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit number starting with 6-9")
    @JsonProperty("mobile_number")
    private String mobileNumber;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @JsonProperty("password")
    private String password;

    @JsonProperty("address")
    private Address familyCaptainAddress;

    @JsonProperty("assigned_families")
    private List<UUID> assignedFamilies;

    @JsonProperty("status")
    private String status = "active";

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("whats_app_number")
    private String whatsAppNumber;
    
    @JsonProperty("gender")
    private String gender;
}
