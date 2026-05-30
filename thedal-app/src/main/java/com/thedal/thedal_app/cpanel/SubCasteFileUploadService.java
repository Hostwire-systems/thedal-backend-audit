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
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SubCasteFileUploadService {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;

    @Autowired
    private SubCasteRepository subCasteRepository;

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
    public void processCpanelSubCasteExcelFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            bulkUploadEntity.setTotalRecords((long) (sheet.getPhysicalNumberOfRows() - 1));
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header
            processCpanelSubCasteFile(rowIterator, headerMapping, bulkUploadEntity);
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
    public void processCpanelSubCasteCsvFile(SectionBulkUploadEntity bulkUploadEntity, String fileUrl) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processCpanelSubCasteFile(processingBr.lines().iterator(), headerMapping, bulkUploadEntity);
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
    public void processCpanelSubCasteFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                          SectionBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, SubCasteEntity> subCasteMap = new HashMap<>(); // Key: subCasteName_casteId_religionId
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;
        Long accountId = 0L;
        Long electionId = 0L;

        // Step 1: Parse the file and collect unique sub-castes
        while (rowIterator.hasNext()) {
            totalRecords++;
            Object rowObj = rowIterator.next();
            SectionBulkUploadEntity.RowError rowError = new SectionBulkUploadEntity.RowError(totalRecords);

            try {
                SubCasteEntity subCaste = null;
                if (rowObj instanceof Row) {
                    subCaste = processCpanelExcelRow((Row) rowObj, headerMapping, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    subCaste = processCpanelCsvRow(fields, headerMapping, rowError);
                }

                if (subCaste != null) {
                    String uniqueKey = subCaste.getSubCasteName() + "_" + subCaste.getCaste().getId() + "_" + subCaste.getReligion().getId();
                    if (subCasteMap.containsKey(uniqueKey)) {
                        log.debug("Overriding duplicate sub-caste in file at row {}: sub_caste_name={}, caste_id={}, religion_id={}",
                                totalRecords, subCaste.getSubCasteName(), subCaste.getCaste().getId(), subCaste.getReligion().getId());
                    }
                    subCasteMap.put(uniqueKey, subCaste);
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

        if (!subCasteMap.isEmpty()) {
            List<SubCasteEntity> subCastesToSave = new ArrayList<>(subCasteMap.values());

            // Step 2: Check for existing sub-castes in the database
            List<SubCasteEntity> existingSubCastes = subCasteRepository.findBySubCasteNameInAndCasteIdInAndReligionIdInAndAccountId(
                    subCastesToSave.stream().map(SubCasteEntity::getSubCasteName).distinct().toList(),
                    subCastesToSave.stream().map(subCaste -> subCaste.getCaste().getId()).distinct().toList(),
                    subCastesToSave.stream().map(subCaste -> subCaste.getReligion().getId()).distinct().toList(),
                    accountId
            );

            // Step 3: Filter out duplicates (same subCasteName within same casteId and religionId)
            Set<String> existingKeys = existingSubCastes.stream()
                    .map(subCaste -> subCaste.getSubCasteName() + "_" + subCaste.getCaste().getId() + "_" + subCaste.getReligion().getId())
                    .collect(Collectors.toSet());
            List<SubCasteEntity> newSubCastes = subCastesToSave.stream()
                    .filter(subCaste -> !existingKeys.contains(subCaste.getSubCasteName() + "_" + subCaste.getCaste().getId() + "_" + subCaste.getReligion().getId()))
                    .collect(Collectors.toList());

            if (!newSubCastes.isEmpty()) {
                // Step 4: Assign orderIndex for new sub-castes, grouped by casteId
                Map<Long, List<SubCasteEntity>> subCastesByCaste = newSubCastes.stream()
                        .collect(Collectors.groupingBy(subCaste -> subCaste.getCaste().getId()));
                for (Long casteId : subCastesByCaste.keySet()) {
                    Integer maxOrderIndex = subCasteRepository.findMaxOrderIndexByCasteIdAndElectionId(casteId, electionId);
                    int nextOrderIndex = (maxOrderIndex == null || maxOrderIndex == -1) ? 0 : maxOrderIndex + 1;
                    for (SubCasteEntity subCaste : subCastesByCaste.get(casteId)) {
                        subCaste.setOrderIndex(nextOrderIndex++);
                    }
                }

                saveBatch(newSubCastes, bulkUploadEntity);
                totalSuccessRecords = newSubCastes.size();
            } else {
                log.info("No new sub-castes to save; all provided subCasteName-casteId-religionId combinations already exist.");
            }

            totalFailedRecords += (subCastesToSave.size() - totalSuccessRecords); // Duplicates count as "failed"
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

    private SubCasteEntity processCpanelExcelRow(Row row, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String subCasteName = getCellValueAsString(row.getCell(headerMapping.get("sub_caste_name")));
        
        // Support both ID and name-based lookups
        Long casteId = getCellValueAsLong(row.getCell(headerMapping.get("caste_id")));
        String casteName = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
        Long religionId = getCellValueAsLong(row.getCell(headerMapping.get("religion_id")));
        String religionName = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));

        if (subCasteName == null || subCasteName.trim().isEmpty()) {
            rowError.addError("sub_caste_name", "Missing or invalid value");
        }
        
        // Validate that either ID or name is provided for caste and religion
        if (casteId == null && (casteName == null || casteName.trim().isEmpty())) {
            rowError.addError("caste", "Either caste_id or caste_name must be provided");
        }
        if (religionId == null && (religionName == null || religionName.trim().isEmpty())) {
            rowError.addError("religion", "Either religion_id or religion_name must be provided");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        // Resolve or create Religion (global data for CPanel)
        ReligionEntity religionEntity = resolveOrCreateCpanelReligion(religionId, religionName, rowError);
        if (religionEntity == null) {
            return null;
        }

        // Resolve or create Caste (global data for CPanel)
        CasteEntity casteEntity = resolveOrCreateCpanelCaste(casteId, casteName, religionEntity, rowError);
        if (casteEntity == null) {
            return null;
        }

        SubCasteEntity subCaste = new SubCasteEntity();
        subCaste.setSubCasteName(subCasteName);
        subCaste.setCaste(casteEntity);
        subCaste.setReligion(religionEntity);
        subCaste.setAccountId(0L);
        subCaste.setElectionId(0L);
        return subCaste;
    }

    private SubCasteEntity processCpanelCsvRow(String[] fields, Map<String, Integer> headerMapping, SectionBulkUploadEntity.RowError rowError) {
        String subCasteName = fields.length > headerMapping.get("sub_caste_name") ? fields[headerMapping.get("sub_caste_name")].trim() : null;
        
        // Support both ID and name-based lookups
        String casteIdStr = headerMapping.containsKey("caste_id") && fields.length > headerMapping.get("caste_id") ? 
                fields[headerMapping.get("caste_id")].trim() : null;
        String casteName = headerMapping.containsKey("caste_name") && fields.length > headerMapping.get("caste_name") ? 
                fields[headerMapping.get("caste_name")].trim() : null;
        String religionIdStr = headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id") ? 
                fields[headerMapping.get("religion_id")].trim() : null;
        String religionName = headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name") ? 
                fields[headerMapping.get("religion_name")].trim() : null;
        
        Long casteId = (casteIdStr != null && !casteIdStr.isEmpty()) ? Long.parseLong(casteIdStr) : null;
        Long religionId = (religionIdStr != null && !religionIdStr.isEmpty()) ? Long.parseLong(religionIdStr) : null;

        if (subCasteName == null || subCasteName.isEmpty()) {
            rowError.addError("sub_caste_name", "Missing or invalid value");
        }
        
        // Validate that either ID or name is provided for caste and religion
        if (casteId == null && (casteName == null || casteName.isEmpty())) {
            rowError.addError("caste", "Either caste_id or caste_name must be provided");
        }
        if (religionId == null && (religionName == null || religionName.isEmpty())) {
            rowError.addError("religion", "Either religion_id or religion_name must be provided");
        }

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        // Resolve or create Religion (global data for CPanel)
        ReligionEntity religionEntity = resolveOrCreateCpanelReligion(religionId, religionName, rowError);
        if (religionEntity == null) {
            return null;
        }

        // Resolve or create Caste (global data for CPanel)
        CasteEntity casteEntity = resolveOrCreateCpanelCaste(casteId, casteName, religionEntity, rowError);
        if (casteEntity == null) {
            return null;
        }

        SubCasteEntity subCaste = new SubCasteEntity();
        subCaste.setSubCasteName(subCasteName);
        subCaste.setCaste(casteEntity);
        subCaste.setReligion(religionEntity);
        subCaste.setAccountId(0L);
        subCaste.setElectionId(0L);
        return subCaste;
    }

    private void saveBatch(List<SubCasteEntity> subCastes, SectionBulkUploadEntity bulkUploadEntity) {
        try {
            subCasteRepository.saveAll(subCastes);
            log.debug("Saved {} sub-castes: {}", subCastes.size(), subCastes);
        } catch (Exception e) {
            log.error("Error saving batch of {} sub-castes: {}", subCastes.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + subCastes.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - subCastes.size());
        }
    }

    /**
     * Resolves Religion by ID or name for CPanel (global data), creates new one if not found
     */
    private ReligionEntity resolveOrCreateCpanelReligion(Long religionId, String religionName, 
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

        // Resolve or create by name
        if (religionName != null && !religionName.trim().isEmpty()) {
            // Look for existing religion by name in global data
            ReligionEntity religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(
                    religionName.trim(), 0L, 0L).orElse(null);
            
            if (religion != null) {
                return religion;
            }

            // Create new religion in global data if not found
            log.info("Creating new global religion: {}", religionName);
            
            religion = new ReligionEntity();
            religion.setReligionName(religionName.trim());
            religion.setAccountId(0L);
            religion.setElectionId(0L);
            
            // Set order index
            Integer maxOrderIndex = religionRepository.findMaxOrderIndexByElectionId(0L);
            religion.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
            
            return religionRepository.save(religion);
        }

        rowError.addError("religion", "Could not resolve religion by ID or name");
        return null;
    }

    /**
     * Resolves Caste by ID or name for CPanel (global data), creates new one if not found
     */
    private CasteEntity resolveOrCreateCpanelCaste(Long casteId, String casteName, 
                                                 ReligionEntity religion, SectionBulkUploadEntity.RowError rowError) {
        // Try to resolve by ID first
        if (casteId != null) {
            CasteEntity caste = casteRepository.findByIdAndAccountIdAndElectionId(casteId, 0L, 0L)
                    .orElse(null);
            if (caste != null) {
                // Verify the caste belongs to the correct religion
                if (!caste.getReligion().getId().equals(religion.getId())) {
                    rowError.addError("caste_id", "Caste does not belong to the specified religion");
                    return null;
                }
                return caste;
            }
            // If ID provided but not found, log warning but continue with name lookup
            log.warn("Caste ID {} not found in global data, attempting lookup by name: {}", casteId, casteName);
        }

        // Resolve or create by name
        if (casteName != null && !casteName.trim().isEmpty()) {
            // Look for existing caste by name and religion in global data
            CasteEntity caste = casteRepository.findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
                    casteName.trim(), religion.getId(), 0L, 0L).orElse(null);
            
            if (caste != null) {
                return caste;
            }

            // Create new caste in global data if not found
            log.info("Creating new global caste: {} for religion: {}", casteName, religion.getReligionName());
            
            caste = new CasteEntity();
            caste.setCasteName(casteName.trim());
            caste.setReligion(religion);
            caste.setAccountId(0L);
            caste.setElectionId(0L);
            
            // Set order index within religion
            Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
                    religion.getId(), 0L);
            caste.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
            
            return casteRepository.save(caste);
        }

        rowError.addError("caste", "Could not resolve caste by ID or name");
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
