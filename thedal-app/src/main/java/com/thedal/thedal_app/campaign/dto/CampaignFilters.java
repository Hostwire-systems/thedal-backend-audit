package com.thedal.thedal_app.campaign.dto;

import java.util.List;
import lombok.Data;

@Data
public class CampaignFilters {
    private Long electionId;
    private Long accountId;
    private List<Integer> partNos;
    private List<String> sectionIds;
    private List<String> districtIds;
    private List<String> constituencyIds;
    private List<Long> casteIds;
    private List<Long> subCasteIds;
    private List<Long> casteCategoryIds;
    private List<Long> religionIds;
    private List<Long> partyIds;
    private List<Long> availabilityIds;
    private String ageRange; // e.g. "18-25"
    private String gender;   // male|female|other
    private List<String> tags;
    private List<String> pollStatus; // "voted", "notVoted" - can be empty (all), single value, or both
    private Boolean aadhaarVerified;   // nullable
    private Boolean membershipVerified; // nullable
}
