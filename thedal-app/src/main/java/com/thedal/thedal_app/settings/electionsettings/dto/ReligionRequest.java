package com.thedal.thedal_app.settings.electionsettings.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReligionRequest {
	
	private String religionName;
    private MultipartFile religionImage;
    private String religionColor;
		
	


}
