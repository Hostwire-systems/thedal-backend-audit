package com.thedal.thedal_app.sirreport;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import com.github.pjfanning.xlsx.StreamingReader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ExcelReaderService {
    
    /**
     * Read voter records from Excel file and return a map of EPIC -> VoterRecord
     * Note: This method is deprecated - use streaming database approach instead
     */
    @Deprecated
    public Map<String, VoterRecord> readVoterFile(MultipartFile file) throws IOException {
        return readVoterFile(file.getInputStream(), file.getOriginalFilename());
    }
    
    /**
     * Read voter records using streaming to avoid memory issues
     * Note: This method is deprecated - use streaming database approach instead
     */
    @Deprecated
    public Map<String, VoterRecord> readVoterFile(InputStream inputStream, String fileName) throws IOException {
        Map<String, VoterRecord> voterMap = new HashMap<>();
        int skippedRows = 0;
        int duplicateEpics = 0;
        
        try (Workbook workbook = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Read header row
            if (!rowIterator.hasNext()) {
                throw new IOException("Excel file is empty");
            }
            
            Row headerRow = rowIterator.next();
            Map<String, Integer> columnIndices = findColumnIndices(headerRow);
            
            log.info("Column mapping for file {}: {}", fileName, columnIndices);
            
            // Read data rows using iterator
            int rowNum = 1;
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                rowNum++;
                
                VoterRecord voter = extractVoterRecord(row, columnIndices);
                if (voter != null && voter.getEpicNumber() != null && !voter.getEpicNumber().trim().isEmpty()) {
                    String normalizedEpic = voter.getEpicNumber().trim().toUpperCase();
                    if (voterMap.containsKey(normalizedEpic)) {
                        duplicateEpics++;
                        log.warn("Duplicate EPIC found: {} in file {}", normalizedEpic, fileName);
                    }
                    voterMap.put(normalizedEpic, voter);
                } else {
                    skippedRows++;
                }
            }
        }
        
        log.info("Read {} voter records from file: {} (skipped: {}, duplicates: {})", 
                voterMap.size(), fileName, skippedRows, duplicateEpics);
        return voterMap;
    }
    
    /**
     * Find column indices from header row (public for streaming)
     */
    public Map<String, Integer> findColumnIndices(Row headerRow) {
        Map<String, Integer> indices = new HashMap<>();
        
        if (headerRow == null) return indices;
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            
            String header = getCellValueAsString(cell).trim().toUpperCase();
            
            // Map common header variations - check most specific patterns first
            // Epic/EPIC_ID column
            if (header.equals("EPIC_ID") || header.equals("EPIC ID") || header.equals("EPIC")) {
                indices.put("EPIC", i);
            } 
            // Part_No column - must contain PART and NO/NUMBER
            else if ((header.equals("PART_NO") || header.equals("PART NO") || header.equals("PARTNO")) ||
                     (header.contains("PART") && (header.contains("NUMBER") || header.endsWith("NO")))) {
                if (!indices.containsKey("PART_NO")) { // Take first match only
                    indices.put("PART_NO", i);
                }
            }
            // Serial number - SrNo
            else if (header.equals("SRNO") || header.equals("SR NO") || header.equals("SR_NO") || 
                     header.equals("SERIAL_NO") || header.equals("SLNO") || header.equals("SL NO")) {
                indices.put("SERIAL_NO", i);
            }
            // Section_No column
            else if (header.equals("SECTION_NO") || header.equals("SECTION NO") || header.equals("SECTIONNO")) {
                if (!indices.containsKey("SECTION_NO")) { // Take first match only
                    indices.put("SECTION_NO", i);
                }
            }
            // House number
            else if (header.contains("HOUSE") && (header.contains("NO") || header.contains("NUMBER"))) {
                indices.put("HOUSE_NO", i);
            }
            // Name in English
            else if (header.equals("NAME_ENG") || header.equals("NAME ENG") || 
                     (header.contains("NAME") && header.contains("ENG"))) {
                if (!indices.containsKey("NAME_EN")) { // Take first match only
                    indices.put("NAME_EN", i);
                }
            }
            // Age
            else if (header.equals("AGE")) {
                indices.put("AGE", i);
            }
            // Gender
            else if (header.equals("SEX") || header.equals("GENDER")) {
                indices.put("GENDER", i);
            }
        }
        
        return indices;
    }
    
    /**
     * Extract voter record from a row (public for streaming)
     */
    public VoterRecord extractVoterFromRow(Row row, Map<String, Integer> columnIndices, String fileName) {
        return extractVoterRecord(row, columnIndices);
    }
    
    /**
     * Extract voter record from a row
     */
    private VoterRecord extractVoterRecord(Row row, Map<String, Integer> columnIndices) {
        VoterRecord voter = new VoterRecord();
        
        try {
            // EPIC Number (required)
            if (columnIndices.containsKey("EPIC")) {
                String epic = getCellValueAsString(row.getCell(columnIndices.get("EPIC")));
                if (epic == null || epic.trim().isEmpty()) {
                    return null; // Skip rows without EPIC
                }
                voter.setEpicNumber(epic.trim().toUpperCase());
            } else {
                return null; // Can't process without EPIC column
            }
            
            // Part Number (required for comparison)
            if (columnIndices.containsKey("PART_NO")) {
                voter.setPartNo(getCellValueAsInteger(row.getCell(columnIndices.get("PART_NO"))));
            }
            
            // Serial Number
            if (columnIndices.containsKey("SERIAL_NO")) {
                voter.setSerialNo(getCellValueAsLong(row.getCell(columnIndices.get("SERIAL_NO"))));
            }
            
            // Section Number
            if (columnIndices.containsKey("SECTION_NO")) {
                voter.setSectionNo(getCellValueAsInteger(row.getCell(columnIndices.get("SECTION_NO"))));
            }
            
            // House Number
            if (columnIndices.containsKey("HOUSE_NO")) {
                voter.setHouseNoEn(getCellValueAsString(row.getCell(columnIndices.get("HOUSE_NO"))));
            }
            
            // Voter Name
            if (columnIndices.containsKey("NAME_EN")) {
                voter.setVoterNameEn(getCellValueAsString(row.getCell(columnIndices.get("NAME_EN"))));
            }
            
            // Age
            if (columnIndices.containsKey("AGE")) {
                voter.setAge(getCellValueAsInteger(row.getCell(columnIndices.get("AGE"))));
            }
            
            // Gender
            if (columnIndices.containsKey("GENDER")) {
                voter.setGender(getCellValueAsString(row.getCell(columnIndices.get("GENDER"))));
            }
            
            return voter;
        } catch (Exception e) {
            log.warn("Error extracting voter record from row {}: {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.warn("Error parsing integer from cell: {}", e.getMessage());
        }
        return null;
    }
    
    private Long getCellValueAsLong(Cell cell) {
        if (cell == null) return null;
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (long) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) return null;
                return Long.parseLong(value);
            }
        } catch (Exception e) {
            log.warn("Error parsing long from cell: {}", e.getMessage());
        }
        return null;
    }
}
