package com.thedal.thedal_app.election.dtos;

public enum ElectionBody {
	
	    UNION_BODY("Union Body (MP)"),
	    STATE_BODY("State Body (MLA)"),
	    URBAN_LOCAL("Urban Local"),
	    RURAL_LOCAL("Rural Local");

	    private final String description;

	    ElectionBody(String description) {
	        this.description = description;
	    }

	    public String getDescription() {
	        return description;
	    }

}
