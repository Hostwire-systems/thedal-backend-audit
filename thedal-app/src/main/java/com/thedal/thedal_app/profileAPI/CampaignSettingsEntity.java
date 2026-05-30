package com.thedal.thedal_app.profileAPI;



import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thedal.thedal_app.profileAPI.dtos.MessagingService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "campaignSettings")
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CampaignSettingsEntity {
	
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
    private Long id;

    @Enumerated(EnumType.STRING)
    private MessagingService smsMessagingService;

    private String smsLicenseKey;

    @JsonIgnore
    private String smsVerificationStatus; 
    
    @JsonIgnore
    @Column(name = "account_id")
    private Long accountId;

	

}
