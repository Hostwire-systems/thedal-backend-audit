package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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
//@Table(name= "caste")
@Table(
	    name = "caste",
	    indexes = {
	        @Index(name = "idx_caste_account_id", columnList = "casteName, religion_id, accountId")
	    }
	)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CasteEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
	@Column(name = "caste_name")
    private String casteName;

	@ManyToOne
    @JoinColumn(name = "religion_id", nullable = false)
    private ReligionEntity religion;
	
//	@OneToMany(mappedBy = "caste", cascade = CascadeType.ALL)
//    private List<SubCasteEntity> subCastes;
//	@Column(name = "sub_caste_id")
//    private Long subCasteId;
	@JsonIgnore
	private Long accountId;
	
	@Column(name = "election_id")
	private Long electionId;
	
	@Column(name = "order_index")
	private Integer orderIndex;
	@JsonIgnore
	@CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
    @JsonIgnore
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "caste", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();

}
