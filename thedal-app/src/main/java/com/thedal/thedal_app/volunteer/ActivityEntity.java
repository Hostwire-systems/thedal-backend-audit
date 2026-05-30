package com.thedal.thedal_app.volunteer;

import java.time.LocalDate;

import com.thedal.thedal_app.volunteer.dto.LocationDto;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "volunteer_activities")
@Setter
@Getter
public class ActivityEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //private String date;
	private LocalDate date;
    private String booth;
    private int votersInteracted;
    private String remarks;
    private LocationDto location;
    private String route;
    
    private Long accountId;
    @ManyToOne 
    @JoinColumn(name = "volunteer_id", nullable = false)
    private VolunteerEntity volunteer; 

    // Constructors, getters, and setters
    public ActivityEntity() {}

    public ActivityEntity(LocalDate date, String booth,LocationDto location , String route, int votersInteracted, String remarks,
    		VolunteerEntity volunteer, Long accountId) {
        this.date = date;
        this.booth = booth;
        this.location=location;
        this.route = route;
        this.votersInteracted = votersInteracted;
        this.remarks = remarks;
        this.volunteer = volunteer;
        this.accountId = accountId;
    }

	

}
