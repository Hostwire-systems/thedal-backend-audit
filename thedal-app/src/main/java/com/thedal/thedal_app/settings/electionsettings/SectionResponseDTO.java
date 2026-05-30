package com.thedal.thedal_app.settings.electionsettings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SectionResponseDTO {
    private Long id;
    private Integer partNo;
    private Integer sectionNo;
    private String sectionNameEn;
    private String sectionNameL1;
   
    
    public SectionResponseDTO(Long id, Integer partNo, Integer sectionNo, String sectionNameEn, String sectionNameL1) {
        this.id = id;
        this.partNo = partNo;
        this.sectionNo = sectionNo;
        this.sectionNameEn = sectionNameEn;
        this.sectionNameL1 = sectionNameL1;
    }
}
