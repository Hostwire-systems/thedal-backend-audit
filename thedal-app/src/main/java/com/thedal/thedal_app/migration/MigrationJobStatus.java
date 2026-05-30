package com.thedal.thedal_app.migration;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the status of a migration job
 */
public enum MigrationJobStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}
