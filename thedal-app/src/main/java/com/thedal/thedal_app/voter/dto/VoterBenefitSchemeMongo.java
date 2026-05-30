package com.thedal.thedal_app.voter.dto;

import com.thedal.thedal_app.voter.VoterBenefitScheme;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VoterBenefitSchemeMongo {
    private Long id;
    private Long benefitSchemeId;
    private Boolean selected;

    public VoterBenefitSchemeMongo(VoterBenefitScheme voterBenefitScheme) {
        this.id = voterBenefitScheme.getId();
        this.benefitSchemeId = voterBenefitScheme.getBenefitScheme() != null ? 
            voterBenefitScheme.getBenefitScheme().getId() : null;
        this.selected = voterBenefitScheme.getSelected() != null ? 
            voterBenefitScheme.getSelected() : false;
    }
}