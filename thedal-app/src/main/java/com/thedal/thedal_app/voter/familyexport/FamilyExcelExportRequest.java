package com.thedal.thedal_app.voter.familyexport;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FamilyExcelExportRequest {
    private Long electionId;
    private Long accountId;
    private String exportType; // "family" or "part"
    private String familyId;    // required if exportType = "family"
    private Integer partNo;     // required if exportType = "part"
    private String orderBy;     // "family" or "serial", default = "family"
}
