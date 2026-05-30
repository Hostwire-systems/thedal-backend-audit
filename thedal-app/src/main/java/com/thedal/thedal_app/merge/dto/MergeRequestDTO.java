package com.thedal.thedal_app.merge.dto;

import java.util.List;

import com.thedal.thedal_app.merge.MergeField;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MergeRequestDTO {
    private Long sourceElectionId;
    private List<MergeField> fields; // user-selected
    private boolean dryRun = false;
}
