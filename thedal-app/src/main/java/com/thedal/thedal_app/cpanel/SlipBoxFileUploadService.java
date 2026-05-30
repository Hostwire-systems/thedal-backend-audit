package com.thedal.thedal_app.cpanel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadRepository;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SlipBoxFileUploadService {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;

    @Autowired
    private SlipBoxRepository slipBoxRepository;

    @Autowired
    private SlipBoxMongoRepository slipBoxMongoRepository;

    public Map<String, Integer> buildHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalizedHeader = normalizeHeader(cell.getStringCellValue());
            headerMapping.put(normalizedHeader, cell.getColumnIndex());
        }
        return headerMapping;
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return header.trim().replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_+", "_").toLowerCase();
    }

    public Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMapping.put(normalizedHeader, i);
        }
        return headerMapping;
    }

    @Transactional
    public void processCpanelSlipBoxExcelFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            bulkUploadEntity.setTotalRecords((long) (sheet.getPhysicalNumberOfRows() - 1));
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header
            processCpanelSlipBoxFile(rowIterator, headerMapping, bulkUploadEntity);
        } catch (Exception e) {
            log.error("Excel processing failed: {}", e.getMessage(), e);
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(bulkUploadEntity);
            throw e;
        }
    }

    @Transactional
    public void processCpanelSlipBoxCsvFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processCpanelSlipBoxFile(processingBr.lines().iterator(), headerMapping, bulkUploadEntity);
            }
        } catch (Exception e) {
            log.error("CSV processing failed: {}", e.getMessage(), e);
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            sectionBulkUploadRepository.save(bulkUploadEntity);
            throw e;
        }
    }

    @Transactional
    public void processCpanelSlipBoxFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                        SectionBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, SlipBoxEntity> slipBoxMap = new HashMap<>(); // Key: slip_box_id
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;
        Long accountId = 0L;
        Long electionId = 0L;

        while (rowIterator.hasNext()) {
            totalRecords++;
            Object rowObj = rowIterator.next();
            SectionBulkUploadEntity.RowError rowError = new SectionBulkUploadEntity.RowError(totalRecords);

            try {
                SlipBoxEntity slipBox = null;
                if (rowObj instanceof Row) {
                    slipBox = processCpanelExcelRow((Row) rowObj, headerMapping, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    slipBox = processCpanelCsvRow(fields, headerMapping, rowError);
                }

                if (slipBox != null) {
                    String uniqueKey = slipBox.getSlipBoxId();
                    if (slipBoxMap.containsKey(uniqueKey)) {
                        log.debug("Overriding duplicate slip box at row {}: slip_box_id={}", 
                                  totalRecords, slipBox.getSlipBoxId());
                    }
                    slipBoxMap.put(uniqueKey, slipBox);
                } else {
                    bulkUploadEntity.getRowErrors().add(rowError);
                    totalFailedRecords++;
                }
            } catch (Exception e) {
                log.error("Error processing row {}: {}", totalRecords, e.getMessage());
                rowError.addError("general", "Unexpected error: " + e.getMessage());
                bulkUploadEntity.getRowErrors().add(rowError);
                totalFailedRecords++;
            }
        }

        if (!slipBoxMap.isEmpty()) {
            List<SlipBoxEntity> slipBoxesToSave = new ArrayList<>(slipBoxMap.values());
            // Find existing slip boxes to avoid duplicates
            List<SlipBoxEntity> existingSlipBoxes = slipBoxRepository.findBySlipBoxIdInAndElectionId(
                    slipBoxesToSave.stream().map(SlipBoxEntity::getSlipBoxId).distinct().toList(),
                    electionId
            );

            // Filter out existing slip boxes
            Set<String> existingKeys = existingSlipBoxes.stream()
                    .map(SlipBoxEntity::getSlipBoxId)
                    .collect(Collectors.toSet());
            List<SlipBoxEntity> newSlipBoxes = slipBoxesToSave.stream()
                    .filter(slipBox -> !existingKeys.contains(slipBox.getSlipBoxId()))
                    .collect(Collectors.toList());

            if (!newSlipBoxes.isEmpty()) {
                saveBatch(newSlipBoxes, bulkUploadEntity);
                totalSuccessRecords = newSlipBoxes.size();
            } else {
                log.info("No new slip boxes to save; all provided slip_box_id values already exist.");
            }

            totalFailedRecords += (slipBoxesToSave.size() - totalSuccessRecords); // Duplicates count as "failed"
        }

        bulkUploadEntity.setTotalProcessedRecords(totalRecords);
        bulkUploadEntity.setTotalSuccessRecords(totalSuccessRecords);
        bulkUploadEntity.setTotalFailedRecords(totalFailedRecords);
        bulkUploadEntity.setEndTime(LocalDateTime.now());
        bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
        bulkUploadEntity.setStatus(totalSuccessRecords > 0 ? BulkUploadStatus.COMPLETED : BulkUploadStatus.FAILED);
        sectionBulkUploadRepository.save(bulkUploadEntity);

        log.info("Processed {} records: {} unique successful, {} failed (including duplicates) in {} ms",
                totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
    }

    private SlipBoxEntity processCpanelExcelRow(Row row, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String mobileNumber = getCellValueAsString(row.getCell(headerMapping.get("mobile_number")));
        String slipBoxName = getCellValueAsString(row.getCell(headerMapping.get("slip_box_name")));
        String slipBoxId = getCellValueAsString(row.getCell(headerMapping.get("slip_box_id")));

        // Validate fields
        if (mobileNumber == null || mobileNumber.trim().isEmpty() || !mobileNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
            rowError.addError("mobile_number", "Missing, empty, or invalid format (must match ^\\+?[1-9]\\d{1,14}$)");
        }
//        if (mobileNumber != null && mobileNumber.matches("^\\d+$") && Long.parseLong(mobileNumber) > Integer.MAX_VALUE) {
//            rowError.addError("mobile_number", "Mobile number exceeds integer range; ensure it is formatted as text in the file.");
//        }
        if (slipBoxName == null || slipBoxName.trim().isEmpty()) {
            rowError.addError("slip_box_name", "Missing or empty value");
        }
        if (slipBoxId == null || slipBoxId.trim().isEmpty()) {
            rowError.addError("slip_box_id", "Missing or empty value");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        SlipBoxEntity slipBox = new SlipBoxEntity();
        slipBox.setMobileNumber(mobileNumber);
        slipBox.setSlipBoxName(slipBoxName);
        slipBox.setSlipBoxId(slipBoxId);
        slipBox.setAccountId(0L);
        slipBox.setElectionId(0L);
        return slipBox;
    }

    private SlipBoxEntity processCpanelCsvRow(String[] fields, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String mobileNumber = fields.length > headerMapping.get("mobile_number") ? fields[headerMapping.get("mobile_number")].trim() : null;
        String slipBoxName = fields.length > headerMapping.get("slip_box_name") ? fields[headerMapping.get("slip_box_name")].trim() : null;
        String slipBoxId = fields.length > headerMapping.get("slip_box_id") ? fields[headerMapping.get("slip_box_id")].trim() : null;

        // Validate fields
        if (mobileNumber == null || mobileNumber.isEmpty() || !mobileNumber.matches("^\\+?[1-9]\\d{1,14}$")) {
            rowError.addError("mobile_number", "Missing, empty, or invalid format (must match ^\\+?[1-9]\\d{1,14}$)");
        }
//        if (mobileNumber != null && mobileNumber.matches("^\\d+$") && Long.parseLong(mobileNumber) > Integer.MAX_VALUE) {
//            rowError.addError("mobile_number", "Mobile number exceeds integer range; ensure it is formatted as text in the file.");
//        }
        if (slipBoxName == null || slipBoxName.isEmpty()) {
            rowError.addError("slip_box_name", "Missing or empty value");
        }
        if (slipBoxId == null || slipBoxId.isEmpty()) {
            rowError.addError("slip_box_id", "Missing or empty value");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        SlipBoxEntity slipBox = new SlipBoxEntity();
        slipBox.setMobileNumber(mobileNumber);
        slipBox.setSlipBoxName(slipBoxName);
        slipBox.setSlipBoxId(slipBoxId);
        slipBox.setAccountId(0L);
        slipBox.setElectionId(0L);
        return slipBox;
    }

    private void saveBatch(List<SlipBoxEntity> slipBoxes, SectionBulkUploadEntity bulkUploadEntity) {
        try {
            // Save to PostgreSQL first
            List<SlipBoxEntity> savedEntities = slipBoxRepository.saveAll(slipBoxes);
            log.debug("Saved {} slip boxes to PostgreSQL", savedEntities.size());
            
            try {
                // Save to MongoDB
                List<SlipBoxMongo> mongoEntities = savedEntities.stream()
                        .map(SlipBoxMongo::new)
                        .collect(Collectors.toList());
                slipBoxMongoRepository.saveAll(mongoEntities);
                log.debug("Synced {} slip boxes to MongoDB", mongoEntities.size());
            } catch (Exception mongoEx) {
                log.error("Failed to sync {} slip boxes to MongoDB: {}", savedEntities.size(), mongoEx.getMessage());
                // Continue as PostgreSQL save was successful
            }
            
        } catch (Exception e) {
            log.error("Error saving batch of {} slip boxes: {}", slipBoxes.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + slipBoxes.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - slipBoxes.size());
        }
    }

//    private String getCellValueAsString(Cell cell) {
//        if (cell == null) return null;
//        switch (cell.getCellType()) {
//            case STRING: return cell.getStringCellValue().trim();
//            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
//            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
//            case FORMULA: return cell.getCellFormula();
//            default: return null;
//        }
//    }
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: 
                // Use DataFormatter to preserve the exact string representation
                return new DataFormatter().formatCellValue(cell).trim();
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return null;
        }
    }
}