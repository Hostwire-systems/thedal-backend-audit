package com.thedal.thedal_app.settings.electionsettings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComplaintRepository extends JpaRepository<ComplaintEntity, Long> {

	Optional<ComplaintEntity> findByComplaintNameAndAccountId(String complaintName, Long accountId);

}
