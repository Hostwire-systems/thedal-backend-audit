package com.thedal.thedal_app.election;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;

public class SurveyExcelDataRow {
	
	
	public static void populateDataRow(Row row, SurveyFormSubmissionEntity submission, 
            List<Map<String, Object>> customFields, 
            Set<String> extraFields) {
       Workbook workbook = row.getSheet().getWorkbook();
       // Create a cell style for dates
       CellStyle dateCellStyle = workbook.createCellStyle();
       CreationHelper createHelper = workbook.getCreationHelper();
       dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

       int colNum = 0;
       // Set submission ID
       setCellValue(row, colNum++, submission.getId());
       // Set submittedAt with proper date formatting
       Cell submittedAtCell = row.createCell(colNum++);
       LocalDateTime submittedAt = submission.getSubmittedAt();
       if (submittedAt != null) {
           submittedAtCell.setCellValue(Date.from(submittedAt.atZone(ZoneId.systemDefault()).toInstant()));
           submittedAtCell.setCellStyle(dateCellStyle); // Apply date format
       } else {
           submittedAtCell.setCellValue(""); // Handle null case
       }
       // Set form ID
       setCellValue(row, colNum++, submission.getFormId());

       // Set custom fields
       Map<String, Object> submissionData = submission.getSubmissionData();
       if (customFields != null) {
           for (Map<String, Object> field : customFields) {
                String label = field.get("label").toString().trim();
                Object value = submissionData != null ? submissionData.get(label) : null;
                setCellValue(row, colNum++, value);
           }
        }

         // Add extra fields
        for (String extraField : extraFields) {
             if (customFields == null || customFields.stream().noneMatch(field -> field.get("label").toString().trim().equalsIgnoreCase(extraField))) {
                 Object value = submissionData != null ? submissionData.getOrDefault(extraField, null) : null;
                 setCellValue(row, colNum++, value);
              }
         }
   }

     private static void setCellValue(Row row, int colNum, Object value) {
          Cell cell = row.createCell(colNum);
          if (value != null) {
              if (value instanceof String) {
                  cell.setCellValue((String) value);
               } else if (value instanceof Integer) {
                   cell.setCellValue((Integer) value);
               } else if (value instanceof Long) {
                    cell.setCellValue((Long) value);
               } else if (value instanceof Double) {
                     cell.setCellValue((Double) value);
               } else if (value instanceof Boolean) {
                      cell.setCellValue((Boolean) value);
               } else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    String formatted = list.stream()
                                      .map(Object::toString)
                                      .collect(Collectors.joining(", "));
                       cell.setCellValue(formatted);
                 } else {
                      cell.setCellValue(value.toString());
               }
           } else {
                    cell.setCellValue("");
           }
}


	
//	public static void populateDataRow(Row row, SurveyFormSubmissionEntity submission, List<Map<String, Object>> customFields, Set<String> extraFields) {
//        int colNum = 0;
//        setCellValue(row, colNum++, submission.getId());
//        setCellValue(row, colNum++, submission.getSubmittedAt());
//        setCellValue(row, colNum++, submission.getFormId());
//
//        Map<String, Object> submissionData = submission.getSubmissionData();
//        if (customFields != null) {
//            for (Map<String, Object> field : customFields) {
//                String label = field.get("label").toString().trim();
//                Object value = submissionData != null ? submissionData.get(label) : null;
//                setCellValue(row, colNum++, value);
//            }
//        }
//
//        // Add extra fields
//        for (String extraField : extraFields) {
//            if (customFields == null || customFields.stream().noneMatch(field -> field.get("label").toString().trim().equalsIgnoreCase(extraField))) {
//                Object value = submissionData != null ? submissionData.getOrDefault(extraField, null) : null;
//                setCellValue(row, colNum++, value);
//            }
//        }
//    }
//	
//	private static void setCellValue(Row row, int colNum, Object value) {
//        if (value != null) {
//            if (value instanceof String) {
//                row.createCell(colNum).setCellValue((String) value);
//            } else if (value instanceof Integer) {
//                row.createCell(colNum).setCellValue((Integer) value);
//            } else if (value instanceof Long) {
//                row.createCell(colNum).setCellValue((Long) value);
//            } else if (value instanceof Double) {
//                row.createCell(colNum).setCellValue((Double) value);
//            } else if (value instanceof Boolean) {
//                row.createCell(colNum).setCellValue((Boolean) value);
//            } else if (value instanceof java.time.LocalDateTime) {
//                row.createCell(colNum).setCellValue((java.time.LocalDateTime) value);
//            } else if (value instanceof List) {
//                List<?> list = (List<?>) value;
//                String formatted = list.stream()
//                        .map(Object::toString)
//                        .collect(Collectors.joining(", "));
//                row.createCell(colNum).setCellValue(formatted);
//            } else {
//                row.createCell(colNum).setCellValue(value.toString());
//            }
//        } else {
//            row.createCell(colNum).setCellValue("");
//        }
//    }
		

    
}