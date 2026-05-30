package com.thedal.thedal_app.voter.familyexport;

import com.thedal.thedal_app.voter.VoterEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class FamilyExcelGenerator {

    /**
     * Generate Excel file with family voter data
     * 
     * @param voters List of voters to export
     * @param outputFile File to write Excel data to
     * @throws IOException if file writing fails
     */
    public void generateExcel(List<VoterEntity> voters, File outputFile) throws IOException {
        log.info("Generating Excel file with {} voters to {}", voters.size(), outputFile.getAbsolutePath());

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Family Voters");

            // Create header row with styling
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Family ID", "Family No", "EPIC Number", "Name (English)", "Name (Local)",
                "Age", "Gender", "Relative Name (English)", "Relative Name (Local)", "Relationship",
                "Part No", "Serial No", "Family Count", "Mobile Number", "House Number",
                "Section Name", "Full Address", "Member Verified", "Aadhaar Verified"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate data rows
            int rowNum = 1;
            for (VoterEntity voter : voters) {
                Row row = sheet.createRow(rowNum++);
                
                // Family ID
                createCell(row, 0, voter.getFamilyId() != null ? voter.getFamilyId().toString() : "", dataStyle);
                
                // Family No (familySequenceNumber)
                createCell(row, 1, voter.getFamilySequenceNumber(), dataStyle);
                
                // EPIC Number
                createCell(row, 2, voter.getEpicNumber(), dataStyle);
                
                // Name (English) - concatenate first and last name
                String nameEn = concatenateName(voter.getVoterFnameEn(), voter.getVoterLnameEn());
                createCell(row, 3, nameEn, dataStyle);
                
                // Name (Local) - L1 only
                String nameLocal = concatenateName(voter.getVoterFnameL1(), voter.getVoterLnameL1());
                createCell(row, 4, nameLocal, dataStyle);
                
                // Age
                createCell(row, 5, voter.getAge(), dataStyle);
                
                // Gender
                createCell(row, 6, voter.getGender(), dataStyle);
                
                // Relative Name (English)
                String rlnNameEn = concatenateName(voter.getRlnFnameEn(), voter.getRlnLnameEn());
                createCell(row, 7, rlnNameEn, dataStyle);
                
                // Relative Name (Local) - L1 only
                String rlnNameLocal = concatenateName(voter.getRlnFnameL1(), voter.getRlnLnameL1());
                createCell(row, 8, rlnNameLocal, dataStyle);
                
                // Relationship
                createCell(row, 9, voter.getRlnType(), dataStyle);
                
                // Part No
                createCell(row, 10, voter.getPartNo(), dataStyle);
                
                // Serial No
                createCell(row, 11, voter.getSerialNo(), dataStyle);
                
                // Family Count
                createCell(row, 12, voter.getFamilyCount(), dataStyle);
                
                // Mobile Number
                createCell(row, 13, voter.getMobileNo(), dataStyle);
                
                // House Number (using English version)
                createCell(row, 14, voter.getHouseNoEn(), dataStyle);
                
                // Section Name (using English version)
                createCell(row, 15, voter.getSectionNameEn(), dataStyle);
                
                // Full Address
                createCell(row, 16, voter.getFullAddress(), dataStyle);
                
                // Member Verified - Always "Yes" for exported data
                createCell(row, 17, "Yes", dataStyle);
                
                // Aadhaar Verified - Yes if aadhaarNumber is present
                String aadhaarVerified = (voter.getAadhaarNumber() != null && !voter.getAadhaarNumber().isEmpty()) ? "Yes" : "No";
                createCell(row, 18, aadhaarVerified, dataStyle);
            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // Add a bit of extra width for padding
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, (int)(currentWidth * 1.1));
            }

            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }

            log.info("Successfully generated Excel file with {} rows at {}", rowNum - 1, outputFile.getAbsolutePath());
        }
    }

    /**
     * Create header cell style with bold font and background color
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Background color
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // Font - bold
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        
        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    /**
     * Create data cell style with borders
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        // Alignment
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return style;
    }

    /**
     * Create cell with String value
     */
    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Create cell with Integer value
     */
    private void createCell(Row row, int column, Integer value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    /**
     * Create cell with Long value
     */
    private void createCell(Row row, int column, Long value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    /**
     * Concatenate first and last name
     */
    private String concatenateName(String firstName, String lastName) {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
