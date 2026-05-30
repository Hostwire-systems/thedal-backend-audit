package com.thedal.thedal_app.election.dtos;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStaticFieldStatusRequest {

    @JsonProperty("fieldStatuses")
    private List<StaticFieldStatusDTO> fieldStatuses;
}