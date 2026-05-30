package com.thedal.thedal_app.volunteer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "volunteer_activity_log")
@Getter
@Setter
public class VolunteerActivityLogsEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

//	@Column(name = "user_id", nullable = false)
//	private Long userId;
	
	@Column(name = "volunteer_id", nullable = false)
	private Long volunteerId;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;
    
	@Column(name = "activity_date", nullable = false)
	private LocalDate activityDate;

	@Column(name = "activity_timestamp", nullable = false)
	private LocalDateTime currentTimeStamp;

	@Column(name = "distance_from_previous_location", nullable = false)
	private BigDecimal distanceFromPreviousLocation;
	
	@ManyToOne
	@JoinColumn(name = "volunteer_daily_activitiy_id")
	private VolunteerDailyActivityEntity volunteerDailyActivityEntity;

}
