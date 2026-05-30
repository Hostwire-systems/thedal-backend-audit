package com.thedal.thedal_app.sirreport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Lightweight temporary table for fast comparison (only 3 columns).
 * V929 optimized this table to only store job_id, epic_number, and part_no.
 */
@Entity
@Table(name = "sir_temp_base_voters")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(SirTempBaseVoterEntity.CompositeId.class)
public class SirTempBaseVoterEntity {
    
    @Id
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    
    @Id
    @Column(name = "epic_number", nullable = false, length = 50)
    private String epicNumber;
    
    @Column(name = "part_no")
    private Integer partNo;
    
    /**
     * Composite primary key class
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompositeId implements Serializable {
        private UUID jobId;
        private String epicNumber;
    }
}

