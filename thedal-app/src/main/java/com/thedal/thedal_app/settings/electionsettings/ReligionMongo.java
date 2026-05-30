package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "religions")
@CompoundIndex(def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}", name = "account_election_order_idx")
@CompoundIndex(def = "{'accountId': 1, 'electionId': 1, 'religionName': 1}", name = "account_election_name_idx")
public class ReligionMongo {

    @Id
    private Long id;

    @Indexed
    private String religionName;

    private String religionImage;

    @Indexed
    private Long accountId;

    @Indexed
    private Long electionId;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructor to map from ReligionEntity
    public ReligionMongo(ReligionEntity religion) {
        this.id = religion.getId();
        this.religionName = religion.getReligionName();
        this.religionImage = religion.getReligionImage();
        this.accountId = religion.getAccountId();
        this.electionId = religion.getElectionId();
        this.orderIndex = religion.getOrderIndex();
        this.createdAt = religion.getCreatedAt();
        this.updatedAt = religion.getUpdatedAt();
    }
}
