package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CasteUpdateRequest {
	//private Long casteId;
	private String casteName;
	private Long religionId;

}
