package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityReponseDTO {
    private Long id;

    private String imageUrl;
    private String categoryName; 
    private String description;
    private Integer orderIndex;

    public AvailabilityReponseDTO() {}
    
    public AvailabilityReponseDTO(Long id, String description) {
        this.id = id;
        this.description = description;
    }
       
    public AvailabilityReponseDTO(Long id,String description,String imageUrl ) {
        this.id =id;
        this.description = description; 
        this.imageUrl = imageUrl;
    }
    
    public AvailabilityReponseDTO(Long id, String description, String imageUrl, Integer orderIndex) {
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
        this.orderIndex = orderIndex;
    }
    public AvailabilityReponseDTO(Long id, String categoryName, String description, String imageUrl, Integer orderIndex) {
        this.id = id;
        this.categoryName = categoryName;
        this.description = description;
        this.imageUrl = imageUrl;
        this.orderIndex = orderIndex;
    }
   

}
