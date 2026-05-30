package com.thedal.thedal_app.volunteer.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerActivityResponseDTO {
	
	private Long id;
	
	private List<Long> assignedBooth;  

	private Double latitude;

	private Double longitude;

	private LocalDate activityDate;

	private LocalDateTime currentTimeStamp;

	private BigDecimal distanceFromPreviousLocation;
}
