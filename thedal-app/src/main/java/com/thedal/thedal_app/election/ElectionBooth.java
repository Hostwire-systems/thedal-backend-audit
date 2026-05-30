package com.thedal.thedal_app.election;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
@Setter
@Table(name = "election_booth",
indexes = {
    @Index(name = "idx_election_booth_election_account_booth",
           columnList = "election_id, account_id, booth_number")
})
//@Table(name="ElectionBooth", indexes = {
//	    @Index(name = "election_booth_idx", columnList = "election_id, boothNumber")
//	})
//@Table(name="ElectionBooth")
public class ElectionBooth {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private ElectionEntity election;

//    @Column(nullable = false,unique = true)
    @Column(nullable = false)
    private Integer boothNumber;
    
    //@Column(nullable = false)
    private Long accountId;

    @Column(nullable = true) 
    private String boothVulnerability;
    
    private Integer orderIndex;

}
