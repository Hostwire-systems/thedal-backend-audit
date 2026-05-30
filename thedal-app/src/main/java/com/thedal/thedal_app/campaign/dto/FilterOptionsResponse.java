package com.thedal.thedal_app.campaign.dto;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FilterOptionsResponse {
    private List<Integer> parts;
    private Map<Integer, List<SimpleItem>> sectionsByPart;
    private List<SimpleItem> districts;
    private Map<String, List<SimpleItem>> constituenciesByDistrict;
    private List<SimpleItem> castes;
    private List<SimpleItem> subCastes;
    private List<SimpleItem> casteCategories;
    private List<SimpleItem> religions;
    private List<SimpleItem> parties;
    private List<SimpleItem> availabilities;
    private List<SimpleItem> ageRanges; // code + label
    private List<String> genders;
    private List<String> tags;

    @Data
    @Builder
    public static class SimpleItem {
        private String id;
        private String name;
    }
}
