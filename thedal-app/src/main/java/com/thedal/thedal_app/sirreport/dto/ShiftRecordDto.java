package com.thedal.thedal_app.sirreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftRecordDto {
    private String epicNumber;
    private Integer oldPartNo;
    private Integer newPartNo;
    private String voterNameEn;
    private Long serialNo;
    private Integer sectionNo;
    private String houseNoEn;
    private Integer age;
    private String gender;
}
