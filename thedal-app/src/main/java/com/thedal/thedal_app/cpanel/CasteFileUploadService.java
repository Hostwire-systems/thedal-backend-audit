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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CasteFileUploadService {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;

    @Autowired
    private CasteRepository casteRepository;

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
    public void processCpanelCasteExcelFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            bulkUploadEntity.setTotalRecords((long) (sheet.getPhysicalNumberOfRows() - 1));
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header
            processCpanelCasteFile(rowIterator, headerMapping, bulkUploadEntity);
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
    public void processCpanelCasteCsvFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processCpanelCasteFile(processingBr.lines().iterator(), headerMapping, bulkUploadEntity);
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
    public void processCpanelCasteFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                       SectionBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, CasteEntity> casteMap = new HashMap<>(); // Key: casteName_religionId
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
                CasteEntity caste = null;
                if (rowObj instanceof Row) {
                    caste = processCpanelExcelRow((Row) rowObj, headerMapping, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    caste = processCpanelCsvRow(fields, headerMapping, rowError);
                }

                if (caste != null) {
                    String uniqueKey = caste.getCasteName() + "_" + caste.getReligion().getId();
                    if (casteMap.containsKey(uniqueKey)) {
                        log.debug("Overriding duplicate caste at row {}: caste_name={}, religion_id={}", 
                                  totalRecords, caste.getCasteName(), caste.getReligion().getId());
                    }
                    casteMap.put(uniqueKey, caste);
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

        if (!casteMap.isEmpty()) {
            List<CasteEntity> castesToSave = new ArrayList<>(casteMap.values());
            // Find existing castes to avoid duplicates
            List<CasteEntity> existingCastes = casteRepository.findByCasteNameInAndReligionIdInAndAccountIdAndElectionId(
                    castesToSave.stream().map(CasteEntity::getCasteName).distinct().toList(),
                    castesToSave.stream().map(caste -> caste.getReligion().getId()).distinct().toList(),
                    accountId, electionId
            );

            // Filter out existing castes
            Set<String> existingKeys = existingCastes.stream()
                    .map(caste -> caste.getCasteName() + "_" + caste.getReligion().getId())
                    .collect(Collectors.toSet());
            List<CasteEntity> newCastes = castesToSave.stream()
                    .filter(caste -> !existingKeys.contains(caste.getCasteName() + "_" + caste.getReligion().getId()))
                    .collect(Collectors.toList());

            if (!newCastes.isEmpty()) {
                // Group by religionId to assign orderIndex
                Map<Long, List<CasteEntity>> castesByReligion = newCastes.stream()
                        .collect(Collectors.groupingBy(caste -> caste.getReligion().getId()));
                for (Long religionId : castesByReligion.keySet()) {
                    Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(religionId, electionId);
                    int nextOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
                    for (CasteEntity caste : castesByReligion.get(religionId)) {
                        caste.setOrderIndex(nextOrderIndex++);
                    }
                }

                saveBatch(newCastes, bulkUploadEntity);
                totalSuccessRecords = newCastes.size();
            } else {
                log.info("No new castes to save; all provided casteName-religionId pairs already exist.");
            }

            totalFailedRecords += (castesToSave.size() - totalSuccessRecords); // Duplicates count as "failed"
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

    private CasteEntity processCpanelExcelRow(Row row, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String casteName = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
        
        // Support both ID and name-based lookups for religion
        Long religionId = getCellValueAsLong(row.getCell(headerMapping.get("religion_id")));
        String religionName = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));

        if (casteName == null || casteName.trim().isEmpty()) {
            rowError.addError("caste_name", "Missing or invalid value");
        }
        
        // Validate that either ID or name is provided for religion
        if (religionId == null && (religionName == null || religionName.trim().isEmpty())) {
            rowError.addError("religion", "Either religion_id or religion_name must be provided");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        // Resolve Religion by ID or name
        ReligionEntity religionEntity = resolveReligion(religionId, religionName, rowError);
        if (religionEntity == null) {
            return null;
        }

        CasteEntity caste = new CasteEntity();
        caste.setCasteName(casteName);
        caste.setReligion(religionEntity);
        caste.setAccountId(0L);
        caste.setElectionId(0L);
        return caste;
    }

    private CasteEntity processCpanelCsvRow(String[] fields, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String casteName = fields.length > headerMapping.get("caste_name") ? fields[headerMapping.get("caste_name")].trim() : null;
        
        // Support both ID and name-based lookups for religion
        String religionIdStr = headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id") ? 
                fields[headerMapping.get("religion_id")].trim() : null;
        String religionName = headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name") ? 
                fields[headerMapping.get("religion_name")].trim() : null;
        
        Long religionId = (religionIdStr != null && !religionIdStr.isEmpty()) ? Long.parseLong(religionIdStr) : null;

        if (casteName == null || casteName.isEmpty()) {
            rowError.addError("caste_name", "Missing or invalid value");
        }
        
        // Validate that either ID or name is provided for religion
        if (religionId == null && (religionName == null || religionName.isEmpty())) {
            rowError.addError("religion", "Either religion_id or religion_name must be provided");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        // Resolve Religion by ID or name
        ReligionEntity religionEntity = resolveReligion(religionId, religionName, rowError);
        if (religionEntity == null) {
            return null;
        }

        CasteEntity caste = new CasteEntity();
        caste.setCasteName(casteName);
        caste.setReligion(religionEntity);
        caste.setAccountId(0L);
        caste.setElectionId(0L);
        return caste;
    }

    private void saveBatch(List<CasteEntity> castes, SectionBulkUploadEntity bulkUploadEntity) {
        try {
            casteRepository.saveAll(castes);
            log.debug("Saved {} castes: {}", castes.size(), castes);
        } catch (Exception e) {
            log.error("Error saving batch of {} castes: {}", castes.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + castes.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - castes.size());
        }
    }

    /**
     * Resolves Religion by ID or name for CPanel (global data)
     */
    private ReligionEntity resolveReligion(Long religionId, String religionName, 
                                         SectionBulkUploadEntity.RowError rowError) {
        // Try to resolve by ID first
        if (religionId != null) {
            ReligionEntity religion = religionRepository.findByIdAndElectionIdAndAccountId(religionId, 0L, 0L)
                    .orElse(null);
            if (religion != null) {
                return religion;
            }
            // If ID provided but not found, log warning but continue with name lookup
            log.warn("Religion ID {} not found in global data, attempting lookup by name: {}", religionId, religionName);
        }

        // Resolve by name
        if (religionName != null && !religionName.trim().isEmpty()) {
            ReligionEntity religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(
                    religionName.trim(), 0L, 0L).orElse(null);
            
            if (religion != null) {
                return religion;
            }
            
            rowError.addError("religion_name", "Religion '" + religionName + "' not found");
            return null;
        }

        rowError.addError("religion", "Could not resolve religion by ID or name");
        return null;
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

    private Long getCellValueAsLong(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (long) cell.getNumericCellValue();
    }
}
