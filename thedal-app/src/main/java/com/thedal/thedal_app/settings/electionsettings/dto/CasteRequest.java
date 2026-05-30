package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CasteRequest {
	
	private String casteName;
    private Long religionId;
    @JsonIgnore
    private Long accountId;

}
