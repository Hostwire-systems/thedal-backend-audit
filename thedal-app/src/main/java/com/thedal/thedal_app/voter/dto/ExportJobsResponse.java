package com.thedal.thedal_app.voter.dto;

import java.util.List;

import com.thedal.thedal_app.voter.ExportJobDTO;

public class ExportJobsResponse {
    private List<ExportJobDTO> exportJobs;
    private long totalCount;

    public ExportJobsResponse(List<ExportJobDTO> exportJobs, long totalCount) {
        this.exportJobs = exportJobs;
        this.totalCount = totalCount;
    }

    // Getters and Setters
    public List<ExportJobDTO> getExportJobs() { return exportJobs; }
    public void setExportJobs(List<ExportJobDTO> exportJobs) { this.exportJobs = exportJobs; }
    public long getTotalCount() { return totalCount; }
    public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
}
