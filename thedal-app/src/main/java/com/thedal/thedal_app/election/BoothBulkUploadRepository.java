package com.thedal.thedal_app.election;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoothBulkUploadRepository  extends JpaRepository<BoothBulkUploadEntity, Long>{

} 