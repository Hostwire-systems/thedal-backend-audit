package com.thedal.thedal_app.voter.dto;

import java.util.List;
import java.util.UUID;

import com.thedal.thedal_app.voter.VoterEntity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendGroupDTO {
    private UUID friendId;
    private List<VoterEntity> members;
    private Integer friendCount;
}