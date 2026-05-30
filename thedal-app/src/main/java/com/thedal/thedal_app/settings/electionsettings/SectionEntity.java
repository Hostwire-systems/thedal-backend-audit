package com.thedal.thedal_app.settings.electionsettings;

import com.thedal.thedal_app.election.ElectionEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "section",
indexes = {
    @Index(name = "idx_section_election_account_part_section",
           columnList = "election_id, account_id, part_no, section_no"),
    @Index(name = "idx_section_election_account_part", columnList = "election_id, account_id, part_no")       
   },
   uniqueConstraints = {
    @UniqueConstraint(name = "uk_election_account_part_section", 
                      columnNames = {"election_id", "account_id", "part_no", "section_no"})
})
//    uniqueConstraints = {
//         @UniqueConstraint(name = "uk_election_account_section_no", 
//                           columnNames = {"election_id", "account_id", "section_no"})
//     }) 
public class SectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_no")
    private Integer partNo;  

    @Column(name = "section_no")
    private Integer sectionNo;  

    @Column(name = "section_name_en")
    private String sectionNameEn;

    @Column(name = "section_name_l1")
    private String sectionNameL1;  

    
//    @JsonIgnore
//    @Column(name = "election_id" )
//    private Long electionId;

     @ManyToOne
     @JoinColumn(name = "election_id", nullable = false)
     private ElectionEntity election;

    private Long accountId;

    public void setElection(ElectionEntity election) {
        this.election = election;
    }
}

