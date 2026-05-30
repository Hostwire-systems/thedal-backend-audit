package com.thedal.thedal_app.election;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "templates")
@CompoundIndexes({
    @CompoundIndex(name = "template_account_election_idx", def = "{'accountId': 1, 'electionId': 1}"),
    @CompoundIndex(name = "template_account_election_name_idx", def = "{'accountId': 1, 'electionId': 1, 'templateName': 1}", unique = true),
    @CompoundIndex(name = "template_slip_id_idx", def = "{'slipId': 1}", unique = true),
    @CompoundIndex(name = "template_active_idx", def = "{'accountId': 1, 'electionId': 1, 'isActive': 1}"),
    @CompoundIndex(name = "template_order_idx", def = "{'accountId': 1, 'electionId': 1, 'orderIndex': 1}")
})
public class TemplateMongo {

    @Id
    private Long id;

    @Field("account_id")
    @Indexed
    private Long accountId;

    @Field("election_id")
    @Indexed
    private Long electionId;

    @Field("slip_id")
    @Indexed
    private String slipId;

    @Field("template_id")
    private Long templateId;

    @Field("template_name")
    private String templateName;

    @Field("image_url")
    private String imageUrl;

    @Field("is_active")
    private Boolean isActive;

    @Field("image_status")
    private Boolean imageStatus;

    @Field("order_index")
    private Integer orderIndex;

    @Field("voter_slip_header")
    private String voterSlipHeader;

    @Field("candidate_info_image_footer")
    private String candidateInfoImageFooter;

    // Constructor to create from TemplateEntity
    public TemplateMongo(TemplateEntity templateEntity) {
        this.id = templateEntity.getId();
        this.accountId = templateEntity.getAccountId();
        this.electionId = templateEntity.getElectionId();
        this.slipId = templateEntity.getSlipId();
        this.templateId = templateEntity.getTemplateId();
        this.templateName = templateEntity.getTemplateName();
        this.imageUrl = templateEntity.getImageUrl();
        this.isActive = templateEntity.getIsActive();
        this.imageStatus = templateEntity.getImageStatus();
        this.orderIndex = templateEntity.getOrderIndex();
        this.voterSlipHeader = templateEntity.getVoterSlipHeader();
        this.candidateInfoImageFooter = templateEntity.getCandidateInfoImageFooter();
    }
}
