package com.thedal.thedal_app.sirreport;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "sir_report_additions", indexes = {
    @Index(name = "idx_sir_addition_job", columnList = "job_id"),
    @Index(name = "idx_sir_addition_epic", columnList = "epic_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SirReportAdditionEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "job_id", nullable = false)
    private UUID jobId;
    
    @Column(name = "epic_number", nullable = false)
    private String epicNumber;
    
    @Column(name = "part_no")
    private Integer partNo;
    
    @Column(name = "voter_name_en")
    private String voterNameEn;
    
    @Column(name = "serial_no")
    private Long serialNo;
    
    @Column(name = "section_no")
    private Integer sectionNo;
    
    @Column(name = "house_no_en")
    private String houseNoEn;
    
    @Column(name = "age")
    private Integer age;
    
    @Column(name = "gender")
    private String gender;
}
