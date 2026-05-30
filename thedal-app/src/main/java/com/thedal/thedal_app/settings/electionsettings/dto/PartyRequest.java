package com.thedal.thedal_app.settings.electionsettings.dto;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartyRequest {
	private Long partyId;  
	@NotNull(message = "Party name is required")
	private String partyName;
	
	private String partyShortName;
//	@NotEmpty(message = "Party image must not be empty")
//    private String partyImage;
	
	private MultipartFile partyImage;
	
	private String partyColor;
	
	private String allianceName;
	
    @JsonIgnore
	private Long electionId;
}
