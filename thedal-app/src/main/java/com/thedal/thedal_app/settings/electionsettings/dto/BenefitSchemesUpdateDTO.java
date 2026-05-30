package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BenefitSchemesUpdateDTO {
    private String schemeName;
    private String imageUrl;
    private String schemeBy;
    private Double schemeValue;
    private Boolean userSelection; 

}
