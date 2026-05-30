package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothReorderRequest {
	
	private Integer boothNumber;
    private Integer newOrderIndex;

}
