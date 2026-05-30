package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Document(collection = "survey_forms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SurveyFormMongo {

    @Id
    private String id;
    
    @Field("survey_form_id")
    private Long surveyFormId; // Reference to PostgreSQL ID
    
    @Field("account_id")
    private Long accountId;
    
    @Field("election_id")
    private Long electionId;
    
    @Field("form_name")
    private String formName;
    
    @Field("form_description")
    private String formDescription;
    
    @Field("is_active")
    private Boolean isActive;
    
    @Field("order_index")
    private Integer orderIndex;
    
    @Field("custom_fields")
    private List<Map<String, Object>> customFields;
    
    @Field("created_time")
    private LocalDateTime createdTime;
    
    @Field("modified_time")
    private LocalDateTime modifiedTime;
    
    // Constructor to create from PostgreSQL entity
    public SurveyFormMongo(SurveyFormEntity entity) {
        this.surveyFormId = entity.getId();
        this.accountId = entity.getAccountId();
        this.electionId = entity.getElectionId();
        this.formName = entity.getFormName();
        this.formDescription = entity.getFormDescription();
        this.isActive = entity.getIsActive();
        this.orderIndex = entity.getOrderIndex();
        this.customFields = entity.getCustomFields();
        this.createdTime = entity.getCreatedTime();
        this.modifiedTime = entity.getModifiedTime();
    }
    
 // New constructor to set _id explicitly
    public SurveyFormMongo(String id, SurveyFormEntity entity) {
        this.id = id;
        this.surveyFormId = entity.getId();
        this.accountId = entity.getAccountId();
        this.electionId = entity.getElectionId();
        this.formName = entity.getFormName();
        this.formDescription = entity.getFormDescription();
        this.isActive = entity.getIsActive();
        this.orderIndex = entity.getOrderIndex();
        this.customFields = entity.getCustomFields();
        this.createdTime = entity.getCreatedTime();
        this.modifiedTime = entity.getModifiedTime();
    }
    
}
