package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LanguageReorderRequest {
    private Long languageId;
    private Integer newOrderIndex;
}