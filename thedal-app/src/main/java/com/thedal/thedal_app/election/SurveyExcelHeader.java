package com.thedal.thedal_app.election;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;

public class SurveyExcelHeader {
    public static void createHeaderRow(Row headerRow, List<Map<String, Object>> customFields, Set<String> extraFields) {
        int colNum = 0;
        headerRow.createCell(colNum++).setCellValue("ID");
        //headerRow.createCell(colNum++).setCellValue("VOTER_ID");
        headerRow.createCell(colNum++).setCellValue("SUBMITTED_AT");
        headerRow.createCell(colNum++).setCellValue("FORM_ID");

        if (customFields != null) {
            for (Map<String, Object> field : customFields) {
                String label = field.get("label").toString().trim().toUpperCase();
                headerRow.createCell(colNum++).setCellValue(label);
            }
        }
        
        for (String extraField : extraFields) {
            if (customFields == null || customFields.stream().noneMatch(field -> field.get("label").toString().trim().equalsIgnoreCase(extraField))) {
                headerRow.createCell(colNum++).setCellValue(extraField.toUpperCase());
            }
        }
        
    }
}