package com.thedal.thedal_app.election.dtos;

import lombok.Data;

@Data
public class ElectionIdImageDTO {
    private Long electionId;
    private String imageUrl;
    private Integer orderIndex;
}
