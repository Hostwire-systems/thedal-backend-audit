package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "caste_categories")
@CompoundIndexes({
    @CompoundIndex(name = "idx_account_election", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "idx_account_election_name", def = "{'accountId': 1, 'electionId': 1, 'casteCategoryName': 1}"),
    @CompoundIndex(name = "idx_account_election_order", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CasteCategoryMongo {
    
    @Id
    private Long id;
    
    private String casteCategoryName;
    private Long accountId;
    private Long electionId;
    private Integer orderIndex;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructor from PostgreSQL entity
    public CasteCategoryMongo(CasteCategoryEntity casteCategoryEntity) {
        this.id = casteCategoryEntity.getId();
        this.casteCategoryName = casteCategoryEntity.getCasteCategoryName();
        this.accountId = casteCategoryEntity.getAccountId();
        this.electionId = casteCategoryEntity.getElectionId();
        this.orderIndex = casteCategoryEntity.getOrderIndex();
        this.createdAt = casteCategoryEntity.getCreatedAt();
        this.updatedAt = casteCategoryEntity.getUpdatedAt();
    }
}
