package com.thedal.thedal_app.report.dto;

import java.util.List;

import com.thedal.thedal_app.report.PollingAgeWiseEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PollingAgeWiseResponse {
//    private List<PollingAgeWiseRedis> pollingAgeWiseRecords;
//    private double overallPolledPercentage;
	private List<PollingAgeWiseEntity> pollingAgeWiseRecords;
    private double overallPolledPercentage;

}
