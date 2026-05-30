package com.thedal.thedal_app.voter.activity.dto;

import com.thedal.thedal_app.voter.activity.ActivityType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordActivityRequest {
    
    @NotBlank(message = "Voter ID is required")
    private String voterId;
    
    @NotNull(message = "Activity type is required")
    private ActivityType activityType;
    
    private Long volunteerId;
    
    private Long templateId;
    
    private String metadata;
}
