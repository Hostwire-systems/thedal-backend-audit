package com.thedal.thedal_app.election.dtos;

import lombok.Data;

@Data
public class ElectionBoothRequest {
    private Integer boothNumber;
    private String boothVulnerability;
}
