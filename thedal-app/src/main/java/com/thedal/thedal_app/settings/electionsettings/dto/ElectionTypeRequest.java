package com.thedal.thedal_app.settings.electionsettings.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElectionTypeRequest {
	
	 @NotNull(message = "Election type cannot be null")
	 private String electionType;

}
