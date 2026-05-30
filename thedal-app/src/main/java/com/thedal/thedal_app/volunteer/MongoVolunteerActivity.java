package com.thedal.thedal_app.volunteer;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDate;
import com.thedal.thedal_app.volunteer.dto.LocationDto;

@Document(collection = "volunteer_activities")
public class MongoVolunteerActivity {
    @Id
    private String id;
    private LocalDate date;
    private String booth;
    private int votersInteracted;
    private String remarks;
    private LocationDto location;
    private String route;
    private Long accountId;
    private String volunteerId;

    // Constructors
    public MongoVolunteerActivity() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getBooth() { return booth; }
    public void setBooth(String booth) { this.booth = booth; }

    public int getVotersInteracted() { return votersInteracted; }
    public void setVotersInteracted(int votersInteracted) { this.votersInteracted = votersInteracted; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public LocationDto getLocation() { return location; }
    public void setLocation(LocationDto location) { this.location = location; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getVolunteerId() { return volunteerId; }
    public void setVolunteerId(String volunteerId) { this.volunteerId = volunteerId; }
}