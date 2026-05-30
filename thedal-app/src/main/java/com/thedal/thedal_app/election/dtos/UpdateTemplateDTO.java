package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
public class UpdateTemplateDTO {

	private String templateName;
	//private String imageUrl;
	private Boolean isActive;
	private Boolean imageStatus;
	
}
