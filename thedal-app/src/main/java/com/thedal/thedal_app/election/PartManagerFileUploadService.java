package com.thedal.thedal_app.election;

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

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PartManagerFileUploadService {
	
	private static final int BATCH_SIZE = 500;

	@Autowired
    private PartManagerBulkUploadRepository partManagerBulkUploadRepository;

    @Autowired
    private PartManagerRepository partManagerRepository;

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
    public void processPartManagerExcelFile(PartManagerBulkUploadEntity bulkUploadEntity, AccountEntity account, 
            String fileUrl, ElectionEntity election) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            validateHeaders(headerMapping, bulkUploadEntity);
            
            int totalRows = sheet.getPhysicalNumberOfRows();
            bulkUploadEntity.setTotalRecords((long) (totalRows - 1)); // Exclude header row
            log.info("Starting Excel processing for {} rows", totalRows - 1);
            
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row
            processPartManagerFile(rowIterator, headerMapping, account, election, bulkUploadEntity);
            
        } catch (Exception e) {
            log.error("Excel processing failed: {}", e.getMessage(), e);
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            partManagerBulkUploadRepository.save(bulkUploadEntity);
            throw e;
        }
    }

    @Transactional
    public void processPartManagerCsvFile(PartManagerBulkUploadEntity bulkUploadEntity, AccountEntity account, 
            String fileUrl, ElectionEntity election) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            validateHeaders(headerMapping, bulkUploadEntity);
            
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            log.info("Starting CSV processing for {} rows", totalLines);
            
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processPartManagerFile(processingBr.lines().iterator(), headerMapping, account, election, bulkUploadEntity);
            }
            
        } catch (Exception e) {
            log.error("CSV processing failed: {}", e.getMessage(), e);
            bulkUploadEntity.setStatus(BulkUploadStatus.FAILED);
            bulkUploadEntity.setEndTime(LocalDateTime.now());
            bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
            partManagerBulkUploadRepository.save(bulkUploadEntity);
            throw e;
        }
    }

    private void validateHeaders(Map<String, Integer> headerMapping, PartManagerBulkUploadEntity bulkUploadEntity) {
        String[] optionalHeaders = {"part_name_l1", "school_name", "part_lat", "part_long", "pincode", "school_lat", "school_long",
        		"booth_vulnerability", 
    			"part_captain_name", "captain_designation", "captain_mobile_no"};
        for (String header : optionalHeaders) {
            if (!headerMapping.containsKey(header)) {
                bulkUploadEntity.getHeaderErrors().add("Missing optional header: " + header);
            }
        }
    }

//    @Transactional
//    public void processPartManagerFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping, 
//            AccountEntity account, ElectionEntity election, PartManagerBulkUploadEntity bulkUploadEntity) {
//        long startTime = System.currentTimeMillis();
//        Map<String, PartManager> partManagerMap = new HashMap<>(); // Map to track latest part_no
//        long totalRecords = 0;
//        long totalSuccessRecords = 0;
//        long totalFailedRecords = 0;
//
//        // Step 1: Process the file and keep only the last occurrence of each part_no
//        while (rowIterator.hasNext()) {
//            totalRecords++;
//            Object rowObj = rowIterator.next();
//            PartManagerBulkUploadEntity.RowError rowError = new PartManagerBulkUploadEntity.RowError(totalRecords);
//
//            try {
//                PartManager partManager = null;
//                if (rowObj instanceof Row) {
//                    Row row = (Row) rowObj;
//                    partManager = processExcelRow(row, headerMapping, account, election, rowError);
//                } else if (rowObj instanceof String) {
//                    String[] fields = ((String) rowObj).split(",");
//                    partManager = processCsvRow(fields, headerMapping, account, election, rowError);
//                }
//
//                if (partManager != null) {
//                    String normalizedPartNo = partManager.getPartNo().trim(); // Normalize partNo
//                    partManager.setPartNo(normalizedPartNo); // Store trimmed value
//                    if (partManagerMap.containsKey(normalizedPartNo)) {
//                        log.debug("Overriding duplicate part_no: {} at row {}", normalizedPartNo, totalRecords);
//                    }
//                    partManagerMap.put(normalizedPartNo, partManager);
//                } else {
//                    bulkUploadEntity.getRowErrors().add(rowError);
//                    totalFailedRecords++;
//                }
//
//            } catch (Exception e) {
//                log.error("Error processing row {}: {}", totalRecords, e.getMessage());
//                rowError.addError("general", "Unexpected error: " + e.getMessage());
//                bulkUploadEntity.getRowErrors().add(rowError);
//                totalFailedRecords++;
//            }
//        }
//
//        // Step 2: Delete all existing records for the given election_id and account_id that match part_no values
//        if (!partManagerMap.isEmpty()) {
//            List<String> partNosToProcess = new ArrayList<>(partManagerMap.keySet());
//            
//            // Delete from SQL database
//            List<PartManager> existingRecords = partManagerRepository.findByElectionIdAndAccountIdAndPartNoIn(
//                election.getId(), account.getId(), partNosToProcess);
//            if (!existingRecords.isEmpty()) {
//                List<Long> idsToDelete = existingRecords.stream().map(PartManager::getId).toList();
//                partManagerRepository.deleteAllByIdInBatch(idsToDelete);
//                log.info("Deleted {} existing records from SQL with matching part_no values", idsToDelete.size());
//            }
//
//            // Step 3: Save the latest records from the file
//            List<PartManager> partManagersToSave = new ArrayList<>(partManagerMap.values());
//            saveBatch(partManagersToSave, bulkUploadEntity);
//
//            totalSuccessRecords = partManagersToSave.size(); // Number of unique records saved
//        }
//
//        totalFailedRecords = totalRecords - totalSuccessRecords; // Failed due to errors or duplicates
//
//        bulkUploadEntity.setTotalProcessedRecords(totalSuccessRecords + totalFailedRecords);
//        bulkUploadEntity.setTotalSuccessRecords(totalSuccessRecords);
//        bulkUploadEntity.setTotalFailedRecords(totalFailedRecords);
//        bulkUploadEntity.setEndTime(LocalDateTime.now());
//        bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
//        bulkUploadEntity.setStatus(totalSuccessRecords > 0 ? BulkUploadStatus.COMPLETED : BulkUploadStatus.FAILED);
//        partManagerBulkUploadRepository.save(bulkUploadEntity);
//
//        log.info("Processed {} records: {} unique successful, {} failed (including duplicates) in {} ms", 
//            totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
//    }
    
    @Transactional
    public void processPartManagerFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, PartManagerBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, PartManager> partManagerMap = new HashMap<>(); // Map to track latest part_no
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        // Step 1: Process the file and keep only the last occurrence of each part_no
        while (rowIterator.hasNext()) {
            Object rowObj = rowIterator.next();
            
            // Skip empty rows early
            if (isEmptyRow(rowObj, headerMapping)) {
                continue; // Skip empty rows without incrementing totalRecords
            }

            totalRecords++;
            PartManagerBulkUploadEntity.RowError rowError = new PartManagerBulkUploadEntity.RowError(totalRecords);

            try {
                PartManager partManager = null;
                if (rowObj instanceof Row) {
                    Row row = (Row) rowObj;
                    partManager = processExcelRow(row, headerMapping, account, election, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    partManager = processCsvRow(fields, headerMapping, account, election, rowError);
                }

                if (partManager != null) {
                    String normalizedPartNo = partManager.getPartNo().trim(); // Normalize partNo
                    partManager.setPartNo(normalizedPartNo); // Store trimmed value
                    if (partManagerMap.containsKey(normalizedPartNo)) {
                        log.debug("Overriding duplicate part_no: {} at row {}", normalizedPartNo, totalRecords);
                    }
                    partManagerMap.put(normalizedPartNo, partManager);
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

        // Step 2: Process existing and new records
        if (!partManagerMap.isEmpty()) {
            List<String> partNosToProcess = new ArrayList<>(partManagerMap.keySet());
            
            // Fetch existing records from the database
            List<PartManager> existingRecords = partManagerRepository.findByElectionIdAndAccountIdAndPartNoIn(
                election.getId(), account.getId(), partNosToProcess);
            Map<String, PartManager> existingRecordsMap = existingRecords.stream()
                .collect(Collectors.toMap(PartManager::getPartNo, pm -> pm));

            List<PartManager> partManagersToSave = new ArrayList<>();
            List<PartManager> partManagersToUpdate = new ArrayList<>();

            // Step 3: Separate records into updates and inserts
            for (PartManager newPartManager : partManagerMap.values()) {
                String partNo = newPartManager.getPartNo();
                if (existingRecordsMap.containsKey(partNo)) {
                    // Update existing record
                    PartManager existingPartManager = existingRecordsMap.get(partNo);
                    existingPartManager.setPartNameEnglish(newPartManager.getPartNameEnglish());
                    existingPartManager.setPartNameL1(newPartManager.getPartNameL1());
                    existingPartManager.setSchoolName(newPartManager.getSchoolName());
                    existingPartManager.setPartLat(newPartManager.getPartLat());
                    existingPartManager.setPartLong(newPartManager.getPartLong());
                    existingPartManager.setPincode(newPartManager.getPincode());
                    existingPartManager.setSchoolLat(newPartManager.getSchoolLat());
                    existingPartManager.setSchoolLong(newPartManager.getSchoolLong());
                    existingPartManager.setBoothVulnerability(newPartManager.getBoothVulnerability());
                    existingPartManager.setOrderIndex(newPartManager.getOrderIndex());
                    existingPartManager.setPartCaptainName(newPartManager.getPartCaptainName());
					existingPartManager.setCaptainDesignation(newPartManager.getCaptainDesignation());
					existingPartManager.setCaptainMobileNo(newPartManager.getCaptainMobileNo());
					existingPartManager.setBloName(newPartManager.getBloName());
					existingPartManager.setBloDesignation(newPartManager.getBloDesignation());
					existingPartManager.setBloMobileNumber(newPartManager.getBloMobileNumber());
					existingPartManager.setBla2Name(newPartManager.getBla2Name());
					existingPartManager.setBla2Designation(newPartManager.getBla2Designation());
					existingPartManager.setBla2MobileNumber(newPartManager.getBla2MobileNumber());
                    partManagersToUpdate.add(existingPartManager);
                } else {
                    // New record to insert
                    partManagersToSave.add(newPartManager);
                }
            }

            // Step 4: Save new records and update existing ones
            if (!partManagersToSave.isEmpty()) {
                saveBatch(partManagersToSave, bulkUploadEntity);
                totalSuccessRecords += partManagersToSave.size();
            }
            if (!partManagersToUpdate.isEmpty()) {
                saveBatch(partManagersToUpdate, bulkUploadEntity);
                totalSuccessRecords += partManagersToUpdate.size();
            }
        }

        bulkUploadEntity.setTotalProcessedRecords(totalSuccessRecords + totalFailedRecords);
        bulkUploadEntity.setTotalSuccessRecords(totalSuccessRecords);
        bulkUploadEntity.setTotalFailedRecords(totalFailedRecords);
        bulkUploadEntity.setEndTime(LocalDateTime.now());
        bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
        bulkUploadEntity.setStatus(totalSuccessRecords > 0 ? BulkUploadStatus.COMPLETED : BulkUploadStatus.FAILED);
        partManagerBulkUploadRepository.save(bulkUploadEntity);

        log.info("Processed {} records: {} unique successful, {} failed (including duplicates) in {} ms", 
            totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
    }
    
    private boolean isEmptyRow(Object rowObj, Map<String, Integer> headerMapping) {
        if (rowObj instanceof Row) {
            Row row = (Row) rowObj;
            String partNo = getCellValueAsString(row.getCell(headerMapping.get("part_no")));
            String partNameEnglish = getCellValueAsString(row.getCell(headerMapping.get("part_name_english")));
            return (partNo == null || partNo.trim().isEmpty()) && 
                   (partNameEnglish == null || partNameEnglish.trim().isEmpty());
        } else if (rowObj instanceof String) {
            String[] fields = ((String) rowObj).split(",");
            String partNo = fields.length > headerMapping.get("part_no") ? 
                fields[headerMapping.get("part_no")].trim() : null;
            String partNameEnglish = fields.length > headerMapping.get("part_name_english") ? 
                fields[headerMapping.get("part_name_english")].trim() : null;
            return (partNo == null || partNo.isEmpty()) && 
                   (partNameEnglish == null || partNameEnglish.isEmpty());
        }
        return true; // Treat unknown row types as empty
    }
    
    private PartManager processExcelRow(Row row, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, PartManagerBulkUploadEntity.RowError rowError) {
        String partNo = getCellValueAsString(row.getCell(headerMapping.get("part_no")));
        String partNameEnglish = getCellValueAsString(row.getCell(headerMapping.get("part_name_english")));
        
        if (partNo == null || partNo.trim().isEmpty()) 
            rowError.addError("part_no", "Missing or invalid value");
        if (partNameEnglish == null || partNameEnglish.isEmpty()) 
            rowError.addError("part_name_english", "Missing or invalid value");

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        PartManager partManager = new PartManager();
        partManager.setAccountId(account.getId());
        partManager.setElectionId(election.getId());
        partManager.setPartNo(partNo.trim()); // Trim immediately
        partManager.setPartNameEnglish(partNameEnglish);
        
        partManager.setPartNameL1(headerMapping.containsKey("part_name_l1") ? 
            getCellValueAsString(row.getCell(headerMapping.get("part_name_l1"))) : null);
        partManager.setSchoolName(headerMapping.containsKey("school_name") ? 
            getCellValueAsString(row.getCell(headerMapping.get("school_name"))) : null);
        partManager.setPartLat(headerMapping.containsKey("part_lat") ? 
            getCellValueAsDoubleWithDefault(row.getCell(headerMapping.get("part_lat"))) : 0.0);
        partManager.setPartLong(headerMapping.containsKey("part_long") ? 
            getCellValueAsDoubleWithDefault(row.getCell(headerMapping.get("part_long"))) : 0.0);
        partManager.setPincode(headerMapping.containsKey("pincode") ? 
            getCellValueAsString(row.getCell(headerMapping.get("pincode"))) : null);
        partManager.setSchoolLat(headerMapping.containsKey("school_lat") ? 
                getCellValueAsDoubleWithDefault(row.getCell(headerMapping.get("school_lat"))) : 0.0);
            partManager.setSchoolLong(headerMapping.containsKey("school_long") ? 
                getCellValueAsDoubleWithDefault(row.getCell(headerMapping.get("school_long"))) : 0.0);
            partManager.setBoothVulnerability(headerMapping.containsKey("booth_vulnerability") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("booth_vulnerability"))) : null);
        		partManager.setPartCaptainName(headerMapping.containsKey("part_captain_name") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("part_captain_name"))) : null);
        		partManager.setCaptainDesignation(headerMapping.containsKey("captain_designation") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("captain_designation"))) : null);
        		partManager.setCaptainMobileNo(headerMapping.containsKey("captain_mobile_no") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("captain_mobile_no"))) : null);
        		
        		partManager.setBloName(headerMapping.containsKey("blo_name") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("blo_name"))) : null);
        		partManager.setBloDesignation(headerMapping.containsKey("blo_designation") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("blo_designation"))) : null);
        		partManager.setBloMobileNumber(headerMapping.containsKey("blo_mobile_number") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("blo_mobile_number"))) : null);
        		
        		partManager.setBla2Name(headerMapping.containsKey("bla2_name") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("bla2_name"))) : null);
        		partManager.setBla2Designation(headerMapping.containsKey("bla2_designation") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("bla2_designation"))) : null);
        		partManager.setBla2MobileNumber(headerMapping.containsKey("bla2_mobile_number") ? 
        			getCellValueAsString(row.getCell(headerMapping.get("bla2_mobile_number"))) : null);

//        		Integer maxOrderIndex = partManagerRepository.findMaxOrderIndexByElectionId(election.getId());
//        		partManager.setOrderIndex(maxOrderIndex != null ? maxOrderIndex + 1 : 0);

        return partManager;
    }

    private PartManager processCsvRow(String[] fields, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, PartManagerBulkUploadEntity.RowError rowError) {
        String partNo = fields.length > headerMapping.get("part_no") ? 
            fields[headerMapping.get("part_no")].trim() : null;
        String partNameEnglish = fields.length > headerMapping.get("part_name_english") ? 
            fields[headerMapping.get("part_name_english")].trim() : null;

        if (partNo == null || partNo.isEmpty()) 
            rowError.addError("part_no", "Missing or invalid value");
        if (partNameEnglish == null || partNameEnglish.isEmpty()) 
            rowError.addError("part_name_english", "Missing or invalid value");

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        PartManager partManager = new PartManager();
        partManager.setAccountId(account.getId());
        partManager.setElectionId(election.getId());
        partManager.setPartNo(partNo); // Already trimmed
        partManager.setPartNameEnglish(partNameEnglish);

        try {
            partManager.setPartNameL1(headerMapping.containsKey("part_name_l1") && 
                fields.length > headerMapping.get("part_name_l1") ? 
                fields[headerMapping.get("part_name_l1")].trim() : null);
            partManager.setSchoolName(headerMapping.containsKey("school_name") && 
                fields.length > headerMapping.get("school_name") ? 
                fields[headerMapping.get("school_name")].trim() : null);
            partManager.setPincode(headerMapping.containsKey("pincode") && 
                fields.length > headerMapping.get("pincode") ? 
                fields[headerMapping.get("pincode")].trim() : null);

            if (headerMapping.containsKey("part_lat") && fields.length > headerMapping.get("part_lat")) {
                String partLatStr = fields[headerMapping.get("part_lat")].trim();
                partManager.setPartLat(partLatStr.isEmpty() ? 0.0 : Double.parseDouble(partLatStr));
            }
            if (headerMapping.containsKey("part_long") && fields.length > headerMapping.get("part_long")) {
                String partLongStr = fields[headerMapping.get("part_long")].trim();
                partManager.setPartLong(partLongStr.isEmpty() ? 0.0 : Double.parseDouble(partLongStr));
            }
            if (headerMapping.containsKey("school_lat") && fields.length > headerMapping.get("school_lat")) {
                String schoolLatStr = fields[headerMapping.get("school_lat")].trim();
                partManager.setSchoolLat(schoolLatStr.isEmpty() ? 0.0 : Double.parseDouble(schoolLatStr));
            }
            if (headerMapping.containsKey("school_long") && fields.length > headerMapping.get("school_long")) {
                String schoolLongStr = fields[headerMapping.get("school_long")].trim();
                partManager.setSchoolLong(schoolLongStr.isEmpty() ? 0.0 : Double.parseDouble(schoolLongStr));
            }
            
            partManager.setBoothVulnerability(headerMapping.containsKey("booth_vulnerability") && 
                fields.length > headerMapping.get("booth_vulnerability") ? 
                fields[headerMapping.get("booth_vulnerability")].trim() : null);
            partManager.setPartCaptainName(headerMapping.containsKey("part_captain_name") && 
                fields.length > headerMapping.get("part_captain_name") ? 
                fields[headerMapping.get("part_captain_name")].trim() : null);
            partManager.setCaptainDesignation(headerMapping.containsKey("captain_designation") && 
                fields.length > headerMapping.get("captain_designation") ? 
                fields[headerMapping.get("captain_designation")].trim() : null);
            partManager.setCaptainMobileNo(headerMapping.containsKey("captain_mobile_no") && 
                fields.length > headerMapping.get("captain_mobile_no") ? 
                fields[headerMapping.get("captain_mobile_no")].trim() : null);
            
            partManager.setBloName(headerMapping.containsKey("blo_name") && 
                fields.length > headerMapping.get("blo_name") ? 
                fields[headerMapping.get("blo_name")].trim() : null);
            partManager.setBloDesignation(headerMapping.containsKey("blo_designation") && 
                fields.length > headerMapping.get("blo_designation") ? 
                fields[headerMapping.get("blo_designation")].trim() : null);
            partManager.setBloMobileNumber(headerMapping.containsKey("blo_mobile_number") && 
                fields.length > headerMapping.get("blo_mobile_number") ? 
                fields[headerMapping.get("blo_mobile_number")].trim() : null);
            
            partManager.setBla2Name(headerMapping.containsKey("bla2_name") && 
                fields.length > headerMapping.get("bla2_name") ? 
                fields[headerMapping.get("bla2_name")].trim() : null);
            partManager.setBla2Designation(headerMapping.containsKey("bla2_designation") && 
                fields.length > headerMapping.get("bla2_designation") ? 
                fields[headerMapping.get("bla2_designation")].trim() : null);
            partManager.setBla2MobileNumber(headerMapping.containsKey("bla2_mobile_number") && 
                fields.length > headerMapping.get("bla2_mobile_number") ? 
                fields[headerMapping.get("bla2_mobile_number")].trim() : null);
        } catch (NumberFormatException e) {
            rowError.addError("coordinates", "Invalid number format: " + e.getMessage());
            return null;
        }

        return partManager;
    }

    private void saveBatch(List<PartManager> partManagers, PartManagerBulkUploadEntity bulkUploadEntity) {
        try {
            // Save to relational database
            List<PartManager> savedPartManagers = partManagerRepository.saveAll(partManagers);
            
            log.debug("Saved {} part managers to SQL: {}", savedPartManagers.size(), savedPartManagers);
        } catch (Exception e) {
            log.error("Error saving batch of {} part managers: {}", partManagers.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + partManagers.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - partManagers.size());
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
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

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return null;
        return cell.getNumericCellValue();
    }
    
    // New method that returns 0.0 instead of null for coordinates
    private Double getCellValueAsDoubleWithDefault(Cell cell) {
        if (cell == null || cell.getCellType() != CellType.NUMERIC) return 0.0;
        return cell.getNumericCellValue();
    }
}
