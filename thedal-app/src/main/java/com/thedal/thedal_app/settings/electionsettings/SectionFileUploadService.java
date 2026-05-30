package com.thedal.thedal_app.settings.electionsettings;

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

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SectionFileUploadService {

	private static final int BATCH_SIZE = 500;

    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SectionMongoRepository sectionMongoRepository;

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
    public void processSectionExcelFile(SectionBulkUploadEntity bulkUploadEntity, AccountEntity account, 
            String fileUrl, ElectionEntity election) throws IOException {
        long startTime = System.currentTimeMillis();
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            bulkUploadEntity.setTotalRecords((long) (sheet.getPhysicalNumberOfRows() - 1));
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // Skip header
            processSectionFile(rowIterator, headerMapping, account, election, bulkUploadEntity);
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
    public void processSectionCsvFile(SectionBulkUploadEntity bulkUploadEntity, AccountEntity account, 
            String fileUrl, ElectionEntity election) throws IOException {
        long startTime = System.currentTimeMillis();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            long totalLines = br.lines().count();
            bulkUploadEntity.setTotalRecords(totalLines);
            try (BufferedReader processingBr = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
                processingBr.readLine(); // Skip header
                processSectionFile(processingBr.lines().iterator(), headerMapping, account, election, bulkUploadEntity);
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

//    @Transactional
//    public void processSectionFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping, 
//            AccountEntity account, ElectionEntity election, SectionBulkUploadEntity bulkUploadEntity) {
//        long startTime = System.currentTimeMillis();
//        Map<String, SectionEntity> sectionMap = new HashMap<>(); // Key: electionId_accountId_sectionNo
//        long totalRecords = 0;
//        long totalSuccessRecords = 0;
//        long totalFailedRecords = 0;
//
//        while (rowIterator.hasNext()) {
//            totalRecords++;
//            Object rowObj = rowIterator.next();
//            SectionBulkUploadEntity.RowError rowError = new SectionBulkUploadEntity.RowError(totalRecords);
//
//            try {
//                SectionEntity section = null;
//                if (rowObj instanceof Row) {
//                    section = processExcelRow((Row) rowObj, headerMapping, account, election, rowError);
//                } else if (rowObj instanceof String) {
//                    String[] fields = ((String) rowObj).split(",");
//                    section = processCsvRow(fields, headerMapping, account, election, rowError);
//                }
//
//                if (section != null) {
//                    String uniqueKey = election.getId() + "_" + account.getId() + "_" + section.getSectionNo();
//                    if (sectionMap.containsKey(uniqueKey)) {
//                        log.debug("Overriding duplicate section_no: {} at row {}", section.getSectionNo(), totalRecords);
//                    }
//                    sectionMap.put(uniqueKey, section);
//                } else {
//                    bulkUploadEntity.getRowErrors().add(rowError);
//                    totalFailedRecords++;
//                }
//            } catch (Exception e) {
//                log.error("Error processing row {}: {}", totalRecords, e.getMessage());
//                rowError.addError("general", "Unexpected error: " + e.getMessage());
//                bulkUploadEntity.getRowErrors().add(rowError);
//                totalFailedRecords++;
//            }
//        }
//
//        if (!sectionMap.isEmpty()) {
//            List<String> sectionNosToProcess = sectionMap.values().stream()
//                .map(s -> String.valueOf(s.getSectionNo())).toList();
//            List<SectionEntity> existingRecords = sectionRepository.findByElectionIdAndAccountIdAndSectionNoIn(
//                election.getId(), account.getId(), sectionNosToProcess);
//            if (!existingRecords.isEmpty()) {
//                List<Long> idsToDelete = existingRecords.stream().map(SectionEntity::getId).toList();
//                sectionRepository.deleteAllByIdInBatch(idsToDelete);
//                log.info("Deleted {} existing records with matching section_no values", idsToDelete.size());
//            }
//
//            List<SectionEntity> sectionsToSave = new ArrayList<>(sectionMap.values());
//            saveBatch(sectionsToSave, bulkUploadEntity);
//            totalSuccessRecords = sectionsToSave.size();
//        }
//
//        totalFailedRecords = totalRecords - totalSuccessRecords;
//        bulkUploadEntity.setTotalProcessedRecords(totalSuccessRecords + totalFailedRecords);
//        bulkUploadEntity.setTotalSuccessRecords(totalSuccessRecords);
//        bulkUploadEntity.setTotalFailedRecords(totalFailedRecords);
//        bulkUploadEntity.setEndTime(LocalDateTime.now());
//        bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
//        bulkUploadEntity.setStatus(totalSuccessRecords > 0 ? BulkUploadStatus.COMPLETED : BulkUploadStatus.FAILED);
//        sectionBulkUploadRepository.save(bulkUploadEntity);
//
//        log.info("Processed {} records: {} unique successful, {} failed in {} ms", 
//            totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
//    }
    @Transactional
    public void processSectionFile(Iterator<?> rowIterator, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, SectionBulkUploadEntity bulkUploadEntity) {
        long startTime = System.currentTimeMillis();
        Map<String, SectionEntity> sectionMap = new HashMap<>(); // Key: electionId_accountId_partNo_sectionNo
        long totalRecords = 0;
        long totalSuccessRecords = 0;
        long totalFailedRecords = 0;

        while (rowIterator.hasNext()) {
            totalRecords++;
            Object rowObj = rowIterator.next();
            SectionBulkUploadEntity.RowError rowError = new SectionBulkUploadEntity.RowError(totalRecords);

            try {
                SectionEntity section = null;
                if (rowObj instanceof Row) {
                    section = processExcelRow((Row) rowObj, headerMapping, account, election, rowError);
                } else if (rowObj instanceof String) {
                    String[] fields = ((String) rowObj).split(",");
                    section = processCsvRow(fields, headerMapping, account, election, rowError);
                }

                if (section != null) {
                    String uniqueKey = election.getId() + "_" + account.getId() + "_" + 
                                     section.getPartNo() + "_" + section.getSectionNo();
                    if (sectionMap.containsKey(uniqueKey)) {
                        log.debug("Overriding duplicate section at row {}: part_no={}, section_no={}", 
                                totalRecords, section.getPartNo(), section.getSectionNo());
                    }
                    sectionMap.put(uniqueKey, section);
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

        if (!sectionMap.isEmpty()) {
            // Update the existing records check to include part_no
            List<SectionEntity> sectionsToSave = new ArrayList<>(sectionMap.values());
            
            // Delete existing records from SQL database
            List<SectionEntity> existingRecords = sectionRepository.findByElectionIdAndAccountIdAndPartNoInAndSectionNoIn(
                election.getId(), 
                account.getId(), 
                sectionsToSave.stream().map(SectionEntity::getPartNo).distinct().toList(),
                sectionsToSave.stream().map(SectionEntity::getSectionNo).distinct().toList()
            );
            
            if (!existingRecords.isEmpty()) {
                List<Long> idsToDelete = existingRecords.stream().map(SectionEntity::getId).toList();
                sectionRepository.deleteAllByIdInBatch(idsToDelete);
                log.info("Deleted {} existing records from SQL with matching part_no and section_no values", idsToDelete.size());
            }

            // Delete existing records from MongoDB
            List<SectionMongo> existingMongoRecords = sectionMongoRepository.findByElectionIdAndAccountIdAndPartNoInAndSectionNoIn(
                election.getId(), 
                account.getId(), 
                sectionsToSave.stream().map(SectionEntity::getPartNo).distinct().toList(),
                sectionsToSave.stream().map(SectionEntity::getSectionNo).distinct().toList()
            );
            
            if (!existingMongoRecords.isEmpty()) {
                List<Long> mongoIdsToDelete = existingMongoRecords.stream().map(SectionMongo::getId).toList();
                sectionMongoRepository.deleteByIdIn(mongoIdsToDelete);
                log.info("Deleted {} existing records from MongoDB with matching part_no and section_no values", mongoIdsToDelete.size());
            }

            saveBatch(sectionsToSave, bulkUploadEntity);
            totalSuccessRecords = sectionsToSave.size();
        }

        totalFailedRecords = totalRecords - totalSuccessRecords;
        bulkUploadEntity.setTotalProcessedRecords(totalSuccessRecords + totalFailedRecords);
        bulkUploadEntity.setTotalSuccessRecords(totalSuccessRecords);
        bulkUploadEntity.setTotalFailedRecords(totalFailedRecords);
        bulkUploadEntity.setEndTime(LocalDateTime.now());
        bulkUploadEntity.setTotalTimeTaken(System.currentTimeMillis() - startTime);
        bulkUploadEntity.setStatus(totalSuccessRecords > 0 ? BulkUploadStatus.COMPLETED : BulkUploadStatus.FAILED);
        sectionBulkUploadRepository.save(bulkUploadEntity);

        log.info("Processed {} records: {} unique successful, {} failed in {} ms", 
                totalRecords, totalSuccessRecords, totalFailedRecords, System.currentTimeMillis() - startTime);
    }

    private SectionEntity processExcelRow(Row row, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, SectionBulkUploadEntity.RowError rowError) {
        Integer partNo = getCellValueAsInteger(row.getCell(headerMapping.get("part_no")));
        Integer sectionNo = getCellValueAsInteger(row.getCell(headerMapping.get("section_no")));
        String sectionNameEn = getCellValueAsString(row.getCell(headerMapping.get("section_name_en")));
        String sectionNameL1 = headerMapping.containsKey("section_name_l1") ? 
            getCellValueAsString(row.getCell(headerMapping.get("section_name_l1"))) : null;

        if (partNo == null) rowError.addError("part_no", "Missing or invalid value");
        if (sectionNo == null) rowError.addError("section_no", "Missing or invalid value");
        if (sectionNameEn == null || sectionNameEn.trim().isEmpty()) 
            rowError.addError("section_name_en", "Missing or invalid value");

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        SectionEntity section = new SectionEntity();
        section.setElection(election);
        section.setAccountId(account.getId());
        section.setPartNo(partNo);
        section.setSectionNo(sectionNo);
        section.setSectionNameEn(sectionNameEn);
        section.setSectionNameL1(sectionNameL1);
        return section;
    }

    private SectionEntity processCsvRow(String[] fields, Map<String, Integer> headerMapping, 
            AccountEntity account, ElectionEntity election, SectionBulkUploadEntity.RowError rowError) {
        String partNoStr = fields.length > headerMapping.get("part_no") ? fields[headerMapping.get("part_no")].trim() : null;
        String sectionNoStr = fields.length > headerMapping.get("section_no") ? fields[headerMapping.get("section_no")].trim() : null;
        String sectionNameEn = fields.length > headerMapping.get("section_name_en") ? fields[headerMapping.get("section_name_en")].trim() : null;
        String sectionNameL1 = headerMapping.containsKey("section_name_l1") && fields.length > headerMapping.get("section_name_l1") ? 
            fields[headerMapping.get("section_name_l1")].trim() : null;

        Integer partNo = partNoStr != null && !partNoStr.isEmpty() ? Integer.parseInt(partNoStr) : null;
        Integer sectionNo = sectionNoStr != null && !sectionNoStr.isEmpty() ? Integer.parseInt(sectionNoStr) : null;

        if (partNo == null) rowError.addError("part_no", "Missing or invalid value");
        if (sectionNo == null) rowError.addError("section_no", "Missing or invalid value");
        if (sectionNameEn == null || sectionNameEn.isEmpty()) 
            rowError.addError("section_name_en", "Missing or invalid value");

        if (!rowError.getErrors().isEmpty()) {
            return null;
        }

        SectionEntity section = new SectionEntity();
        section.setElection(election);
        section.setAccountId(account.getId());
        section.setPartNo(partNo);
        section.setSectionNo(sectionNo);
        section.setSectionNameEn(sectionNameEn);
        section.setSectionNameL1(sectionNameL1);
        return section;
    }

    private void saveBatch(List<SectionEntity> sections, SectionBulkUploadEntity bulkUploadEntity) {
        try {
            // Save to relational database
            List<SectionEntity> savedSections = sectionRepository.saveAll(sections);
            
            // Sync to MongoDB
            List<SectionMongo> mongoSections = new ArrayList<>();
            for (SectionEntity section : savedSections) {
                mongoSections.add(new SectionMongo(section));
            }
            sectionMongoRepository.saveAll(mongoSections);
            
            log.debug("Saved {} sections to both SQL and MongoDB: {}", savedSections.size(), savedSections);
        } catch (Exception e) {
            log.error("Error saving batch of {} sections: {}", sections.size(), e.getMessage());
            bulkUploadEntity.setTotalFailedRecords(bulkUploadEntity.getTotalFailedRecords() + sections.size());
            bulkUploadEntity.setTotalSuccessRecords(bulkUploadEntity.getTotalSuccessRecords() - sections.size());
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
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case NUMERIC: return (int) cell.getNumericCellValue();
            case STRING: 
                String stringValue = cell.getStringCellValue().trim();
                if (stringValue.isEmpty()) return null;
                try {
                    return Integer.parseInt(stringValue);
                } catch (NumberFormatException e) {
                    return null;
                }
            default: return null;
        }
    }  
    
}
