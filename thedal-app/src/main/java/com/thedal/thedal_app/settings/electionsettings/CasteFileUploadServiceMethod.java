package com.thedal.thedal_app.settings.electionsettings;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.election.ElectionEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import java.util.stream.Collectors;

@Slf4j
@Service
public class CasteFileUploadServiceMethod {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private CasteRepository casteRepository;

    @Autowired
    private ReligionRepository religionRepository;

    @Autowired
    private CasteMongoRepository casteMongoRepository;

    public Map<String, Integer> buildHeaderMapping(Row headerRow) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (Cell cell : headerRow) {
            String normalizedHeader = normalizeHeader(cell.getStringCellValue());
            headerMapping.put(normalizedHeader, cell.getColumnIndex());
        }
        return headerMapping;
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
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
    public Map<String, Object> processCasteExcelFile(AccountEntity account, String fileUrl, ElectionEntity election,
                                                    Map<String, Integer> headerMapping) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<CasteEntity> savedCastes = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(headerMapping, rowErrors);

            int totalRows = sheet.getPhysicalNumberOfRows();
            totalRecords = totalRows - 1; // Exclude header row
            log.info("Starting Excel processing for {} rows", totalRecords);

            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row
            Map<String, Object> processResult = processCasteFile(rowIterator, headerMapping, account, election,
                    rowErrors, savedCastes);
            totalSuccessRecords = (long) processResult.get("totalSuccessRecords");
            totalFailedRecords = (long) processResult.get("totalFailedRecords");

        } catch (Exception e) {
            log.error("Excel processing failed: {}", e.getMessage(), e);
            rowErrors.add(createErrorMap(0, "general", "Excel processing failed: " + e.getMessage()));
            totalFailedRecords = totalRecords;
        }

        result.put("totalRecords", totalRecords);
        result.put("totalProcessedRecords", totalSuccessRecords + totalFailedRecords);
        result.put("totalSuccessRecords", totalSuccessRecords);
        result.put("totalFailedRecords", totalFailedRecords);
        result.put("savedCastes", savedCastes);
        result.put("rowErrors", rowErrors);
        result.put("totalTimeTaken", System.currentTimeMillis() - startTime);

        log.info("Processed {} records: {} successful, {} failed in {} ms",
                totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
        return result;
    }

    @Transactional
    public Map<String, Object> processCasteCsvFile(AccountEntity account, String fileUrl, ElectionEntity election,
                                                  Map<String, Integer> headerMapping) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<CasteEntity> savedCastes = new ArrayList<>();
        List<Map<String, Object>> rowErrors = new ArrayList<>();
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            validateHeaders(headerMapping, rowErrors);

            totalRecords = br.lines().count();
            log.info("Starting CSV processing for {} rows", totalRecords);

            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                Map<String, Object> processResult = processCasteFile(processingBr.lines().iterator(), headerMapping,
                        account, election, rowErrors, savedCastes);
                totalSuccessRecords = (long) processResult.get("totalSuccessRecords");
                totalFailedRecords = (long) processResult.get("totalFailedRecords");
            }

        } catch (Exception e) {
            log.error("CSV processing failed: {}", e.getMessage(), e);
            rowErrors.add(createErrorMap(0, "general", "CSV processing failed: " + e.getMessage()));
            totalFailedRecords = totalRecords;
        }

        result.put("totalRecords", totalRecords);
        result.put("totalProcessedRecords", totalSuccessRecords + totalFailedRecords);
        result.put("totalSuccessRecords", totalSuccessRecords);
        result.put("totalFailedRecords", totalFailedRecords);
        result.put("savedCastes", savedCastes);
        result.put("rowErrors", rowErrors);
        result.put("totalTimeTaken", System.currentTimeMillis() - startTime);

        log.info("Processed {} records: {} successful, {} failed in {} ms",
                totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
        return result;
    }

    private void validateHeaders(Map<String, Integer> headerMapping, List<Map<String, Object>> rowErrors) {
        String[] optionalHeaders = {"order_index"};
        for (String header : optionalHeaders) {
            if (!headerMapping.containsKey(header)) {
                rowErrors.add(createErrorMap(0, header, "Missing optional header: " + header));
            }
        }
    }

    private Map<String, Object> processCasteFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                                AccountEntity account, ElectionEntity election,
                                                List<Map<String, Object>> rowErrors, List<CasteEntity> savedCastes) {
        long startTime = System.currentTimeMillis();
        Map<String, CasteEntity> casteMap = new HashMap<>(); // Map to track latest caste_name + religion_id
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        // Step 1: Process the file and keep only the last occurrence of each caste_name + religion_id
        while (rowIterator.hasNext()) {
            Object rowObj = rowIterator.next();

            // Skip empty rows early
            if (isEmptyRow(rowObj, headerMapping)) {
                continue;
            }

            totalRecords++;
            Map<String, Object> rowError = new HashMap<>();
            rowError.put("rowNumber", totalRecords);
            rowError.put("errors", new HashMap<String, String>());

            try {
                CasteEntity caste = null;
                if (rowObj instanceof Row) {
                    Row row = (Row) rowObj;
                    caste = processExcelRow(row, headerMapping, account, election, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    caste = processCsvRow(fields, headerMapping, account, election, rowError);
                }

                if (caste != null) {
                    String key = caste.getCasteName().trim() + "_" + caste.getReligion().getId();
                    casteMap.put(key, caste);
                } else {
                    rowErrors.add(rowError);
                    totalFailedRecords++;
                }

            } catch (Exception e) {
                log.error("Error processing row {}: {}", totalRecords, e.getMessage());
                ((Map<String, String>) rowError.get("errors")).put("general", "Unexpected error: " + e.getMessage());
                rowErrors.add(rowError);
                totalFailedRecords++;
            }
        }

        // Step 2: Process existing and new records
        if (!casteMap.isEmpty()) {
            List<String> casteNamesToProcess = casteMap.values().stream()
                    .map(CasteEntity::getCasteName)
                    .collect(Collectors.toList());

            // Fetch existing records from the database
            List<CasteEntity> existingRecords = casteRepository.findByElectionIdAndAccountIdAndCasteNameIn(
                    election.getId(), account.getId(), casteNamesToProcess);
            Map<String, CasteEntity> existingRecordsMap = existingRecords.stream()
                    .collect(Collectors.toMap(c -> c.getCasteName() + "_" + c.getReligion().getId(), c -> c));

            List<CasteEntity> castesToSave = new ArrayList<>();
            List<CasteEntity> castesToUpdate = new ArrayList<>();

            // Step 3: Separate records into updates and inserts
            for (CasteEntity newCaste : casteMap.values()) {
                String key = newCaste.getCasteName() + "_" + newCaste.getReligion().getId();
                if (existingRecordsMap.containsKey(key)) {
                    // Update existing record
                    CasteEntity existingCaste = existingRecordsMap.get(key);
                    existingCaste.setCasteName(newCaste.getCasteName());
                    existingCaste.setReligion(newCaste.getReligion());
                    existingCaste.setOrderIndex(newCaste.getOrderIndex());
                    existingCaste.setUpdatedAt(LocalDateTime.now());
                    castesToUpdate.add(existingCaste);
                } else {
                    // Set order index if not provided
                    if (newCaste.getOrderIndex() == null) {
                        Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
                                newCaste.getReligion().getId(), election.getId());
                        newCaste.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
                    }
                    castesToSave.add(newCaste);
                }
            }

            // Step 4: Save new records and update existing ones
            if (!castesToSave.isEmpty()) {
                totalSuccessRecords += saveBatch(castesToSave, rowErrors, savedCastes, totalRecords);
            }
            if (!castesToUpdate.isEmpty()) {
                totalSuccessRecords += saveBatch(castesToUpdate, rowErrors, savedCastes, totalRecords);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalSuccessRecords", totalSuccessRecords);
        result.put("totalFailedRecords", totalFailedRecords);
        return result;
    }

    private boolean isEmptyRow(Object rowObj, Map<String, Integer> headerMapping) {
        if (rowObj instanceof Row) {
            Row row = (Row) rowObj;
            String casteName = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
            
            // Check for religion by ID or name
            String religionId = headerMapping.containsKey("religion_id") ? 
                    getCellValueAsString(row.getCell(headerMapping.get("religion_id"))) : null;
            String religionName = headerMapping.containsKey("religion_name") ?
                    getCellValueAsString(row.getCell(headerMapping.get("religion_name"))) : null;
            
            return (casteName == null || casteName.trim().isEmpty()) &&
                   (religionId == null || religionId.trim().isEmpty()) &&
                   (religionName == null || religionName.trim().isEmpty());
        } else if (rowObj instanceof String) {
            String[] fields = ((String) rowObj).split(",");
            String casteName = fields.length > headerMapping.get("caste_name") ?
                    fields[headerMapping.get("caste_name")].trim() : null;
            
            // Check for religion by ID or name
            String religionId = headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id") ?
                    fields[headerMapping.get("religion_id")].trim() : null;
            String religionName = headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name") ?
                    fields[headerMapping.get("religion_name")].trim() : null;
            
            return (casteName == null || casteName.isEmpty()) &&
                   (religionId == null || religionId.isEmpty()) &&
                   (religionName == null || religionName.isEmpty());
        }
        return true;
    }

    private CasteEntity processExcelRow(Row row, Map<String, Integer> headerMapping,
                                       AccountEntity account, ElectionEntity election, Map<String, Object> rowError) {
        Map<String, String> errors = (Map<String, String>) rowError.get("errors");
        String casteName = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
        String religionIdStr = getCellValueAsString(row.getCell(headerMapping.get("religion_id")));
        String religionName = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));

        if (casteName == null || casteName.trim().isEmpty())
            errors.put("caste_name", "Missing or invalid value");
            
        // Validate that either religion_id or religion_name is provided
        if ((religionIdStr == null || religionIdStr.trim().isEmpty()) && 
            (religionName == null || religionName.trim().isEmpty())) {
            errors.put("religion", "Either religion_id or religion_name must be provided");
            return null;
        }

        // Resolve religion by ID or name
        ReligionEntity religion = resolveReligion(religionIdStr, religionName, account, election, errors);
        if (religion == null) {
            return null;
        }

        if (!errors.isEmpty()) {
            return null;
        }

        CasteEntity caste = new CasteEntity();
        caste.setAccountId(account.getId());
        caste.setElectionId(election.getId());
        caste.setCasteName(casteName.trim());
        caste.setReligion(religion);
        caste.setOrderIndex(headerMapping.containsKey("order_index") ?
                getCellValueAsInteger(row.getCell(headerMapping.get("order_index"))) : null);
        caste.setCreatedAt(LocalDateTime.now());
        caste.setUpdatedAt(LocalDateTime.now());

        return caste;
    }

    private CasteEntity processCsvRow(String[] fields, Map<String, Integer> headerMapping,
                                     AccountEntity account, ElectionEntity election, Map<String, Object> rowError) {
        Map<String, String> errors = (Map<String, String>) rowError.get("errors");
        String casteName = fields.length > headerMapping.get("caste_name") ?
                fields[headerMapping.get("caste_name")].trim() : null;
        String religionIdStr = headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id") ?
                fields[headerMapping.get("religion_id")].trim() : null;
        String religionName = headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name") ?
                fields[headerMapping.get("religion_name")].trim() : null;

        if (casteName == null || casteName.isEmpty())
            errors.put("caste_name", "Missing or invalid value");
            
        // Validate that either religion_id or religion_name is provided
        if ((religionIdStr == null || religionIdStr.isEmpty()) && 
            (religionName == null || religionName.isEmpty())) {
            errors.put("religion", "Either religion_id or religion_name must be provided");
            return null;
        }

        // Resolve religion by ID or name
        ReligionEntity religion = resolveReligion(religionIdStr, religionName, account, election, errors);
        if (religion == null) {
            return null;
        }

        if (!errors.isEmpty()) {
            return null;
        }

        CasteEntity caste = new CasteEntity();
        caste.setAccountId(account.getId());
        caste.setElectionId(election.getId());
        caste.setCasteName(casteName);
        caste.setReligion(religion);
        caste.setCreatedAt(LocalDateTime.now());
        caste.setUpdatedAt(LocalDateTime.now());

        try {
            if (headerMapping.containsKey("order_index") && fields.length > headerMapping.get("order_index")) {
                String orderIndexStr = fields[headerMapping.get("order_index")].trim();
                caste.setOrderIndex(orderIndexStr.isEmpty() ? null : Integer.parseInt(orderIndexStr));
            }
        } catch (NumberFormatException e) {
            errors.put("order_index", "Invalid number format");
            return null;
        }

        return caste;
    }

    private long saveBatch(List<CasteEntity> castes, List<Map<String, Object>> rowErrors,
                          List<CasteEntity> savedCastes, long currentRow) {
        long successCount = 0;
        try {
            // Save to relational database
            List<CasteEntity> saved = casteRepository.saveAll(castes);
            savedCastes.addAll(saved);

            // Save to MongoDB
            List<CasteMongo> mongoCastes = saved.stream()
                    .map(CasteMongo::new)
                    .collect(Collectors.toList());
            casteMongoRepository.saveAll(mongoCastes);

            successCount = saved.size();
            log.debug("Saved {} castes to SQL and MongoDB: {}", successCount, saved);
        } catch (Exception e) {
            log.error("Error saving batch of {} castes: {}", castes.size(), e.getMessage());
            rowErrors.add(createErrorMap(currentRow, "general", "Failed to save batch: " + e.getMessage()));
        }
        return successCount;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
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
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return (int) cell.getNumericCellValue();
    }

    private Map<String, Object> createErrorMap(long rowNumber, String field, String error) {
        Map<String, Object> errorMap = new HashMap<>();
        errorMap.put("rowNumber", rowNumber);
        Map<String, String> errors = new HashMap<>();
        errors.put(field, error);
        errorMap.put("errors", errors);
        return errorMap;
    }
    
    /**
     * Resolves Religion by ID or name for Election Settings (account-specific data)
     */
    private ReligionEntity resolveReligion(String religionIdStr, String religionName, 
                                         AccountEntity account, ElectionEntity election, 
                                         Map<String, String> errors) {
        // Try to resolve by ID first
        if (religionIdStr != null && !religionIdStr.trim().isEmpty()) {
            try {
                Long religionId = Long.parseLong(religionIdStr.trim());
                ReligionEntity religion = religionRepository.findByIdAndElectionIdAndAccountId(
                        religionId, election.getId(), account.getId()).orElse(null);
                if (religion != null) {
                    return religion;
                }
                // If ID provided but not found, log warning but continue with name lookup
                System.out.println("Religion ID " + religionId + " not found for account " + account.getId() + 
                                 " and election " + election.getId() + ", attempting lookup by name: " + religionName);
            } catch (NumberFormatException e) {
                errors.put("religion_id", "Invalid number format");
                return null;
            }
        }

        // Resolve by name
        if (religionName != null && !religionName.trim().isEmpty()) {
            ReligionEntity religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(
                    religionName.trim(), account.getId(), election.getId()).orElse(null);
            
            if (religion != null) {
                return religion;
            }
            
            errors.put("religion_name", "Religion '" + religionName + "' not found");
            return null;
        }

        errors.put("religion", "Could not resolve religion by ID or name");
        return null;
    }
}