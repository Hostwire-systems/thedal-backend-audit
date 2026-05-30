package com.thedal.thedal_app.election.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ElectionReorderDTO {
	
	private Long electionId;
    private int newIndex;

}
