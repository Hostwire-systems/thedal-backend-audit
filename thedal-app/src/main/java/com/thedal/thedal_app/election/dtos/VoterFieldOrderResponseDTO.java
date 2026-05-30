package com.thedal.thedal_app.election.dtos;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterFieldOrderResponseDTO {
    private Long id;
    private Long electionId;
    private List<FieldOrderItem> fields;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldOrderItem {
        private String name;
        private Integer orderIndex;
    }
}