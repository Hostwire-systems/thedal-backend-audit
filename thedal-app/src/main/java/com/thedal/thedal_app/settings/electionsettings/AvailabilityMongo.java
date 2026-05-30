package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "availability")
@CompoundIndexes({
    @CompoundIndex(name = "account_election_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "account_election_order_idx", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}"),
    @CompoundIndex(name = "category_election_idx", def = "{'categoryName': 1, 'electionId': 1}"),
    @CompoundIndex(name = "description_election_idx", def = "{'description': 1, 'electionId': 1}")
})
public class AvailabilityMongo {
    @Id
    private Long id;

    private String categoryName;
    private String description;
    private Long accountId;
    private Long electionId;
    private String availabilityImage;
    private Integer orderIndex;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AvailabilityMongo() {}

    public AvailabilityMongo(Availability availability) {
        this.id = availability.getId();
        this.categoryName = availability.getCategoryName();
        this.description = availability.getDescription();
        this.accountId = availability.getAccountId();
        this.electionId = availability.getElectionId();
        this.availabilityImage = availability.getAvailabilityImage();
        this.orderIndex = availability.getOrderIndex();
        this.createdAt = availability.getCreatedAt();
        this.updatedAt = availability.getUpdatedAt();
    }
}