package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CasteReorderRequest {
	
	private Long casteId;
    private Integer newOrderIndex;

}
