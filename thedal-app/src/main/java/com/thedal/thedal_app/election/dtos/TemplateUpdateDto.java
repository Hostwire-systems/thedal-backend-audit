package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateUpdateDto {
	
	private Long templateId;
    private String templateName;
    private String imageUrl;
    private Boolean isActive;
    private Boolean imageStatus;

}
