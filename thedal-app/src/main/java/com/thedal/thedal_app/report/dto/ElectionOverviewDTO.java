package com.thedal.thedal_app.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class ElectionOverviewDTO {
    private String pincode;
	private String mobileNumber;
	private String gender;
	private boolean isNewVoter;

}
