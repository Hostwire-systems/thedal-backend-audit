package com.thedal.thedal_app.election;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TemplateDTOResponse {
  
    private Long templateId;
    private String slipId;
    private String templateName;
    @JsonIgnore
    private Long electionId;
    @JsonIgnore
    private Long accountId;
    private String imageUrl;
    private Boolean isActive;
    private Boolean imageStatus;   
    private Integer orderIndex;

    private String voterSlipHeader;
    private String candidateInfoImageFooter;


}
