package com.thedal.thedal_app.sirreport;

import lombok.Data;

/**
 * Record of a voter who shifted between part numbers
 */
@Data
class ShiftRecord {
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
