package com.thedal.thedal_app.volunteer;

import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;

public class VolunteerExcelDataRow {
    
    public static void populateDataRow(Row row, VolunteerEntity volunteer) {
        int colNum = 0;
        setCellValue(row, colNum++, volunteer.getId());
        setCellValue(row, colNum++, volunteer.getUserEntity() != null ? volunteer.getUserEntity().getFirstName() : "");
        setCellValue(row, colNum++, volunteer.getUserEntity() != null ? volunteer.getUserEntity().getLastName() : "");
        setCellValue(row, colNum++, volunteer.getUserEntity() != null ? volunteer.getUserEntity().getEmail() : "");
        setCellValue(row, colNum++, volunteer.getUserEntity() != null ? volunteer.getUserEntity().getMobileNumber() : "");
        setCellValue(row, colNum++, volunteer.getWhatsAppNumber());
        setCellValue(row, colNum++, volunteer.getGender());
        setCellValue(row, colNum++, volunteer.getStatus());
        setCellValue(row, colNum++, volunteer.getAssignedBooth() != null ? volunteer.getAssignedBooth().stream().map(String::valueOf).collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, volunteer.getVolunteerAddress() != null ? volunteer.getVolunteerAddress().getStreet() : "");
        setCellValue(row, colNum++, volunteer.getVolunteerAddress() != null ? volunteer.getVolunteerAddress().getCity() : "");
        setCellValue(row, colNum++, volunteer.getVolunteerAddress() != null ? volunteer.getVolunteerAddress().getState() : "");
        setCellValue(row, colNum++, volunteer.getVolunteerAddress() != null ? volunteer.getVolunteerAddress().getPostalCode() : "");
        setCellValue(row, colNum++, volunteer.getVolunteerAddress() != null ? volunteer.getVolunteerAddress().getCountry() : "");
        setCellValue(row, colNum++, volunteer.getUserEntity() != null ? volunteer.getUserEntity().getProfilePicture() : "");
        setCellValue(row, colNum++, volunteer.getRemarks());
        setCellValue(row, colNum++, volunteer.getRoleId());
        setCellValue(row, colNum++, volunteer.getAdminUserId());
        setCellValue(row, colNum++, volunteer.getCreatedTime());
        setCellValue(row, colNum++, volunteer.getModifiedTime());
    }
    
    private static void setCellValue(Row row, int colNum, Object value) {
        if (value != null) {
            if (value instanceof String) {
                row.createCell(colNum).setCellValue((String) value);
            } else if (value instanceof Integer) {
                row.createCell(colNum).setCellValue((Integer) value);
            } else if (value instanceof Long) {
                row.createCell(colNum).setCellValue((Long) value);
            } else if (value instanceof Double) {
                row.createCell(colNum).setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                row.createCell(colNum).setCellValue((Boolean) value);
            } else if (value instanceof java.util.Date) {
                row.createCell(colNum).setCellValue((java.util.Date) value);
            } else {
                row.createCell(colNum).setCellValue(value.toString());
            }
        } else {
            row.createCell(colNum).setCellValue("");
        }
    }
}