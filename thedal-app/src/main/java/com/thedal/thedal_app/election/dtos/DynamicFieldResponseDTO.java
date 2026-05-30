package com.thedal.thedal_app.election.dtos;

import org.springframework.data.domain.Page;

import com.thedal.thedal_app.election.DynamicFieldEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFieldResponseDTO {
    private Page<DynamicFieldEntity> fieldsPage;
}