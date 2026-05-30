package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "parties")
@CompoundIndexes({
    @CompoundIndex(name = "accountId_electionId_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "partyName_accountId_electionId_idx", def = "{'partyName': 1, 'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "accountId_electionId_orderIndex_idx", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}")
})
public class PartyMongo {

    @Id
    private Long id;

    @Indexed
    private String partyName;

    private String partyShortName;

    private String partyImage;
    
    private String partyColor;
    
    private String allianceName;

    private Long accountId;

    private Long electionId;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructor to map from Party entity
    public PartyMongo(Party party) {
        this.id = party.getId();
        this.partyName = party.getPartyName();
        this.partyShortName = party.getPartyShortName();
        this.partyImage = party.getPartyImage();
        this.partyColor = party.getPartyColor();
        this.allianceName = party.getAllianceName();
        this.accountId = party.getAccountId();
        this.electionId = party.getElectionId();
        this.orderIndex = party.getOrderIndex();
        this.createdAt = party.getCreatedAt();
        this.updatedAt = party.getUpdatedAt();
    }
}