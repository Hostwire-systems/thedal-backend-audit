package com.thedal.thedal_app.volunteer;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "volunteer_daily_activitity")
@Setter
@Getter
public class VolunteerDailyActivityEntity {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

	@Column(name = "check_in_time", nullable = false)
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time", nullable = true)
    private LocalDateTime checkOutTime;
    
    @Column(name = "hours_worked", nullable = true)
    private Duration hoursWorked;
    
    @Column(name = "account_id", nullable = false)
    private Long accountId;
    
//	@Column(name = "user_id", nullable = false)
//	private Long userId;
	
	@Column(name = "volunteer_id", nullable = false)
	private Long volunteerId;
	
	@Column(nullable = false)
	private boolean isChecked;

}
