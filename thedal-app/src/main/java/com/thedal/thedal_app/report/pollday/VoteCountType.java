package com.thedal.thedal_app.report.pollday;


public enum VoteCountType {

	TOTAL_VOTERS(1), 
	POLLED_2025(2),
	POLLED_2024(4), 
	NOT_VOTED_VOTERS(8);
	
	 private final int value;

    VoteCountType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    
    public static VoteCountType fromValue(int value) {
        for (VoteCountType type : values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}

