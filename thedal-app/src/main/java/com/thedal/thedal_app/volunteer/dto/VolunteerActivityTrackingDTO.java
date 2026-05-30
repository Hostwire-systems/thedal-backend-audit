package com.thedal.thedal_app.volunteer.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerActivityTrackingDTO {



	@DecimalMin(value = "-90.0", message = "40611")
	@DecimalMax(value = "90.0", message = "40612")
	private Double latitude;

	@DecimalMin(value = "-180.0", message = "40613")
	@DecimalMax(value = "180.0", message = "40614")
	private double longitude;

}
