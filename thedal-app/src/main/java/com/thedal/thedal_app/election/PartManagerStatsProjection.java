package com.thedal.thedal_app.election;

/**
 * Projection interface for PartManager statistics queries
 * Used to get vulnerability counts without loading full entity data
 */
public interface PartManagerStatsProjection {
    Long getTotalCount();
    Long getHighVulnerabilityCount();
    Long getMediumVulnerabilityCount();
    Long getLowVulnerabilityCount();
}