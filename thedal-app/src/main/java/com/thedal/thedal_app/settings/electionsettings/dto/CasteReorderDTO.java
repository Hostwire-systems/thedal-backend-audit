package com.thedal.thedal_app.settings.electionsettings.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class CasteReorderDTO {
    private Long casteId;
    private Integer orderIndex;
}
