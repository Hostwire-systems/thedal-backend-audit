package com.thedal.thedal_app.settings.electionsettings;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "voter_history")
@CompoundIndexes({
    @CompoundIndex(name = "accountId_electionId", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "accountId_electionId_voterHistoryName", def = "{'accountId': 1, 'electionId': 1, 'voterHistoryName': 1}", unique = true),
    @CompoundIndex(name = "accountId_electionId_orderIndex", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}")
})
public class VoterHistoryMongo {

    @Id
    private Long id;

    @Indexed
    private String voterHistoryName;

    private String voterHistoryImage;

    private Long accountId;

    private Long electionId;

    private Integer orderIndex;

    // Constructor to map from VoterHistoryEntity
    public VoterHistoryMongo(VoterHistoryEntity entity) {
        this.id = entity.getId();
        this.voterHistoryName = entity.getVoterHistoryName();
        this.voterHistoryImage = entity.getVoterHistoryImage();
        this.accountId = entity.getAccountId();
        this.electionId = entity.getElectionId();
        this.orderIndex = entity.getOrderIndex();
    }
}
