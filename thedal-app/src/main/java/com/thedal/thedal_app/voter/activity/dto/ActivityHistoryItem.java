package com.thedal.thedal_app.voter.activity.dto;

import java.time.LocalDateTime;

import com.thedal.thedal_app.voter.activity.ActivityType;

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
public class ActivityHistoryItem {
    
    private Long id;
    private String voterId;
    private ActivityType activityType;
    private LocalDateTime activityTime;
    private Long volunteerId;
    private Long templateId;
    private String metadata;
}
