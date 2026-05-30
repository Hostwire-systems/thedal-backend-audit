package com.thedal.thedal_app.cpanel.dtos;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class CatalogueItemRequest {
    private Long itemId;
    private String name;
    private Double price;
    private String description;
    private MultipartFile image;
    private Boolean active;
}
