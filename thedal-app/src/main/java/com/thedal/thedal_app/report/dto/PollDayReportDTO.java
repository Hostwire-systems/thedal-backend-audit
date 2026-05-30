package com.thedal.thedal_app.report.dto;

import java.util.List;

import lombok.Data;

@Data
public class PollDayReportDTO {
    private String electionId;
    private String boothNumber; // Optional
    private String activeTab; // "vote", "performance", "demographics", "timing"
    private Tabs tabs;

    @Data
    public static class Tabs {
        private List<String> vote;
        private List<String> performance;
        private List<String> demographics;
        private List<String> timing;
    }
}
