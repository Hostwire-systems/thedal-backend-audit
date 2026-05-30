package com.thedal.thedal_app.report.aggregates;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestrates periodic full recomputation of all election reporting aggregates.
 * (Future optimization: detect and process only changed elections.)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ElectionDashboardAggregationScheduler {

    private final ElectionStatsAggregationService aggregationService;
    private final ElectionDemographicsAggregationService demographicsAggregationService;
    private final ElectionBoothProgressAggregationService boothProgressAggregationService;
    private final ElectionPartyPollingAggregationService partyPollingAggregationService;
    private final ElectionFeedbackIssuesAggregationService feedbackIssuesAggregationService;
    private final ElectionContactStatusAggregationService contactStatusAggregationService;

    @Value("${AGGREGATION_ENABLED:true}")
    private boolean aggregationEnabled;

    private static final String DISTINCT_ELECTIONS_SQL = "SELECT DISTINCT account_id, election_id FROM _voters";

    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void aggregateElectionStats() {
        if (!aggregationEnabled) {
            return; // feature toggle off
        }
        aggregationService.aggregateAllDistinctElections(DISTINCT_ELECTIONS_SQL);
        demographicsAggregationService.aggregateAll();
        boothProgressAggregationService.aggregateAll();
        partyPollingAggregationService.aggregateAll();
        feedbackIssuesAggregationService.aggregateAll();
        contactStatusAggregationService.aggregateAll();
        // Note: Cadre and Poll-day dashboard aggregates now run in separate reporting service
    }
}
