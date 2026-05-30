package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "sub_castes")
public class SubCasteMongo {

    @Id
    private Long id;

    @Indexed
    private String subCasteName;

    private Long casteId;

    private Long religionId;

    private Long accountId;

    private Long electionId;

    private Integer orderIndex;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

//    // Constructor to map from SubCasteEntity
//    public SubCasteMongo(SubCasteEntity subCaste) {
//        this.id = subCaste.getId();
//        this.subCasteName = subCaste.getSubCasteName();
//        this.casteId = subCaste.getCaste().getId();
//        this.religionId = subCaste.getReligion().getId();
//        this.accountId = subCaste.getAccountId();
//        this.electionId = subCaste.getElectionId();
//        this.orderIndex = subCaste.getOrderIndex();
//        this.createdAt = subCaste.getCreatedAt();
//        this.updatedAt = subCaste.getUpdatedAt();
//    }
    public SubCasteMongo(SubCasteEntity subCaste) {
        this.id = subCaste.getId();
        this.subCasteName = subCaste.getSubCasteName();
        this.casteId = subCaste.getCaste() != null ? subCaste.getCaste().getId() : null;
        this.religionId = subCaste.getReligion() != null ? subCaste.getReligion().getId() : null;
        this.accountId = subCaste.getAccountId();
        this.electionId = subCaste.getElectionId();
        this.orderIndex = subCaste.getOrderIndex();
        this.createdAt = subCaste.getCreatedAt();
        this.updatedAt = subCaste.getUpdatedAt();
    }
}
