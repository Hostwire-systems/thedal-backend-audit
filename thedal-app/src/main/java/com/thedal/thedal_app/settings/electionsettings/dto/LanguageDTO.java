package com.thedal.thedal_app.settings.electionsettings.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LanguageDTO {
	//@JsonIgnore
	private Long id;
    private String languageName;
    
    @JsonIgnore
   	private Long accountId;
	   private String state;
}
