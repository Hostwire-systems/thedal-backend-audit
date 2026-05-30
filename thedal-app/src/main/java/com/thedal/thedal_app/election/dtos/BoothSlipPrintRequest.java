package com.thedal.thedal_app.election.dtos;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BoothSlipPrintRequest {
	
	 private String voterId;
	 private Long templateId;

}
