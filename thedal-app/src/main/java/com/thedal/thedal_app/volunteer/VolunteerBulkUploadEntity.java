package com.thedal.thedal_app.volunteer;

import java.time.LocalDateTime;

import com.thedal.thedal_app.files.Files;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "volunteer_bulk_upload")
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerBulkUploadEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	private Long id;
	
	@Column(name = "account_id")
    private Long accountId;

//    @Column(nullable = true)
//    private Long electionId;

    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Enumerated(EnumType.STRING)
    private BulkUploadStatus status;
    
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Files file;
    
    private Long totalRecords;
    private Long totalTimeTaken;
}
