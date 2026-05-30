package com.thedal.thedal_app.profileAPI.dtos;

public enum SubscriptionType {
	
	FREE("Free 30 Days Trial"),
    PREMIUM("Premium"),
    PAID("Paid");

    private final String description;
    
    SubscriptionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
