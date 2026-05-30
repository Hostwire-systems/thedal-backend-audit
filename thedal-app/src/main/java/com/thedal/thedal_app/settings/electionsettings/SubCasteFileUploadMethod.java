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
public class SubCasteFileUploadMethod {

    private static final int BATCH_SIZE = 500;

    @Autowired
    private SubCasteRepository subCasteRepository;

    @Autowired
    private CasteRepository casteRepository;

    @Autowired
    private ReligionRepository religionRepository;

    @Autowired
    private SubCasteMongoRepository subCasteMongoRepository;

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
    public Map<String, Object> processSubCasteExcelFile(AccountEntity account, String fileUrl, ElectionEntity election,
                                                       Map<String, Integer> headerMapping) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<SubCasteEntity> savedSubCastes = new ArrayList<>();
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
            Map<String, Object> processResult = processSubCasteFile(rowIterator, headerMapping, account, election,
                    rowErrors, savedSubCastes);
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
        result.put("savedSubCastes", savedSubCastes);
        result.put("rowErrors", rowErrors);
        result.put("totalTimeTaken", System.currentTimeMillis() - startTime);

        log.info("Processed {} records: {} successful, {} failed in {} ms",
                totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
        return result;
    }

    @Transactional
    public Map<String, Object> processSubCasteCsvFile(AccountEntity account, String fileUrl, ElectionEntity election,
                                                     Map<String, Integer> headerMapping) throws IOException {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();
        List<SubCasteEntity> savedSubCastes = new ArrayList<>();
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
                Map<String, Object> processResult = processSubCasteFile(processingBr.lines().iterator(), headerMapping,
                        account, election, rowErrors, savedSubCastes);
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
        result.put("savedSubCastes", savedSubCastes);
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

    private Map<String, Object> processSubCasteFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping,
                                                   AccountEntity account, ElectionEntity election,
                                                   List<Map<String, Object>> rowErrors, List<SubCasteEntity> savedSubCastes) {
        long startTime = System.currentTimeMillis();
        Map<String, SubCasteEntity> subCasteMap = new HashMap<>(); // Map to track latest sub_caste_name + caste_id + religion_id
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        // Step 1: Process the file and keep only the last occurrence of each sub_caste_name + caste_id + religion_id
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
                SubCasteEntity subCaste = null;
                if (rowObj instanceof Row) {
                    Row row = (Row) rowObj;
                    subCaste = processExcelRow(row, headerMapping, account, election, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    subCaste = processCsvRow(fields, headerMapping, account, election, rowError);
                }

                if (subCaste != null) {
                    String key = subCaste.getSubCasteName().trim() + "_" + subCaste.getCaste().getId() + "_" + subCaste.getReligion().getId();
                    subCasteMap.put(key, subCaste);
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
        if (!subCasteMap.isEmpty()) {
            List<String> subCasteNamesToProcess = subCasteMap.values().stream()
                    .map(SubCasteEntity::getSubCasteName)
                    .collect(Collectors.toList());

            // Fetch existing records from the database
            List<SubCasteEntity> existingRecords = subCasteRepository.findBySubCasteNameInAndCaste_ElectionIdAndCaste_Religion_AccountId(
                    subCasteNamesToProcess, election.getId(), account.getId());
            Map<String, SubCasteEntity> existingRecordsMap = existingRecords.stream()
                    .collect(Collectors.toMap(
                            c -> c.getSubCasteName() + "_" + c.getCaste().getId() + "_" + c.getReligion().getId(),
                            c -> c));

            List<SubCasteEntity> subCastesToSave = new ArrayList<>();
            List<SubCasteEntity> subCastesToUpdate = new ArrayList<>();

            // Step 3: Separate records into updates and inserts
            for (SubCasteEntity newSubCaste : subCasteMap.values()) {
                String key = newSubCaste.getSubCasteName() + "_" + newSubCaste.getCaste().getId() + "_" + newSubCaste.getReligion().getId();
                if (existingRecordsMap.containsKey(key)) {
                    // Update existing record
                    SubCasteEntity existingSubCaste = existingRecordsMap.get(key);
                    existingSubCaste.setSubCasteName(newSubCaste.getSubCasteName());
                    existingSubCaste.setCaste(newSubCaste.getCaste());
                    existingSubCaste.setReligion(newSubCaste.getReligion());
                    existingSubCaste.setOrderIndex(newSubCaste.getOrderIndex());
                    existingSubCaste.setUpdatedAt(LocalDateTime.now());
                    subCastesToUpdate.add(existingSubCaste);
                } else {
                    // Set order index if not provided
                    if (newSubCaste.getOrderIndex() == null) {
                        Integer maxOrderIndex = subCasteRepository.findMaxOrderIndexByCasteIdAndElectionId(
                                newSubCaste.getCaste().getId(), election.getId());
                        newSubCaste.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
                    }
                    subCastesToSave.add(newSubCaste);
                }
            }

            // Step 4: Save new records and update existing ones
            if (!subCastesToSave.isEmpty()) {
                totalSuccessRecords += saveBatch(subCastesToSave, rowErrors, savedSubCastes, totalRecords);
            }
            if (!subCastesToUpdate.isEmpty()) {
                totalSuccessRecords += saveBatch(subCastesToUpdate, rowErrors, savedSubCastes, totalRecords);
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
            String subCasteName = getCellValueAsString(row.getCell(headerMapping.get("sub_caste_name")));
            
            // Support both ID and name-based columns for caste
            String casteValue = null;
            if (headerMapping.containsKey("caste_id")) {
                casteValue = getCellValueAsString(row.getCell(headerMapping.get("caste_id")));
            } else if (headerMapping.containsKey("caste_name")) {
                casteValue = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
            }
            
            // Support both ID and name-based columns for religion
            String religionValue = null;
            if (headerMapping.containsKey("religion_id")) {
                religionValue = getCellValueAsString(row.getCell(headerMapping.get("religion_id")));
            } else if (headerMapping.containsKey("religion_name")) {
                religionValue = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));
            }
            
            return (subCasteName == null || subCasteName.trim().isEmpty()) &&
                   (casteValue == null || casteValue.trim().isEmpty()) &&
                   (religionValue == null || religionValue.trim().isEmpty());
        } else if (rowObj instanceof String) {
            String[] fields = ((String) rowObj).split(",");
            String subCasteName = fields.length > headerMapping.get("sub_caste_name") ?
                    fields[headerMapping.get("sub_caste_name")].trim() : null;
            
            // Support both ID and name-based columns for caste
            String casteValue = null;
            if (headerMapping.containsKey("caste_id") && fields.length > headerMapping.get("caste_id")) {
                casteValue = fields[headerMapping.get("caste_id")].trim();
            } else if (headerMapping.containsKey("caste_name") && fields.length > headerMapping.get("caste_name")) {
                casteValue = fields[headerMapping.get("caste_name")].trim();
            }
            
            // Support both ID and name-based columns for religion
            String religionValue = null;
            if (headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id")) {
                religionValue = fields[headerMapping.get("religion_id")].trim();
            } else if (headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name")) {
                religionValue = fields[headerMapping.get("religion_name")].trim();
            }
            
            return (subCasteName == null || subCasteName.isEmpty()) &&
                   (casteValue == null || casteValue.isEmpty()) &&
                   (religionValue == null || religionValue.isEmpty());
        }
        return true;
    }

    private SubCasteEntity processExcelRow(Row row, Map<String, Integer> headerMapping,
                                          AccountEntity account, ElectionEntity election, Map<String, Object> rowError) {
        Map<String, String> errors = (Map<String, String>) rowError.get("errors");
        String subCasteName = getCellValueAsString(row.getCell(headerMapping.get("sub_caste_name")));
        
        // Support both ID and name-based lookups
        String casteIdStr = getCellValueAsString(row.getCell(headerMapping.get("caste_id")));
        String casteName = getCellValueAsString(row.getCell(headerMapping.get("caste_name")));
        String religionIdStr = getCellValueAsString(row.getCell(headerMapping.get("religion_id")));
        String religionName = getCellValueAsString(row.getCell(headerMapping.get("religion_name")));

        if (subCasteName == null || subCasteName.trim().isEmpty())
            errors.put("sub_caste_name", "Missing or invalid value");

        // Validate that either ID or name is provided for caste and religion
        if ((casteIdStr == null || casteIdStr.trim().isEmpty()) && (casteName == null || casteName.trim().isEmpty()))
            errors.put("caste", "Either caste_id or caste_name must be provided");
        if ((religionIdStr == null || religionIdStr.trim().isEmpty()) && (religionName == null || religionName.trim().isEmpty()))
            errors.put("religion", "Either religion_id or religion_name must be provided");

        if (!errors.isEmpty()) {
            return null;
        }

        // Resolve or create Religion
        ReligionEntity religion = resolveOrCreateReligion(religionIdStr, religionName, account, election, errors);
        if (religion == null) {
            return null;
        }

        // Resolve or create Caste
        CasteEntity caste = resolveOrCreateCaste(casteIdStr, casteName, religion, account, election, errors);
        if (caste == null) {
            return null;
        }

        if (!errors.isEmpty()) {
            return null;
        }

        SubCasteEntity subCaste = new SubCasteEntity();
        subCaste.setAccountId(account.getId());
        subCaste.setElectionId(election.getId());
        subCaste.setSubCasteName(subCasteName.trim());
        subCaste.setCaste(caste);
        subCaste.setReligion(religion);
        subCaste.setOrderIndex(headerMapping.containsKey("order_index") ?
                getCellValueAsInteger(row.getCell(headerMapping.get("order_index"))) : null);
        subCaste.setCreatedAt(LocalDateTime.now());
        subCaste.setUpdatedAt(LocalDateTime.now());

        return subCaste;
    }

    private SubCasteEntity processCsvRow(String[] fields, Map<String, Integer> headerMapping,
                                        AccountEntity account, ElectionEntity election, Map<String, Object> rowError) {
        Map<String, String> errors = (Map<String, String>) rowError.get("errors");
        String subCasteName = fields.length > headerMapping.get("sub_caste_name") ?
                fields[headerMapping.get("sub_caste_name")].trim() : null;
        
        // Support both ID and name-based lookups
        String casteIdStr = headerMapping.containsKey("caste_id") && fields.length > headerMapping.get("caste_id") ?
                fields[headerMapping.get("caste_id")].trim() : null;
        String casteName = headerMapping.containsKey("caste_name") && fields.length > headerMapping.get("caste_name") ?
                fields[headerMapping.get("caste_name")].trim() : null;
        String religionIdStr = headerMapping.containsKey("religion_id") && fields.length > headerMapping.get("religion_id") ?
                fields[headerMapping.get("religion_id")].trim() : null;
        String religionName = headerMapping.containsKey("religion_name") && fields.length > headerMapping.get("religion_name") ?
                fields[headerMapping.get("religion_name")].trim() : null;

        if (subCasteName == null || subCasteName.isEmpty())
            errors.put("sub_caste_name", "Missing or invalid value");

        // Validate that either ID or name is provided for caste and religion
        if ((casteIdStr == null || casteIdStr.isEmpty()) && (casteName == null || casteName.isEmpty()))
            errors.put("caste", "Either caste_id or caste_name must be provided");
        if ((religionIdStr == null || religionIdStr.isEmpty()) && (religionName == null || religionName.isEmpty()))
            errors.put("religion", "Either religion_id or religion_name must be provided");

        if (!errors.isEmpty()) {
            return null;
        }

        // Resolve or create Religion
        ReligionEntity religion = resolveOrCreateReligion(religionIdStr, religionName, account, election, errors);
        if (religion == null) {
            return null;
        }

        // Resolve or create Caste
        CasteEntity caste = resolveOrCreateCaste(casteIdStr, casteName, religion, account, election, errors);
        if (caste == null) {
            return null;
        }

        if (!errors.isEmpty()) {
            return null;
        }

        SubCasteEntity subCaste = new SubCasteEntity();
        subCaste.setAccountId(account.getId());
        subCaste.setElectionId(election.getId());
        subCaste.setSubCasteName(subCasteName);
        subCaste.setCaste(caste);
        subCaste.setReligion(religion);
        subCaste.setCreatedAt(LocalDateTime.now());
        subCaste.setUpdatedAt(LocalDateTime.now());

        try {
            if (headerMapping.containsKey("order_index") && fields.length > headerMapping.get("order_index")) {
                String orderIndexStr = fields[headerMapping.get("order_index")].trim();
                subCaste.setOrderIndex(orderIndexStr.isEmpty() ? null : Integer.parseInt(orderIndexStr));
            }
        } catch (NumberFormatException e) {
            errors.put("order_index", "Invalid number format");
            return null;
        }

        return subCaste;
    }

    private long saveBatch(List<SubCasteEntity> subCastes, List<Map<String, Object>> rowErrors,
                          List<SubCasteEntity> savedSubCastes, long currentRow) {
        long successCount = 0;
        try {
            // Save to relational database
            List<SubCasteEntity> saved = subCasteRepository.saveAll(subCastes);
            savedSubCastes.addAll(saved);

            // Save to MongoDB
            List<SubCasteMongo> mongoSubCastes = saved.stream()
                    .map(SubCasteMongo::new)
                    .collect(Collectors.toList());
            subCasteMongoRepository.saveAll(mongoSubCastes);

            successCount = saved.size();
            log.debug("Saved {} sub-castes to SQL and MongoDB: {}", successCount, saved);
        } catch (Exception e) {
            log.error("Error saving batch of {} sub-castes: {}", subCastes.size(), e.getMessage());
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
     * Resolves Religion by ID or name, creates new one if not found
     */
    private ReligionEntity resolveOrCreateReligion(String religionIdStr, String religionName, 
                                                  AccountEntity account, ElectionEntity election, 
                                                  Map<String, String> errors) {
        // Try to resolve by ID first
        if (religionIdStr != null && !religionIdStr.trim().isEmpty()) {
            try {
                Long religionId = Long.parseLong(religionIdStr);
                ReligionEntity religion = religionRepository.findByIdAndAccountIdAndElectionId(
                        religionId, account.getId(), election.getId()).orElse(null);
                if (religion != null) {
                    return religion;
                }
                // If ID provided but not found, log warning but continue with name lookup
                log.warn("Religion ID {} not found, attempting lookup by name: {}", religionId, religionName);
            } catch (NumberFormatException e) {
                errors.put("religion_id", "Invalid number format");
                return null;
            }
        }

        // Resolve or create by name
        if (religionName != null && !religionName.trim().isEmpty()) {
            // Look for existing religion by name
            ReligionEntity religion = religionRepository.findByReligionNameAndAccountIdAndElectionId(
                    religionName.trim(), account.getId(), election.getId()).orElse(null);
            
            if (religion != null) {
                return religion;
            }

            // Create new religion if not found
            log.info("Creating new religion: {} for account: {}, election: {}", 
                    religionName, account.getId(), election.getId());
            
            religion = new ReligionEntity();
            religion.setReligionName(religionName.trim());
            religion.setAccountId(account.getId());
            religion.setElectionId(election.getId());
            
            // Set order index
            Integer maxOrderIndex = religionRepository.findMaxOrderIndexByElectionId(election.getId());
            religion.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
            
            religion.setCreatedAt(LocalDateTime.now());
            religion.setUpdatedAt(LocalDateTime.now());
            
            return religionRepository.save(religion);
        }

        errors.put("religion", "Could not resolve religion by ID or name");
        return null;
    }

    /**
     * Resolves Caste by ID or name, creates new one if not found
     */
    private CasteEntity resolveOrCreateCaste(String casteIdStr, String casteName, 
                                           ReligionEntity religion, AccountEntity account, 
                                           ElectionEntity election, Map<String, String> errors) {
        // Try to resolve by ID first
        if (casteIdStr != null && !casteIdStr.trim().isEmpty()) {
            try {
                Long casteId = Long.parseLong(casteIdStr);
                CasteEntity caste = casteRepository.findByIdAndAccountIdAndElectionId(
                        casteId, account.getId(), election.getId()).orElse(null);
                if (caste != null) {
                    // Verify the caste belongs to the correct religion
                    if (!caste.getReligion().getId().equals(religion.getId())) {
                        errors.put("caste_id", "Caste does not belong to the specified religion");
                        return null;
                    }
                    return caste;
                }
                // If ID provided but not found, log warning but continue with name lookup
                log.warn("Caste ID {} not found, attempting lookup by name: {}", casteId, casteName);
            } catch (NumberFormatException e) {
                errors.put("caste_id", "Invalid number format");
                return null;
            }
        }

        // Resolve or create by name
        if (casteName != null && !casteName.trim().isEmpty()) {
            // Look for existing caste by name and religion
            CasteEntity caste = casteRepository.findByCasteNameAndReligion_IdAndAccountIdAndElectionId(
                    casteName.trim(), religion.getId(), account.getId(), election.getId()).orElse(null);
            
            if (caste != null) {
                return caste;
            }

            // Create new caste if not found
            log.info("Creating new caste: {} for religion: {}, account: {}, election: {}", 
                    casteName, religion.getReligionName(), account.getId(), election.getId());
            
            caste = new CasteEntity();
            caste.setCasteName(casteName.trim());
            caste.setReligion(religion);
            caste.setAccountId(account.getId());
            caste.setElectionId(election.getId());
            
            // Set order index within religion
            Integer maxOrderIndex = casteRepository.findMaxOrderIndexByReligionIdAndElectionId(
                    religion.getId(), election.getId());
            caste.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);
            
            caste.setCreatedAt(LocalDateTime.now());
            caste.setUpdatedAt(LocalDateTime.now());
            
            return casteRepository.save(caste);
        }

        errors.put("caste", "Could not resolve caste by ID or name");
        return null;
    }
}