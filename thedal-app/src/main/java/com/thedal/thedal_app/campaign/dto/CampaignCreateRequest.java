package com.thedal.thedal_app.campaign.dto;

import java.util.List;
import lombok.Data;

@Data
public class CampaignCreateRequest {
    private String channel; // whatsapp | sms
    private String title;
    private String senderId;
    private String language; // e.g. en
    private String contentHtml;

    @Data
    public static class Button {
        private String type; // url|call|quick_reply
        private String label;
        private String value;
    }

    @Data
    public static class Media {
        private String mediaId;
        private String caption;
    }

    private List<Button> buttons;
    private Media media;
    private List<String> tags;
    private CampaignFilters filters;

    @Data
    public static class Schedule {
        private String when; // now | ISO datetime
    }

    private Schedule schedule;
}
