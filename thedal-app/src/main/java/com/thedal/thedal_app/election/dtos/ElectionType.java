package com.thedal.thedal_app.election.dtos;

public enum ElectionType {
	
	GENERAL_ELECTION("General Election"),
    BY_ELECTION("By Election");

    private final String description;

    ElectionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
