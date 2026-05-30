package com.thedal.thedal_app.campaign.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CampaignResponse {
    private String id;
    private String channel;
    private String title;
    private String senderId;
    private String language;
    private String status; // DRAFT|SCHEDULED|SENDING|SENT|FAILED
    private OffsetDateTime createdAt;
    private OffsetDateTime scheduledAt;
    private Long recipientsCount;

    private String contentHtml;
    private List<CampaignCreateRequest.Button> buttons;
    private CampaignCreateRequest.Media media;
    private List<String> tags;
    private CampaignFilters filters;
}
