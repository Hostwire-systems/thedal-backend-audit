package com.thedal.thedal_app.report.pollday.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyWisePollingResponse {
    private List<FamilyWisePollingData> parts;
    private FamilyWisePollingSummary summary;
}
