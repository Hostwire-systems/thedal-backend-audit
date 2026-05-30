package com.thedal.thedal_app.election;

import lombok.Data;

@Data
public class UpdateTemplateDetailsResponse {
    private String templateName;
    private String voterSlipHeader;
    private String candidateInfoImageFooter;
}
