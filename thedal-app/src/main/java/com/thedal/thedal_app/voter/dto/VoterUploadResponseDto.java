package com.thedal.thedal_app.voter.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoterUploadResponseDto {

	private Long totalVoters;
    private List<Integer> boothList;
}
