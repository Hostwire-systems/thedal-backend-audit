package com.thedal.thedal_app.voter.dto;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendMappingRequest {
    private UUID friendId;
    private String friendEpicNumber;
}