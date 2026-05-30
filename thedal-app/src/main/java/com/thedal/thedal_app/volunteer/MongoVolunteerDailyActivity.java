package com.thedal.thedal_app.volunteer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Duration;
import java.time.LocalDateTime;

@Document(collection = "volunteer_daily_activities")
public class MongoVolunteerDailyActivity {
    @Id
    private String id;
    private Long volunteerId;
    private Long accountId;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Duration hoursWorked;
    private boolean isChecked;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;

    // Constructors
    public MongoVolunteerDailyActivity() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getVolunteerId() { return volunteerId; }
    public void setVolunteerId(Long volunteerId) { this.volunteerId = volunteerId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public Duration getHoursWorked() { return hoursWorked; }
    public void setHoursWorked(Duration hoursWorked) { this.hoursWorked = hoursWorked; }

    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getModifiedTime() { return modifiedTime; }
    public void setModifiedTime(LocalDateTime modifiedTime) { this.modifiedTime = modifiedTime; }
}