package com.thedal.thedal_app.cpanel.dtos;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "voter_history",
    indexes = {
        @Index(name = "idx_voter_history_account_id", columnList = "voterHistoryName, accountId"),
        @Index(name = "idx_voter_history_name", columnList = "voterHistoryName")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class VoterHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_history_name", nullable = false)
    private String voterHistoryName;

    @Column(name = "voter_history_image", nullable = false)
    private String voterHistoryImage;

    @JsonIgnore
    private Long accountId;

    @JsonIgnore
    @Column(name = "election_id")
    private Long electionId;

    @Column(name = "order_index")
    private Integer orderIndex;
    
    @ManyToMany(mappedBy = "voterHistories", fetch = jakarta.persistence.FetchType.LAZY)
    @JsonIgnore
    private Set<VoterEntity> voters = new HashSet<>();
}
