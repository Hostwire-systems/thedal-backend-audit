package com.thedal.thedal_app.settings.electionsettings.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityDTO {
    
    private String categoryName;
    private String description;

    public AvailabilityDTO() {}
    
   
    public AvailabilityDTO(String description ) {
        this.description = description; 
    }
   

}
