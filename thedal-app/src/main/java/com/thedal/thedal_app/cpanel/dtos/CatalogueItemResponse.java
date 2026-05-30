package com.thedal.thedal_app.cpanel.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CatalogueItemResponse {
    private Long id;
    private String name;
    private Double price;
    private String description;
    private String image;
    private Integer orderIndex;
    private Boolean active;
}