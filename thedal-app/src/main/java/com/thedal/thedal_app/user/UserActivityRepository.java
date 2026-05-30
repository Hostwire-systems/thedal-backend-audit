package com.thedal.thedal_app.user;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;

public interface UserActivityRepository extends MongoRepository<UserActivity, String> {

    Optional<UserActivity> findFirstByUserIdOrderByLastLoginDesc(Long userId);

    Optional<UserActivity> findByUserId(Long userId);

}



