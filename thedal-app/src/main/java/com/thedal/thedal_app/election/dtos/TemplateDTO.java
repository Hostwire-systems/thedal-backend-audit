package com.thedal.thedal_app.election.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TemplateDTO {

    private Long templateId;
    private String slipId;
    private String templateName;
    private String imageUrl;
    private Boolean isActive;
    private Boolean imageStatus;   
    private Integer orderIndex;
    
    private String voterSlipHeader;
    private String candidateInfoImageFooter;
}
