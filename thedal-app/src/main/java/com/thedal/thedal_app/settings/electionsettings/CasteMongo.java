package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "castes")
public class CasteMongo {

    @Id
    private Long id;

    @Indexed
    private String casteName;

    private Long religionId;

    private Long accountId;

    private Long electionId;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructor to map from CasteEntity
    public CasteMongo(CasteEntity caste) {
        this.id = caste.getId();
        this.casteName = caste.getCasteName();
        this.religionId = caste.getReligion().getId();
        this.accountId = caste.getAccountId();
        this.electionId = caste.getElectionId();
        this.orderIndex = caste.getOrderIndex();
        this.createdAt = caste.getCreatedAt();
        this.updatedAt = caste.getUpdatedAt();
    }
}