package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TemplateReorderRequest {
	
	//private Long templateId;
	private String templateName;
    private Integer newOrderIndex;

}
