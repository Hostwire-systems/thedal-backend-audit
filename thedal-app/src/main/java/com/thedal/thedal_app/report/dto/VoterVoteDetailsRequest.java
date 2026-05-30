package com.thedal.thedal_app.report.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoterVoteDetailsRequest {
    private Integer boothNumber;

    private LocalDateTime voteTimestamp;

}
