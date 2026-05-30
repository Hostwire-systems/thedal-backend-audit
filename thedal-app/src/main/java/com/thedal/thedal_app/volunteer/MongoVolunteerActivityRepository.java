package com.thedal.thedal_app.volunteer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MongoVolunteerActivityRepository extends MongoRepository<MongoVolunteerActivity, String> {
    
    List<MongoVolunteerActivity> findByVolunteerId(String volunteerId);
    
    List<MongoVolunteerActivity> findByAccountId(Long accountId);
    
    List<MongoVolunteerActivity> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<MongoVolunteerActivity> findByVolunteerIdAndDateBetween(String volunteerId, LocalDate startDate, LocalDate endDate);
    
    List<MongoVolunteerActivity> findByBooth(String booth);
    
    List<MongoVolunteerActivity> findByVolunteerIdAndAccountId(String volunteerId, Long accountId);
}