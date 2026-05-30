package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BoothSlipPrintResponse {

	private String voterId;
    private LocalDateTime printedTime;
    private Long templateId;
    
    public BoothSlipPrintResponse(String voterId, LocalDateTime printedTime, Long templateId) {
        this.voterId = voterId;
        this.printedTime = printedTime;
        this.templateId = templateId;
    }
	
}
