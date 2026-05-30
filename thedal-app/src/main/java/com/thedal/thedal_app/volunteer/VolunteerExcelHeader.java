package com.thedal.thedal_app.volunteer;

import org.apache.poi.ss.usermodel.Row;

public class VolunteerExcelHeader {
    
    public static void createHeaderRow(Row headerRow) {
        int colNum = 0;
        headerRow.createCell(colNum++).setCellValue("VOLUNTEER_ID");
        headerRow.createCell(colNum++).setCellValue("FIRST_NAME");
        headerRow.createCell(colNum++).setCellValue("LAST_NAME");
        headerRow.createCell(colNum++).setCellValue("EMAIL");
        headerRow.createCell(colNum++).setCellValue("MOBILE_NUMBER");
        headerRow.createCell(colNum++).setCellValue("WHATS_APP_NUMBER");
        headerRow.createCell(colNum++).setCellValue("GENDER");
        headerRow.createCell(colNum++).setCellValue("STATUS");
        headerRow.createCell(colNum++).setCellValue("ASSIGNED_BOOTHS");
        headerRow.createCell(colNum++).setCellValue("STREET");
        headerRow.createCell(colNum++).setCellValue("CITY");
        headerRow.createCell(colNum++).setCellValue("STATE");
        headerRow.createCell(colNum++).setCellValue("POSTAL_CODE");
        headerRow.createCell(colNum++).setCellValue("COUNTRY");
        headerRow.createCell(colNum++).setCellValue("PHOTO_URL");
        headerRow.createCell(colNum++).setCellValue("REMARKS");
        headerRow.createCell(colNum++).setCellValue("ROLE_ID");
        headerRow.createCell(colNum++).setCellValue("ADMIN_USER_ID");
        headerRow.createCell(colNum++).setCellValue("CREATED_TIME");
        headerRow.createCell(colNum++).setCellValue("MODIFIED_TIME");
    }
}