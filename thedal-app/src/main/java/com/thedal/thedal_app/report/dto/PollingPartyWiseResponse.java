package com.thedal.thedal_app.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollingPartyWiseResponse {

		private Long partyId;

		//private String partyName;
		
	    private Integer votersCount;
}


