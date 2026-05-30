package com.thedal.thedal_app.voter.dto;

import org.springframework.data.domain.Page;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendGroupResponseDTO {
    private Page<FriendGroupDTO> friendGroups;
    private GenderStatsDTO genderStats;

    public FriendGroupResponseDTO(Page<FriendGroupDTO> friendGroups, GenderStatsDTO genderStats) {
        this.friendGroups = friendGroups;
        this.genderStats = genderStats;
    }
}