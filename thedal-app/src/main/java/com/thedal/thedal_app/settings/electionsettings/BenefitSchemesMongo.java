package com.thedal.thedal_app.settings.electionsettings;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Document(collection = "benefit_schemes")
@CompoundIndex(name = "idx_account_election", def = "{'accountId': 1, 'electionId': 1}")
@CompoundIndex(name = "idx_id_account_election", def = "{'_id': 1, 'accountId': 1, 'electionId': 1}")
@CompoundIndex(name = "idx_scheme_name_election", def = "{'schemeName': 1, 'electionId': 1}")
public class BenefitSchemesMongo {

    @Id
    private Long id;

    @Field("scheme_name")
    private String schemeName;
    
    @Field("image_url")
    private String imageUrl;
    
    @Field("scheme_by")
    private String schemeBy; // Store as String to match SchemeBy.getValue()
    
    @Field("account_id")
    private Long accountId;
    
    @Field("election_id")
    private Long electionId;
    
    @Field("order_index")
    private Integer orderIndex;
    
    private Double schemeValue;
    
    @Field("created_at")
    private LocalDateTime createdAt;
    
    @Field("updated_at")
    private LocalDateTime updatedAt;
    
    @Field("user_selection")
    private Boolean userSelection;

    public BenefitSchemesMongo() {}

    public BenefitSchemesMongo(BenefitSchemes benefitSchemes) {
        this.id = benefitSchemes.getId();
        this.schemeName = benefitSchemes.getSchemeName();
        this.imageUrl = benefitSchemes.getImageUrl();
        this.schemeBy = benefitSchemes.getSchemeBy().getValue();
        this.userSelection = benefitSchemes.getUserSelection();
        this.accountId = benefitSchemes.getAccountId();
        this.electionId = benefitSchemes.getElectionId();
        this.orderIndex = benefitSchemes.getOrderIndex();
        this.schemeValue = benefitSchemes.getSchemeValue();
        this.createdAt = benefitSchemes.getCreatedAt();
        this.updatedAt = benefitSchemes.getUpdatedAt();
    }
}
