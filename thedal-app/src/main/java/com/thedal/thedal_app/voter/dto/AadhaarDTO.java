package com.thedal.thedal_app.voter.dto;

import java.util.Map;

import lombok.Data;

@Data
public class AadhaarDTO {
	private Long electionId;
	//private String aadhaarNumber;
    private Map<String, Object> aadhaarData;
}