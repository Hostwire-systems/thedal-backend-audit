package com.thedal.thedal_app.voter.duplicate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DuplicateVoterExportRow {
    private Long groupId;
    private Integer groupSize;
    private Long voterId;
    private String voterFnameEn;
    private String voterLnameEn;
    private String rlnFnameEn;
    private String rlnLnameEn;
    private Integer partNo;
    private Long serialNo;
    private String epicNumber;
    private Integer age;
    private String gender;
    private Integer sectionNo;
}
