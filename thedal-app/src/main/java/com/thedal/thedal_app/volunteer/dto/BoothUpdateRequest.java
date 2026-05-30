package com.thedal.thedal_app.volunteer.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoothUpdateRequest {
	
	private List<Long> booths;
    private boolean overwrite;

}
