package com.thedal.thedal_app.cpanel.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatalogueReorderRequest {
    private Long itemId;
    private Integer newOrderIndex;
}