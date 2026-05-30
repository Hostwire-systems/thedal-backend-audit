package com.thedal.thedal_app.voter.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExportJobListResponse {
    private List<VoterExportStatusResponse> exports;
    private long totalCount;

    public ExportJobListResponse(List<VoterExportStatusResponse> exports, long totalCount) {
        this.exports = exports;
        this.totalCount = totalCount;
    }

}
