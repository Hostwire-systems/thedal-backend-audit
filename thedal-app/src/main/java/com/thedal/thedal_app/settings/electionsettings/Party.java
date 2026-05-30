package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "parties",
       indexes = {
        @Index(name = "idx_party_name", columnList = "partyName")
       }
		)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Party {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Column(nullable = false)
    private String partyName;
    
    //@Column(nullable = false, unique = true)
    private String partyShortName;

    @Column
    private String partyImage;
    
    private String partyColor;
    
    private String allianceName;
    
    @JsonIgnore
    private Long accountId;
    @JsonIgnore
    private Long electionId; 
    
    @Column(name = "order_index")
	private Integer orderIndex;
    
    @CreatedDate
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "party", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();


}
