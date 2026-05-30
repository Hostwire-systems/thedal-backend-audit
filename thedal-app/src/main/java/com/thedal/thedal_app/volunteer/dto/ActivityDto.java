package com.thedal.thedal_app.volunteer.dto;

import java.time.LocalDate;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
//@AllArgsConstructor
public class ActivityDto {
	
	//private String date;
	private LocalDate date;
    private String booth;
    private int votersInteracted;
    private String remarks;    
    private String route;
    
    private LocationDto location;
    
    public ActivityDto() {}

    public ActivityDto(LocalDate date, String booth, LocationDto location,
                       String route, int votersInteracted, String remarks) {
        this.date = date;
        this.booth = booth;
        this.location=location;
        this.route = route;
        this.votersInteracted = votersInteracted;
        this.remarks = remarks;
    }
    
    
 
}
