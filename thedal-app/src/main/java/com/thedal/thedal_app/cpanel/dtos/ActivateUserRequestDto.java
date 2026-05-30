package com.thedal.thedal_app.cpanel.dtos;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ActivateUserRequestDto {
	
	@JsonProperty("expiryDate")  // Accept 'expiryDate' from frontend
	private LocalDateTime expiryAt;  // Map to 'expiryAt' internally

}
