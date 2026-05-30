package com.thedal.thedal_app.volunteer.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VolunteerUploadSummary {
    private long totalRecords;
    private long successCount;
    private long failedCount;
    private List<FailedRecord> failedRecords;

    @Getter
    @Setter
    public static class FailedRecord {
        private int rowNumber; // For Excel, row number; for CSV, line number
        private String reason; // Reason for failure
        private String data;   // Raw data (e.g., CSV line or Excel row data as string)

        public FailedRecord(int rowNumber, String reason, String data) {
            this.rowNumber = rowNumber;
            this.reason = reason;
            this.data = data;
        }
    }

    public VolunteerUploadSummary() {
        this.totalRecords = 0;
        this.successCount = 0;
        this.failedCount = 0;
        this.failedRecords = new ArrayList<>();
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void incrementFailed(int rowNumber, String reason, String data) {
        this.failedCount++;
        this.failedRecords.add(new FailedRecord(rowNumber, reason, data));
    }
}