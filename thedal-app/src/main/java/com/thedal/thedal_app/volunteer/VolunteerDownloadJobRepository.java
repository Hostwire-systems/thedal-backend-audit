package com.thedal.thedal_app.volunteer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VolunteerDownloadJobRepository extends JpaRepository<VolunteerDownloadJob, Long>{

}
