package com.thedal.thedal_app.election.dtos;

import lombok.Data;

@Data
public class PartManagerBoothResponseDTO {
    private String partNo; 
    private String boothVulnerability;
    private Integer orderIndex; 
}
