package com.thedal.thedal_app.cpanel.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SlipBoxDTO {

    private Long id;

    @NotBlank(message = "Mobile number is mandatory")
    //@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number format")
    private String mobileNumber;

    @NotBlank(message = "Slip box name is mandatory")
    private String slipBoxName;

    @NotBlank(message = "Slip box ID is mandatory")
    private String slipBoxId;
    
    @JsonIgnore
    private LocalDateTime createdTime;

    @JsonIgnore
    private LocalDateTime modifiedTime;
    @JsonIgnore
    private Boolean isDefault = false;
    
    public SlipBoxDTO() {
        this.isDefault = false;
    }
    
    public SlipBoxDTO(Long id, String mobileNumber, String slipBoxName, String slipBoxId,
            LocalDateTime createdTime, LocalDateTime modifiedTime, Boolean isDefault) {
       this.id = id;
       this.mobileNumber = mobileNumber;
       this.slipBoxName = slipBoxName;
       this.slipBoxId = slipBoxId;
       this.createdTime = createdTime;
       this.modifiedTime = modifiedTime;
       this.isDefault = isDefault != null ? isDefault : false;
}
}