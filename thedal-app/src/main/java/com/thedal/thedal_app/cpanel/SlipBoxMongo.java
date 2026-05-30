package com.thedal.thedal_app.cpanel;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Document(collection = "slip_boxes")
//@CompoundIndexes({
//    @CompoundIndex(name = "accountId_electionId", def = "{'accountId': 1, 'electionId': 1}"),
//    @CompoundIndex(name = "accountId_electionId_slipBoxId", def = "{'accountId': 1, 'electionId': 1, 'slipBoxId': 1}", unique = true),
//    @CompoundIndex(name = "accountId_electionId_slipBoxName", def = "{'accountId': 1, 'electionId': 1, 'slipBoxName': 1}"),
//    @CompoundIndex(name = "accountId_electionId_isDefault", def = "{'accountId': 1, 'electionId': 1, 'isDefault': 1}")
//})
@CompoundIndexes({
    @CompoundIndex(name = "accountId_slipBoxId", def = "{'accountId': 1, 'slipBoxId': 1}", unique = true),
    @CompoundIndex(name = "accountId_slipBoxName", def = "{'accountId': 1, 'slipBoxName': 1}"),
    @CompoundIndex(name = "accountId_isDefault", def = "{'accountId': 1, 'isDefault': 1}")
})
public class SlipBoxMongo {

    @Id
    private Long id;

    @Indexed
    private String mobileNumber;

    @Indexed
    private String slipBoxName;

    @Indexed
    private String slipBoxId;

    private Long accountId;

    private Long electionId;

    private Boolean isDefault;

    // Constructor to map from SlipBoxEntity
    public SlipBoxMongo(SlipBoxEntity entity) {
        this.id = entity.getId();
        this.mobileNumber = entity.getMobileNumber();
        this.slipBoxName = entity.getSlipBoxName();
        this.slipBoxId = entity.getSlipBoxId();
        this.accountId = entity.getAccountId();
        this.electionId = entity.getElectionId();
        this.isDefault = entity.getIsDefault();
    }
}
