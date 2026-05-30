package com.thedal.thedal_app.report.aggregates;

public enum AggregationJobStatus {
    QUEUED,       // Job created but not started
    IN_PROGRESS,  // Currently processing
    COMPLETED,    // Successfully finished
    FAILED,       // Error occurred
    CANCELLED     // User cancelled
}
