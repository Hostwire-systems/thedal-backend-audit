package com.thedal.thedal_app.volunteer;

import com.thedal.thedal_app.volunteer.dto.LocationDto;

public class ActivityEntityMapper {
    
    public static MongoVolunteerActivity toMongo(ActivityEntity entity) {
        MongoVolunteerActivity mongo = new MongoVolunteerActivity();
        
        // Map ID
        if (entity.getId() != null) {
            mongo.setId(entity.getId().toString());
        }
        
        // Map basic fields
        mongo.setDate(entity.getDate());
        mongo.setBooth(entity.getBooth());
        mongo.setVotersInteracted(entity.getVotersInteracted());
        mongo.setRemarks(entity.getRemarks());
        mongo.setLocation(entity.getLocation());
        mongo.setRoute(entity.getRoute());
        mongo.setAccountId(entity.getAccountId());
        
        // Map volunteer ID from the relationship
        if (entity.getVolunteer() != null && entity.getVolunteer().getId() != null) {
            mongo.setVolunteerId(entity.getVolunteer().getId().toString());
        }
        
        return mongo;
    }
    
    public static ActivityEntity fromMongo(MongoVolunteerActivity mongo) {
        ActivityEntity entity = new ActivityEntity();
        
        // Map ID (convert string back to Long)
        if (mongo.getId() != null) {
            try {
                entity.setId(Long.valueOf(mongo.getId()));
            } catch (NumberFormatException e) {
                // Handle case where MongoDB ID is not a valid Long
                entity.setId(null);
            }
        }
        
        // Map basic fields
        entity.setDate(mongo.getDate());
        entity.setBooth(mongo.getBooth());
        entity.setVotersInteracted(mongo.getVotersInteracted());
        entity.setRemarks(mongo.getRemarks());
        entity.setLocation(mongo.getLocation());
        entity.setRoute(mongo.getRoute());
        entity.setAccountId(mongo.getAccountId());
        
        // Note: Volunteer entity mapping would need to be handled separately
        // since we only have the volunteerId string in MongoDB
        // You would need to fetch the VolunteerEntity by ID if needed
        
        return entity;
    }
    
    public static MongoVolunteerActivity toMongoWithVolunteerId(ActivityEntity entity, String volunteerId) {
        MongoVolunteerActivity mongo = toMongo(entity);
        mongo.setVolunteerId(volunteerId);
        return mongo;
    }
}