package com.thedal.thedal_app.report.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class CadreReportDTO {
	@JsonIgnore
    private String electionId;
    private String activeTab;
    private Tabs tabs;
    private Integer selectedBooth; // Optional

    @Data
    public static class Tabs {
        private List<String> topPerformers;
        private List<String> lowPerformers;
        private List<String> activity;
        private List<String> demographics;
        private List<String> updates;
    }
}