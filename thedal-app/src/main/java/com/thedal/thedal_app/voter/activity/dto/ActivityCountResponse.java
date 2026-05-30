package com.thedal.thedal_app.voter.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityCountResponse {
    
    private String voterId;
    private Integer voterSlipPrintCount;
    private Integer familySlipPrintCount;
    private Integer benefitSlipPrintCount;
    private Integer whatsappShareCount;
    private Integer smsShareCount;
    private Integer voiceShareCount;
    private Integer totalActivityCount;
}
