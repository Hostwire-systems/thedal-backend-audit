package com.thedal.thedal_app.voter.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VoterVoteRequest {
	
	//private String voterId;
	private String epicNumber;
    private Boolean hasVoted;
    private LocalDateTime votedTimestamp; // Only keep this timestamp

    // Getters and setters
//    public String getVoterId() {
//        return voterId;
//    }
//
//    public void setVoterId(String voterId) {
//        this.voterId = voterId;
//    }
    public String getEpicNumber() {
        return epicNumber;
    }

    public void setEpicNumber(String epicNumber) {
        this.epicNumber = epicNumber;
    }

    public Boolean getHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(Boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public LocalDateTime getVotedTimestamp() {
        return votedTimestamp;
    }

    public void setVotedTimestamp(LocalDateTime votedTimestamp) {
        this.votedTimestamp = votedTimestamp;
    }

}
