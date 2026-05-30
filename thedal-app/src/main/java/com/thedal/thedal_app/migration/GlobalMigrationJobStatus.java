package com.thedal.thedal_app.migration;

/**
 * Status enum for global migration jobs
 */
public enum GlobalMigrationJobStatus {
    PENDING,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
