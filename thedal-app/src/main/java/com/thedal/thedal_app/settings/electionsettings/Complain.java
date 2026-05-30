package com.thedal.thedal_app.settings.electionsettings;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "complaint")
public class Complain {
      
    @Id
    private String id;

    @Column(name = "complaint_name")
    private String complaintName;

    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;

}

