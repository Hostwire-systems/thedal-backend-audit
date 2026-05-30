package com.thedal.thedal_app.settings.electionsettings.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class ReligionUpdateRequest {

	private String religionName;
	private MultipartFile religionImage;
	private String religionColor;
}
