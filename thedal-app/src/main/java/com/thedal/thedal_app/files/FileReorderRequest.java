package com.thedal.thedal_app.files;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileReorderRequest {
    private Long fileId;
    private Integer newOrderIndex;
}
