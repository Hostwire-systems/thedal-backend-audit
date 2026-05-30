package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoterStatusDTO {
    private Long serialNo;
    private String epicNumber;
    private Boolean hasVoted;
    private LocalDateTime votedTimestamp;
}