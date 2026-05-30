package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class VoterVotingRequest {
    private Boolean hasVoted;
    private LocalDateTime votedTimestamp; 
}