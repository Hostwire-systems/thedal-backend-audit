package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ElectionReorderRequest {
    private Long electionId;
    private int newIndex;
}