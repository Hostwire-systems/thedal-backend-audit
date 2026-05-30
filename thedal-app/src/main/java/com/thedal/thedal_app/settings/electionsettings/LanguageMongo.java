package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.voter.VoterEntity;

import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "language")
@CompoundIndexes({
    @CompoundIndex(name = "accountId_electionId_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "languageName_electionId_idx", def = "{'languageName': 1, 'electionId': 1}"),
    @CompoundIndex(name = "accountId_electionId_orderIndex_idx", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}")
})
public class LanguageMongo {
    @Id
    private Long id;

    @Indexed
    private String languageName;
    private Long accountId;
    private Long electionId;
    private Integer orderIndex;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    //private Set<Long> voterIds = new HashSet<>();

    public LanguageMongo() {}

    public LanguageMongo(Language language) {
        this.id = language.getId();
        this.languageName = language.getLanguageName();
        this.accountId = language.getAccountId();
        this.electionId = language.getElectionId();
        this.orderIndex = language.getOrderIndex();
        this.state = language.getState();
        this.createdAt = language.getCreatedAt();
        this.updatedAt = language.getUpdatedAt();
        
    }
}