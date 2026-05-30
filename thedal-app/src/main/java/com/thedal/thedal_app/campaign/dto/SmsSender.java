package com.thedal.thedal_app.campaign.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SmsSender {
    private String id;
    private String display;
    private String senderName; // SMS sender name (6 chars alphanumeric)
}