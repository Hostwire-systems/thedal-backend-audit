package com.thedal.thedal_app.migration;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationReport {
    private Long accountId;
    private Long electionId;
    private Map<String, EntityValidationResult> entityResults;
    private boolean overallStatus;
    private String summary;
    private long validationTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityValidationResult {
        private String entityName;
        private long postgresCount;
        private long mongoCount;
        private boolean isConsistent;
        private String discrepancyDetails;
    }
}
