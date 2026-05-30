package com.thedal.thedal_app.settings.electionsettings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionDTO {
	private Integer partNo;
    private Integer sectionNo;
    private String sectionNameEn;
    private String sectionNameL1;
    
    public SectionDTO(Integer partNo, Integer sectionNo, String sectionNameEn, String sectionNameL1) {
        this.partNo = partNo;
        this.sectionNo = sectionNo;
        this.sectionNameEn = sectionNameEn;
        this.sectionNameL1 = sectionNameL1;
    }
    
}
