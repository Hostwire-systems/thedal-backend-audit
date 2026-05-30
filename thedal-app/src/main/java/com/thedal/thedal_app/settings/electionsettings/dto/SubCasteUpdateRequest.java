package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubCasteUpdateRequest {
	//private Long subCasteId;
	private String subCasteName;
	private Long casteId;
    private Long religionId;

}
