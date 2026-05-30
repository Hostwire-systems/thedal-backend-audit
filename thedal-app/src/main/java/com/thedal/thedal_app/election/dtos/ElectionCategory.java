package com.thedal.thedal_app.election.dtos;

public enum ElectionCategory {

	POLITICAL("Political"),
    NON_POLITICAL("Non Political");

    private final String description;

    ElectionCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
	
}
