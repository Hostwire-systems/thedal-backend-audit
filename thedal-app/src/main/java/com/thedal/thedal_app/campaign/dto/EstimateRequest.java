package com.thedal.thedal_app.campaign.dto;

import lombok.Data;

@Data
public class EstimateRequest {
    private String channel; // whatsapp | sms
    private CampaignFilters filters;
}
