package com.thedal.thedal_app.settings.electionsettings;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface ComplainMongoRepository extends MongoRepository<Complain,String> {

Optional<Complain> findByComplaintNameAndAccountId(String complaintName, Long accountId);


}
