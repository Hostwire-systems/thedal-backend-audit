package com.thedal.thedal_app.volunteer.dto;

public class MigrateVolunteerAdminResponseDTO {
    private String message;
    private int updatedRecords;

    public MigrateVolunteerAdminResponseDTO(String message, int updatedRecords) {
        this.message = message;
        this.updatedRecords = updatedRecords;
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getUpdatedRecords() {
        return updatedRecords;
    }

    public void setUpdatedRecords(int updatedRecords) {
        this.updatedRecords = updatedRecords;
    }
}