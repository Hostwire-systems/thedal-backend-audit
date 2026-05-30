package com.thedal.thedal_app.volunteer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MongoVolunteerActivityLogRepository extends MongoRepository<MongoVolunteerActivityLog, String> {
    
    List<MongoVolunteerActivityLog> findByVolunteerId(Long volunteerId);
    
    List<MongoVolunteerActivityLog> findByVolunteerIdAndAccountId(Long volunteerId, Long accountId);
    
    List<MongoVolunteerActivityLog> findByVolunteerIdAndActivityDateBetween(
        Long volunteerId, LocalDate startDate, LocalDate endDate);
    
    List<MongoVolunteerActivityLog> findByAccountId(Long accountId);
    
    List<MongoVolunteerActivityLog> findByVolunteerDailyActivityId(String volunteerDailyActivityId);
}