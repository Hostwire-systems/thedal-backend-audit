package com.thedal.thedal_app.volunteer.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MigrationResult {
    private int updatedCount;
    private int failedCount;
    private List<String> errorMessages;

    public MigrationResult() {
        this.updatedCount = 0;
        this.failedCount = 0;
        this.errorMessages = new ArrayList<>();
    }
}