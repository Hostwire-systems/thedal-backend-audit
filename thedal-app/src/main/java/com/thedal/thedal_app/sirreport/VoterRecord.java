package com.thedal.thedal_app.sirreport;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterRecord {
    private String epicNumber;
    private Integer partNo;
    private String voterNameEn;
    private Long serialNo;
    private Integer sectionNo;
    private String houseNoEn;
    private Integer age;
    private String gender;
}
