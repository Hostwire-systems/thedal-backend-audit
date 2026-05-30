package com.thedal.thedal_app.election.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FieldReorderRequest {
    private String fieldLabel; 
    private Integer newOrderIndex; 
}