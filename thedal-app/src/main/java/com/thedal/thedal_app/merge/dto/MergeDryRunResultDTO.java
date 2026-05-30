package com.thedal.thedal_app.merge.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.thedal.thedal_app.merge.MergeField;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MergeDryRunResultDTO {
    private boolean dryRun;
    private Long sourceElectionId;
    private Long targetElectionId;
    private List<MergeField> selectedFields;
    private long votersMatched; // EPIC in both
    private long votersAffected; // at least one field will change
    private long missingEpicInTargetCount; // EPIC present in source but not destination
    private List<String> missingEpicSample;
    private Map<String, Object> fieldStats; // per-field map (counts, missing names, etc.)
    private Map<String, Object> fieldAvailability; // unsupported / partial
    private List<String> warnings;
    private boolean canProceed;
    private long estimatedRuntimeSeconds;
    private Instant generatedAt;
}
