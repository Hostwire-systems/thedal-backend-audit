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
@Document(collection = "sections")
@CompoundIndexes({
    @CompoundIndex(name = "accountId_electionId_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "accountId_electionId_partNo_sectionNo_idx", def = "{'accountId': 1, 'electionId': 1, 'partNo': 1, 'sectionNo': 1}"),
    @CompoundIndex(name = "accountId_electionId_partNo_idx", def = "{'accountId': 1, 'electionId': 1, 'partNo': 1}")
})
public class SectionMongo {

    @Id
    private Long id;

    @Indexed
    private Integer partNo;

    @Indexed
    private Integer sectionNo;

    private String sectionNameEn;

    private String sectionNameL1;

    private Long electionId;

    private Long accountId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Constructor to map from SectionEntity
    public SectionMongo(SectionEntity section) {
        this.id = section.getId();
        this.partNo = section.getPartNo();
        this.sectionNo = section.getSectionNo();
        this.sectionNameEn = section.getSectionNameEn();
        this.sectionNameL1 = section.getSectionNameL1();
        this.electionId = section.getElection() != null ? section.getElection().getId() : null;
        this.accountId = section.getAccountId();
        // Note: createdAt and updatedAt would need to be added to SectionEntity if needed
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
