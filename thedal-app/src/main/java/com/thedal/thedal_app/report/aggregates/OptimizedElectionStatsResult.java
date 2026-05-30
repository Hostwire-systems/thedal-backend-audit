package com.thedal.thedal_app.report.aggregates;

import lombok.Data;

/**
 * Result object for the optimized election stats query.
 * Contains all aggregated counts from a single query execution.
 */
@Data
public class OptimizedElectionStatsResult {
    private int totalVoters;
    private int totalFamily;
    private int distinctPincode;
    private int distinctMobile;
    private int male;
    private int female;
    private int transgender;
    private int age18To30;
    private int age30To40;
    private int age40To50;
    private int age50To60;
    private int age60To70;
    private int ageGreaterThan70;
    private int firstTimeVoters;
    private int seniorCitizens;
    private int superSeniors;
    private int dateOfBirth;
    private int starVoters;
    private int religionCount;
    private int casteCount;
    private int totalMobileCount;
    private int maleMobileCount;
    private int femaleMobileCount;
    private int transgenderMobileCount;
    private int maleDateOfBirthCount;
    private int femaleDateOfBirthCount;
    private int transgenderDateOfBirthCount;
    private int totalSchool;
    private int crossBoothFamily;
    private int oneVoterFamily;
}
