package com.thedal.thedal_app.election.dtos;

import lombok.Data;

@Data
public class BannerActiveStatusRequest {
    private Long fileId;
    private Boolean isActive;
}