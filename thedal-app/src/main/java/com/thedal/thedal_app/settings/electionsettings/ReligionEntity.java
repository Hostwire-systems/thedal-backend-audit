package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
//@Table(name= "religion")
@Table(
	    name = "religion",
	    indexes = {
	        @Index(name = "idx_religion_account_id", columnList = "religionName, accountId"),
	        @Index(name = "idx_religion_name", columnList = "religionName")
	    }
	)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ReligionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	//@Column(name = "id", nullable = false, updatable = false)
	private Long id;
	
	@Column(name = "religion_name")
	//@NotBlank(message = "Religion name  is required")
	private String religionName;
	
//	@OneToMany(mappedBy = "religion", cascade = CascadeType.ALL)
//    private List<CasteEntity> castes;
//	@Column(name = "caste_id")
//    private Long casteId;
	
	@JsonIgnore
	private Long accountId;
	@JsonIgnore
	@Column(name = "election_id")
    private Long electionId;
	
	private String religionImage;
	
	private String religionColor;
	
	private Integer orderIndex; 
	@JsonIgnore
	@CreationTimestamp
	@Column(updatable = false)
	private LocalDateTime createdAt;
	@JsonIgnore
	@UpdateTimestamp
	private LocalDateTime updatedAt;

	@OneToMany(mappedBy = "religion", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();
	
}
