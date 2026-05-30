package com.thedal.thedal_app.familycaptain.dto;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.thedal.thedal_app.voter.Address;

import lombok.Data;

@Data
public class FamilyCaptainDetailsUpdate {

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
}
