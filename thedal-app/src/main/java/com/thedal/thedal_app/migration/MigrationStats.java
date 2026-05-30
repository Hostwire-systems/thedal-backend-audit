package com.thedal.thedal_app.migration;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MigrationStats {
    private Long accountId;
    private Long electionId;
    private Map<String, EntityStats> entityStats;
    private long totalPostgresRecords;
    private long totalMongoRecords;
    private double migrationCompleteness;
    private String lastMigrationDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityStats {
        private String entityName;
        private long postgresCount;
        private long mongoCount;
        private boolean hasMigration;
        private String lastMigrationTime;
    }
}
