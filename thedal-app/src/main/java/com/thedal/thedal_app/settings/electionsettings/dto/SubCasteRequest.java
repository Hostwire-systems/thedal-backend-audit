package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubCasteRequest {
	
	 private String subCasteName;
	    private Long casteId;
	    private Long religionId;
	    @JsonIgnore
	    private Long accountId;

}
