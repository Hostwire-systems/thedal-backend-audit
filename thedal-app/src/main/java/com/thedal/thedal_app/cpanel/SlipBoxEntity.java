package com.thedal.thedal_app.cpanel;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "_slip_boxes",
    indexes = {
       // @Index(name = "idx_account_election_slipbox", columnList = "account_id, election_id, slip_box_id")
    	@Index(name = "idx_account_slipbox", columnList = "account_id, slip_box_id")
    },
    uniqueConstraints = {
        //@UniqueConstraint(name = "unique_slip_box_id_election", columnNames = {"slip_box_id", "election_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlipBoxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Mobile number is mandatory")
    //@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number format")
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @NotBlank(message = "Slip box name is mandatory")
    @Column(name = "slip_box_name", nullable = false)
    private String slipBoxName;

    @NotBlank(message = "Slip box ID is mandatory")
    @Column(name = "slip_box_id", nullable = false)
    private String slipBoxId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

//    @Column(name = "election_id", nullable = false)
    @Column(name = "election_id")
    private Long electionId;
    
    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    @Column(name = "modified_time")
    private LocalDateTime modifiedTime;

    @PrePersist
    protected void onCreate() {
        if (createdTime == null) {
            createdTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedTime = LocalDateTime.now();
    }
    
    public boolean isDefault() {
        return isDefault != null ? isDefault : false; 
    }
    
}