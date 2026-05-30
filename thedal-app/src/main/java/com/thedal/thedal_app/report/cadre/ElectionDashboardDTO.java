package com.thedal.thedal_app.report.cadre;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ElectionDashboardDTO {
   // private String userId;
    private String electionId;
    private String boothNumber;
    private String activeTab;
    private Tabs tabs;

    @Data
    public static class Tabs {
        private List<String> demographics;
        private List<String> issues;
        private List<String> religion;
        private List<String> caste;
        private List<String> subcaste;
        private List<String> languages;
        private List<String> party;
        private List<String> scheme;
        private List<String> history;
        private List<String> availability;
    }
}