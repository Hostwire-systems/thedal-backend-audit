package com.thedal.thedal_app.volunteer;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MongoVolunteerRepository extends MongoRepository<MongoVolunteer, String> {
    Page<MongoVolunteer> findByLastNameContainingIgnoreCase(String lastName, Pageable pageable);
    Page<MongoVolunteer> findByStatus(String status, Pageable pageable);
    Page<MongoVolunteer> findByLastNameContainingIgnoreCaseAndStatus(String lastName, String status, Pageable pageable);
    Page<MongoVolunteer> findAll(Pageable pageable);
    
    List<MongoVolunteer> findByAdminUserId(Long adminUserId);
}
