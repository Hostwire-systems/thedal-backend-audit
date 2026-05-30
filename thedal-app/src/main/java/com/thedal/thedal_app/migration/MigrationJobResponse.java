package com.thedal.thedal_app.migration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationJobResponse {
    private String jobId;
    private String status;
    private String description;
    private Long accountId;
    private Long electionId;
    private long estimatedDuration;
}
