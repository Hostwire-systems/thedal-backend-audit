package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurveyFormReorderRequest {
    private Long formId;
    private Integer newOrderIndex;
}