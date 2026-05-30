package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PartyReorderRequest {
	
	private Long partyId;
    private Integer newOrderIndex;

}
