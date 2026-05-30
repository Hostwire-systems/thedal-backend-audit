package com.thedal.thedal_app.profileAPI;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.thedal.thedal_app.profileAPI.dtos.MessagingService;

public interface CampaignSettingsRepo extends JpaRepository<CampaignSettingsEntity, Long>{

	Optional<CampaignSettingsEntity> findByAccountId(Long accountId);
	//Optional<CampaignSettingsEntity> findByAccountIdAndMessagingService(Long accountId, String messagingService);
	Optional<CampaignSettingsEntity> findByAccountIdAndSmsMessagingService(Long accountId,
			MessagingService smsMessagingService);
}
