package com.thedal.thedal_app.sirreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SirReportDetailResponse<T> {
    private String type; // ADDITIONS, DELETIONS, SHIFTS
    private Long total;
    private Page<T> data;
}
