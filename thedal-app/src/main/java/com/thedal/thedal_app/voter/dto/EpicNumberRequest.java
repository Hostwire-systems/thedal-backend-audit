package com.thedal.thedal_app.voter.dto;

import java.util.UUID;

public class EpicNumberRequest {
	
	private String otherEpicNumber;
	private UUID familyId;

    public String getOtherEpicNumber() {
        return otherEpicNumber;
    }

    public void setOtherEpicNumber(String otherEpicNumber) {
        this.otherEpicNumber = otherEpicNumber;
    }
    
    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

}
