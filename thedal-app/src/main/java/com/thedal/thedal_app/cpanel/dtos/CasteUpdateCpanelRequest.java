package com.thedal.thedal_app.cpanel.dtos;

import lombok.Data;

@Data
public class CasteUpdateCpanelRequest {
	
	    private Long casteId;
		private String casteName;

		private Long religionId;

}
