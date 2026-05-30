package com.thedal.thedal_app.familycaptain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.voter.Address;

import lombok.Data;

@Data
public class FamilyCaptainDetailsDTO {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("mobile_number")
    private String mobileNumber;

    @JsonProperty("address")
    private Address familyCaptainAddress;

    @JsonProperty("assigned_families")
    private List<UUID> assignedFamilies;

    @JsonProperty("assigned_family_details")
    private List<FamilyDetailsDTO> assignedFamilyDetails;

    @JsonProperty("status")
    private String status;

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("remarks")
    private String remarks;

    @JsonProperty("whats_app_number")
    private String whatsAppNumber;
    
    @JsonProperty("gender")
    private String gender;

    @JsonProperty("created_time")
    private LocalDateTime createdTime;

    @JsonProperty("modified_time")
    private LocalDateTime modifiedTime;

    @JsonProperty("election_id")
    private Long electionId;

    @JsonProperty("account_id")
    private Long accountId;
}
