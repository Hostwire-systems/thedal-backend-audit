package com.thedal.thedal_app.volunteer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "volunteer_activity_logs")
public class MongoVolunteerActivityLog {
    @Id
    private String id;
    private Long volunteerId;
    private Long accountId;
    private Double latitude;
    private Double longitude;
    private LocalDate activityDate;
    private LocalDateTime currentTimeStamp;
    private BigDecimal distanceFromPreviousLocation;
    private String volunteerDailyActivityId; // Reference to MongoVolunteerDailyActivity
    private LocalDateTime createdTime;

    // Constructors
    public MongoVolunteerActivityLog() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVolunteerId() { return volunteerId; }
    public void setVolunteerId(Long volunteerId) { this.volunteerId = volunteerId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }

    public LocalDateTime getCurrentTimeStamp() { return currentTimeStamp; }
    public void setCurrentTimeStamp(LocalDateTime currentTimeStamp) { this.currentTimeStamp = currentTimeStamp; }

    public BigDecimal getDistanceFromPreviousLocation() { return distanceFromPreviousLocation; }
    public void setDistanceFromPreviousLocation(BigDecimal distanceFromPreviousLocation) { 
        this.distanceFromPreviousLocation = distanceFromPreviousLocation; 
    }

    public String getVolunteerDailyActivityId() { return volunteerDailyActivityId; }
    public void setVolunteerDailyActivityId(String volunteerDailyActivityId) { 
        this.volunteerDailyActivityId = volunteerDailyActivityId; 
    }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }
}