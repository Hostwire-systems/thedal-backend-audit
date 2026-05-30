package com.thedal.thedal_app.merge.dto;

import com.thedal.thedal_app.merge.entity.MergeJobEntity;
import com.thedal.thedal_app.merge.MergeJobStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class MergeJobDtos {
    public record MergeJobSummary(
            UUID id,
            Long targetElectionId,
            MergeJobStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
        Long processedVoters,
        Long totalVoters,
            String errorMessage
    ) {
        public static MergeJobSummary from(MergeJobEntity e) {
            return new MergeJobSummary(
                    e.getId(),
                    e.getTargetElectionId(),
                    e.getStatus(),
                    e.getCreatedAt(),
                    e.getStartedAt(),
                    e.getFinishedAt(),
            e.getProcessedVoters(),
            e.getTotalVoters(),
                    e.getErrorMessage()
            );
        }
    }

    public record MergeJobDetail(
            UUID id,
            Long targetElectionId,
            MergeJobStatus status,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt,
        Long processedVoters,
        Long totalVoters,
            String errorMessage,
        List<String> fields,
        String resultStatsJson
    ) {
    public static MergeJobDetail from(MergeJobEntity e, List<String> fields) {
            return new MergeJobDetail(
                    e.getId(),
                    e.getTargetElectionId(),
                    e.getStatus(),
                    e.getCreatedAt(),
                    e.getStartedAt(),
                    e.getFinishedAt(),
            e.getProcessedVoters(),
            e.getTotalVoters(),
                    e.getErrorMessage(),
            fields,
            e.getResultStatsJson()
            );
        }
    }
}
