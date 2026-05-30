package com.thedal.thedal_app.report.pollday.export;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExportFilters {
    private List<String> parties;
    private List<String> religions;
    private List<String> casteCategories;
    private List<String> castes;
    private List<String> subCastes;
    private List<String> languages;
    private List<String> schemes;
    private List<String> genders;
    private Integer minAge;
    private Integer maxAge;
    private Boolean includeUnknownAge;
}
