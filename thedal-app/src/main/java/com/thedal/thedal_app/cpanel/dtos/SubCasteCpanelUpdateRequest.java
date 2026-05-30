package com.thedal.thedal_app.cpanel.dtos;

import lombok.Data;

@Data
public class SubCasteCpanelUpdateRequest {
	
	private Long subCasteId;
	private String subCasteName;
	private Long casteId;   
    private Long religionId;

}
