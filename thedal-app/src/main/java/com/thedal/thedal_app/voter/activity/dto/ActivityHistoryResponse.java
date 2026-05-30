package com.thedal.thedal_app.voter.activity.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActivityHistoryResponse {
    
    private String voterId;
    private List<ActivityHistoryItem> activities;
    private Integer totalCount;
    private Integer pageNumber;
    private Integer pageSize;
    private Integer totalPages;
}
