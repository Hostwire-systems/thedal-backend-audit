package com.thedal.thedal_app.settings.electionsettings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AvailabilityResponse {

    private Long availabilityId;
    private String categoryName;
    private String description;
     private String imageUrl;
     private Integer orderIndex; 

    public AvailabilityResponse() {}
    
       
    public AvailabilityResponse(Long availabilityId, String description,String imageUrl) {
        this.availabilityId = availabilityId;
        this.description = description;
        this.imageUrl=imageUrl;
    }
    public AvailabilityResponse(Long availabilityId, String description,String imageUrl, Integer orderIndex) {
        this.availabilityId = availabilityId;
        this.description = description;
        this.imageUrl=imageUrl;
        this.orderIndex=orderIndex;
    }
    public AvailabilityResponse(Long availabilityId, String categoryName, String description, String availabilityImage, Integer orderIndex) {
        this.availabilityId = availabilityId;
        this.categoryName = categoryName;
        this.description = description;
        this.imageUrl = availabilityImage;
        this.orderIndex = orderIndex;
    }
}
