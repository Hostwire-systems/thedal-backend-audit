package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
//@Table(name= "sub_caste")
@Table(
	    name = "sub_caste",
	    indexes = {
	        @Index(name = "idx_sub_caste_account_id", columnList = "subCasteName, caste_id, religion_id, accountId")
	    }
	)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) 
public class SubCasteEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@Column(name = "sub_caste_name")
    private String subCasteName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "caste_id", nullable = false)
    private CasteEntity caste;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "religion_id", nullable = false)
    private ReligionEntity religion;
//    @Column(name = "religion_id")
//    private Long religionId;

    private Long accountId;
    @Column(name = "election_id")
	private Long electionId;
    @Column(name = "order_index")
	private Integer orderIndex;
    @CreationTimestamp
@Column(name = "created_at", updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(name = "updated_at")
private LocalDateTime updatedAt;

@OneToMany(mappedBy = "subCaste", fetch = FetchType.LAZY)
@JsonIgnore
private Set<VoterEntity> voters = new HashSet<>();



}
