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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "language")
public class Language {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String languageName;

     @JsonIgnore
	private Long accountId;

	@JsonIgnore
	@Column(name = "election_id")
    private Long electionId;
	
	@Column(name = "order_index")
	private Integer orderIndex;
    
    @Column(name = "state")
    private String state;

    public Language(){}

    public Language(String languageName){
        this.languageName=languageName;
    }
    @JsonIgnore
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @JsonIgnore
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToMany(mappedBy = "languages", fetch = jakarta.persistence.FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();


}
