package com.thedal.thedal_app.sirreport;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Result of comparing two voter lists
 */
@Data
@AllArgsConstructor
class ComparisonResult {
    private List<VoterRecord> additions;
    private List<VoterRecord> deletions;
    private List<ShiftRecord> shifts;
}
