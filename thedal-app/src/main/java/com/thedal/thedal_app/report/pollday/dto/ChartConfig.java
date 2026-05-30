package com.thedal.thedal_app.report.pollday.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChartConfig {
    @JsonProperty("chartId")
    private String chartId;
    
    @JsonProperty("id")
    private String id;
    
    private List<Integer> selectedParts;
    
    private String customTitle;
    
    private String chartColor;
    
    // New fields for enhanced chart configuration
    private String chartType; // Type of chart - e.g., "age-group", "gender", "caste", "ward", etc.
    
    private String viewType; // "bar", "line", "table", or "stacked"
    
    private String sortType; // "asc" or "desc"
    
    private Integer order; // For drag-and-drop ordering
    
    private Integer width; // Chart width in pixels (default: 600)
    
    private Integer height; // Chart height in pixels (default: 450)
    
    private Integer x; // X position for free-form positioning (default: 0)
    
    private Integer y; // Y position for free-form positioning (default: 0)
    
    // Advanced Filters
    private ChartFilters filters;
    
    // Helper method to get the actual chart ID from either field
    public String getActualChartId() {
        return chartId != null ? chartId : id;
    }
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChartFilters {
        private List<String> parties;           // Multi-select party names
        private List<String> religions;         // Multi-select religion names
        private List<String> casteCategories;   // Multi-select caste category names
        private List<String> castes;            // Multi-select caste names
        private List<String> subCastes;         // Multi-select sub-caste names
        private List<String> languages;         // Multi-select language names
        private List<String> schemes;           // Multi-select scheme names
        private List<String> genders;           // Multi-select: "Male", "Female", "Other"
        private Integer minAge;                 // Age range min (default: 18)
        private Integer maxAge;                 // Age range max (default: 120)
        private Boolean includeUnknownAge;      // Include voters with unknown age
    }
}
