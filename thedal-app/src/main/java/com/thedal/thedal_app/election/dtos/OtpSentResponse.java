package com.thedal.thedal_app.election.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OtpSentResponse {
    private Long userId;

    public OtpSentResponse(Long userId) {
        this.userId = userId;
    }
}