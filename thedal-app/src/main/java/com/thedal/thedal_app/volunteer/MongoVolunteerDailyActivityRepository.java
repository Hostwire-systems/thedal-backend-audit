package com.thedal.thedal_app.volunteer;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MongoVolunteerDailyActivityRepository extends MongoRepository<MongoVolunteerDailyActivity, String> {
    
    List<MongoVolunteerDailyActivity> findByVolunteerId(Long volunteerId);
    
    List<MongoVolunteerDailyActivity> findByVolunteerIdAndAccountId(Long volunteerId, Long accountId);
    
    Optional<MongoVolunteerDailyActivity> findByVolunteerIdAndCheckInTimeBetween(
        Long volunteerId, LocalDateTime start, LocalDateTime end);
    
    List<MongoVolunteerDailyActivity> findByAccountId(Long accountId);
}