package com.thedal.thedal_app.voter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WinningProbabilityDTO {
    private Long electionId;
    private DefaultPartyInfo defaultParty;
    private ElectionStatistics statistics;
    private ProbabilityInfo winningProbability;
    private String computedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefaultPartyInfo {
        private Long partyId;
        private String partyName;
        private String partyShortName;
        private String partyImage;
        private String partyColor;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ElectionStatistics {
        private Long totalVoters;
        private Long totalVoted;
        private Double overallTurnout;
        private Long defaultPartySupporters;
        private Long defaultPartyVoted;
        private Double defaultPartyTurnout;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProbabilityInfo {
        private Double supportPercentage;
        private Double probability;
        private String confidence;
        private String message;
    }
}
