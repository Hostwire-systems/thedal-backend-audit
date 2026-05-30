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
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadRepository;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReligionFileUploadService {

	private static final int BATCH_SIZE = 500;

    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;

    @Autowired
    private ReligionRepository religionRepository;

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
    public void processCpanelReligionExcelFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            bulkUploadEntity.setTotalRecords((long) (sheet.getPhysicalNumberOfRows() - 1));
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header
            processCpanelReligionFile(rowIterator, headerMapping, bulkUploadEntity);
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
    public void processCpanelReligionCsvFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processCpanelReligionFile(processingBr.lines().iterator(), headerMapping, bulkUploadEntity);
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
    public void processCpanelReligionFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                          SectionBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, ReligionEntity> religionMap = new HashMap<>(); // Key: religionName (since accountId=0)
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
                ReligionEntity religion = null;
                if (rowObj instanceof Row) {
                    religion = processCpanelExcelRow((Row) rowObj, headerMapping, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    religion = processCpanelCsvRow(fields, headerMapping, rowError);
                }

                if (religion != null) {
                    String uniqueKey = religion.getReligionName(); // Unique by religionName for cPanel
                    if (religionMap.containsKey(uniqueKey)) {
                        log.debug("Overriding duplicate religion at row {}: religion_name={}", totalRecords, religion.getReligionName());
                    }
                    religionMap.put(uniqueKey, religion);
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

        if (!religionMap.isEmpty()) {
            List<ReligionEntity> religionsToSave = new ArrayList<>(religionMap.values());
            // Find existing religions to avoid duplicates
            List<ReligionEntity> existingRecords = religionRepository.findByAccountIdAndReligionNameIn(
                    accountId, religionsToSave.stream().map(ReligionEntity::getReligionName).distinct().toList()
            );

            // Filter out religions that already exist
            Set<String> existingNames = existingRecords.stream()
                    .map(ReligionEntity::getReligionName)
                    .collect(Collectors.toSet());
            List<ReligionEntity> newReligions = religionsToSave.stream()
                    .filter(religion -> !existingNames.contains(religion.getReligionName()))
                    .collect(Collectors.toList());

            if (!newReligions.isEmpty()) {
                // Assign orderIndex incrementally
                Integer maxOrderIndex = religionRepository.findMaxOrderIndexByElectionId(electionId);
                int nextOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
                for (ReligionEntity religion : newReligions) {
                    religion.setOrderIndex(nextOrderIndex++);
                }

                saveBatch(newReligions, bulkUploadEntity);
                totalSuccessRecords = newReligions.size();
            } else {
                log.info("No new religions to save; all provided names already exist.");
            }

            totalFailedRecords += (religionsToSave.size() - totalSuccessRecords); // Duplicates count as "failed"
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

    private ReligionEntity processCpanelExcelRow(Row row, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String religionName = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));

        if (religionName == null || religionName.trim().isEmpty()) {
            rowError.addError("religion_name", "Missing or invalid value");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        ReligionEntity religion = new ReligionEntity();
        religion.setAccountId(0L);
        religion.setElectionId(0L);
        religion.setReligionName(religionName);
        return religion;
    }

    private ReligionEntity processCpanelCsvRow(String[] fields, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String religionName = fields.length > headerMapping.get("religion_name") ? fields[headerMapping.get("religion_name")].trim() : null;

        if (religionName == null || religionName.isEmpty()) {
            rowError.addError("religion_name", "Missing or invalid value");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        ReligionEntity religion = new ReligionEntity();
        religion.setAccountId(0L);
        religion.setElectionId(0L);
        religion.setReligionName(religionName);
        return religion;
    }

    private void saveBatch(List<ReligionEntity> religions, SectionBulkUploadEntity bulkUploadEntity) {
        try {
            religionRepository.saveAll(religions);
            log.debug("Saved {} religions: {}", religions.size(), religions);
        } catch (Exception e) {
            log.error("Error saving batch of {} religions: {}", religions.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + religions.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - religions.size());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            case FORMULA: return cell.getCellFormula();
            default: return null;
        }
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (int) cell.getNumericCellValue();
    }
}