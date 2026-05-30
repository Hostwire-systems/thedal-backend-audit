package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
public class BenefitSchemesDTO {

   
        private Long id;
        private String schemeName;
        private String imageUrl;
        private String schemeBy;
        private Integer orderIndex;
        private Double schemeValue;
        private Boolean userSelection;
       
        public BenefitSchemesDTO() {}
        
        
       
       
    }
    