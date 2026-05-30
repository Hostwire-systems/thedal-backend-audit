package com.thedal.thedal_app.migration;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectiveMigrationRequest {
    private Long accountId;
    private Long electionId;
    private List<String> modules;
    private int batchSize = 1000;
    private boolean overwriteExisting = false;
}
