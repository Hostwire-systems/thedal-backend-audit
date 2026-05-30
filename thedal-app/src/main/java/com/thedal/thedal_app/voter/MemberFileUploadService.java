package com.thedal.thedal_app.voter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MemberFileUploadService {

    private static final int BATCH_SIZE = 1000;

    @Autowired
    private MemberRepository memberRepo;
    @Autowired
    private BulkUploadRepo bulkUploadRepo;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BulkUploadMemberRepository bulkUploadMemberRepo;
    @Autowired
    private BulkUploadErrorRepository bulkUploadErrorRepo;
    @Autowired
    private MemberMongoRepository memberMongoRepository;

    @Async
    public void processExcelFileAsync(Long bulkUploadId, Long accountId, Long electionId, String fileUrl,
            Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) throws IOException {
        BulkUploadMemberEntity bulkUpload = bulkUploadMemberRepo.findById(bulkUploadId)
                .orElseThrow(() -> new IllegalArgumentException("BulkUploadMemberEntity not found"));

        try (InputStream inputStream = new URL(fileUrl).openStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            processFileAsync(sheet.iterator(), headerMapping, accountId, electionId, true, bulkUpload, mandatoryHeaders);
        } catch (Exception e) {
            log.error("Error processing Excel file: {}", e.getMessage(), e);
            bulkUpload.setStatus(BulkUploadStatus.FAILED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUploadMemberRepo.save(bulkUpload);
            throw new IOException("Failed to process Excel file", e);
        }
    }

    @Async
    public void processCsvFileAsync(Long bulkUploadId, Long accountId, Long electionId, String fileUrl,
            Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) throws IOException {
        BulkUploadMemberEntity bulkUpload = bulkUploadMemberRepo.findById(bulkUploadId)
                .orElseThrow(() -> new IllegalArgumentException("BulkUploadMemberEntity not found"));

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            br.readLine(); // Skip header
            processFileAsync(br.lines().iterator(), headerMapping, accountId, electionId, false, bulkUpload, mandatoryHeaders);
        } catch (Exception e) {
            log.error("Error processing CSV file: {}", e.getMessage(), e);
            bulkUpload.setStatus(BulkUploadStatus.FAILED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUploadMemberRepo.save(bulkUpload);
            throw new IOException("Failed to process CSV file", e);
        }
    }

    @Transactional
    public void processFileAsync(Iterator<?> dataIterator, Map<String, Integer> headerMapping, Long accountId,
            Long electionId, boolean isExcel, BulkUploadMemberEntity bulkUpload, Set<String> mandatoryHeaders) throws IOException {
        List<MemberEntity> memberBatch = new ArrayList<>();
        int totalRecords = 0;
        Map<Integer, List<Map<String, Object>>> rowErrorsByNumber = new HashMap<>();

        if (isExcel) dataIterator.next(); // Skip header row

        while (dataIterator.hasNext()) {
            totalRecords++;
            Object currentRow = dataIterator.next();
            int rowNum = totalRecords;

            Map<String, String> rawValues = isExcel
                    ? extractRawValues((Row) currentRow, headerMapping, mandatoryHeaders)
                    : extractRawValuesFromCsv(((String) currentRow).split(","), headerMapping, mandatoryHeaders);

            Map<String, String> validationErrors = validateRowData(rawValues, mandatoryHeaders);
            if (!validationErrors.isEmpty()) {
                List<Map<String, Object>> rowErrors = rowErrorsByNumber.computeIfAbsent(rowNum, k -> new ArrayList<>());
                validationErrors.forEach((header, value) -> {
                    Map<String, Object> errorDetail = new HashMap<>();
                    errorDetail.put("header", header);
                    errorDetail.put("value", value);
                    errorDetail.put("reason", getReasonForError(header, value));
                    rowErrors.add(errorDetail);
                });
                bulkUpload.setTotalFailedMembers(bulkUpload.getTotalFailedMembers() + 1);
                continue;
            }

            MemberEntity memberEntity = isExcel
                    ? mapToMemberEntityDynamic((Row) currentRow, headerMapping, accountId, electionId)
                    : mapToMemberEntityFromCsv(((String) currentRow).split(","), headerMapping, accountId, electionId);
            memberBatch.add(memberEntity);

            if (memberBatch.size() >= BATCH_SIZE) {
                processBatch(memberBatch, bulkUpload);
                memberBatch.clear();
            }
        }

        if (!memberBatch.isEmpty()) {
            processBatch(memberBatch, bulkUpload);
        }

        // Log errors if any
        if (!rowErrorsByNumber.isEmpty()) {
            BulkUploadErrorEntity errorEntity = new BulkUploadErrorEntity();
            errorEntity.setBulkUploadId(bulkUpload.getId());
            errorEntity.setElectionId(electionId);
            errorEntity.setAccountId(accountId);

            ObjectMapper mapper = new ObjectMapper();
            try {
                errorEntity.setRowNumber(mapper.writeValueAsString(new ArrayList<>(rowErrorsByNumber.keySet())));
                List<Map<String, Object>> allErrors = rowErrorsByNumber.entrySet().stream()
                        .map(entry -> {
                            Map<String, Object> rowError = new HashMap<>();
                            rowError.put("rowNumber", entry.getKey());
                            rowError.put("errors", entry.getValue());
                            return rowError;
                        }).collect(Collectors.toList());
                errorEntity.setRowError(mapper.writeValueAsString(allErrors));
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize errors: {}", e.getMessage());
                errorEntity.setRowNumber(rowErrorsByNumber.keySet().stream().map(String::valueOf).collect(Collectors.joining(",")));
                errorEntity.setRowError("Serialization failed: " + e.getMessage());
            }
            bulkUploadErrorRepo.save(errorEntity);
        }

        bulkUpload.setTotalRecords(totalRecords);
        bulkUpload.setTotalProcessedMembers(bulkUpload.getTotalSuccessMembers() + bulkUpload.getTotalFailedMembers());
        bulkUpload.setStatus(bulkUpload.getTotalFailedMembers() == totalRecords ? BulkUploadStatus.FAILED : BulkUploadStatus.COMPLETED);
        bulkUpload.setEndTime(LocalDateTime.now());
        bulkUploadMemberRepo.save(bulkUpload);
    }

    @Transactional
    private void processBatch(List<MemberEntity> memberBatch, BulkUploadMemberEntity bulkUpload) {
        try {
            // Save to PostgreSQL
            List<MemberEntity> savedMembers = memberRepo.saveAll(memberBatch);
            
            // Save to MongoDB with dual-write pattern
            try {
                List<MemberMongo> mongoMembers = savedMembers.stream()
                        .map(MemberMongo::new)
                        .collect(Collectors.toList());
                memberMongoRepository.saveAll(mongoMembers);
                log.info("Successfully saved {} members to MongoDB in batch", savedMembers.size());
            } catch (Exception mongoEx) {
                log.error("Failed to save member batch to MongoDB: {}", mongoEx.getMessage(), mongoEx);
                throw new RuntimeException("MongoDB batch save failed, triggering rollback", mongoEx);
            }
            
            bulkUpload.setTotalSuccessMembers(bulkUpload.getTotalSuccessMembers() + memberBatch.size());
            bulkUpload.setTotalProcessedMembers(bulkUpload.getTotalProcessedMembers() + memberBatch.size());
            bulkUpload.setLastUpdatedTime(LocalDateTime.now());
            bulkUploadMemberRepo.save(bulkUpload);
            log.info("Batch insert completed, rows affected: {}", memberBatch.size());
        } catch (Exception e) {
            log.error("Error during batch insert: {}", e.getMessage(), e);
            bulkUpload.setTotalFailedMembers(bulkUpload.getTotalFailedMembers() + memberBatch.size());
            bulkUpload.setTotalProcessedMembers(bulkUpload.getTotalProcessedMembers() + memberBatch.size());
            bulkUpload.setLastUpdatedTime(LocalDateTime.now());
            bulkUploadMemberRepo.save(bulkUpload);
            throw e;
        }
    }

    private Map<String, String> extractRawValues(Row row, Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
        Map<String, String> rawValues = new HashMap<>();
        for (String field : mandatoryHeaders) {
            Integer index = headerMapping.get(field);
            rawValues.put(field, index != null ? getCellValue(row, index) : null);
        }
        return rawValues;
    }

    private Map<String, String> extractRawValuesFromCsv(String[] row, Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) {
        Map<String, String> rawValues = new HashMap<>();
        for (String field : mandatoryHeaders) {
            Integer index = headerMapping.get(field);
            rawValues.put(field, index != null && index < row.length ? row[index].trim() : null);
        }
        return rawValues;
    }

    private Map<String, String> validateRowData(Map<String, String> rawValues, Set<String> mandatoryHeaders) {
        Map<String, String> errors = new HashMap<>();
        for (String field : mandatoryHeaders) {
            String value = rawValues.get(field);
            switch (field) {
                case "member_name":
                case "mobile_number":
                    if (value == null || value.trim().isEmpty()) {
                        errors.put(field, value);
                    }
                    break;
                case "gender":
                    if (value == null || value.trim().isEmpty()) {
                        errors.put(field, value);
                    } else if (!Set.of("male", "female", "other").contains(value.toLowerCase())) {
                        errors.put(field, value);
                    }
                    break;
            }
        }
        return errors;
    }

    private String getReasonForError(String header, String value) {
        switch (header) {
            case "member_name":
            case "mobile_number":
                return value == null || value.trim().isEmpty() ? "Mandatory field missing" : "Invalid value";
            case "gender":
                return value == null || value.trim().isEmpty() ? "Mandatory field missing" : "Invalid value (expected: male, female, other)";
            default:
                return "Unknown error";
        }
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default: return null;
        }
    }

    private MemberEntity mapToMemberEntityDynamic(Row row, Map<String, Integer> headerMapping, Long accountId, Long electionId) {
        MemberEntity member = new MemberEntity();
        member.setAccountId(accountId);
        member.setElectionId(electionId);

        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            Integer index = entry.getValue();
            String value = getCellValue(row, index);
            if (value == null) continue;

            switch (header) {
                case "member_name": member.setMemberName(value); break;
                case "relation_name": member.setRelationName(value); break;
                case "relation_type": member.setRelationType(value); break;
                case "gender": member.setGender(value.toLowerCase()); break;
                case "date_of_birth": member.setDateOfBirth(LocalDate.parse(value)); break;
                case "age": member.setAge(Integer.parseInt(value)); break;
                case "occupation": member.setOccupation(value); break;
                case "education": member.setEducation(value); break;
                case "full_address": member.setFullAddress(value); break;
                case "mobile_number": member.setMobileNumber(value); break;
                case "member_since_year": member.setMemberSinceYear(Integer.parseInt(value)); break;
                case "membership_no": member.setMembershipNo(value); break;
                case "state_name_en": member.setStateNameEn(value); break;
                case "state_name_l1": member.setStateNameL1(value); break;
                case "state_name_l2": member.setStateNameL2(value); break;
                case "district_code": member.setDistrictCode(value); break;
                case "district_name_en": member.setDistrictNameEn(value); break;
                case "district_name_l1": member.setDistrictNameL1(value); break;
                case "district_name_l2": member.setDistrictNameL2(value); break;
                case "pc_no": member.setPcNo(value); break;
                case "pc_name_en": member.setPcNameEn(value); break;
                case "pc_name_l1": member.setPcNameL1(value); break;
                case "pc_name_l2": member.setPcNameL2(value); break;
                case "ac_no": member.setAcNo(value); break;
                case "ac_name_en": member.setAcNameEn(value); break;
                case "ac_name_l1": member.setAcNameL1(value); break;
                case "ac_name_l2": member.setAcNameL2(value); break;
                case "urban_no": member.setUrbanNo(value); break;
                case "urban_name_en": member.setUrbanNameEn(value); break;
                case "urban_name_l1": member.setUrbanNameL1(value); break;
                case "urban_ward_no": member.setUrbanWardNo(value); break;
                case "rur_district_union_no": member.setRurDistrictUnionNo(value); break;
                case "rur_district_union_name_en": member.setRurDistrictUnionNameEn(value); break;
                case "rur_district_union_name_l1": member.setRurDistrictUnionNameL1(value); break;
                case "rur_district_union_name_l2": member.setRurDistrictUnionNameL2(value); break;
                case "rur_district_union_ward_no": member.setRurDistrictUnionWardNo(value); break;
                case "pan_union_no": member.setPanUnionNo(value); break;
                case "pan_union_name_en": member.setPanUnionNameEn(value); break;
                case "pan_union_name_l1": member.setPanUnionNameL1(value); break;
                case "pan_union_name_l2": member.setPanUnionNameL2(value); break;
                case "pan_union_ward_no": member.setPanUnionWardNo(value); break;
                case "vill_pan_no": member.setVillPanNo(value); break;
                case "vill_pan_name_en": member.setVillPanNameEn(value); break;
                case "vill_pan_name_l1": member.setVillPanNameL1(value); break;
                case "vill_pan_ward_no": member.setVillPanWardNo(value); break;
                case "state_code": member.setStateCode(value); break;
                case "epic_number": member.setEpicNumber(value); break;
            }
        }
        return member;
    }

    private MemberEntity mapToMemberEntityFromCsv(String[] fields, Map<String, Integer> headerMapping, Long accountId, Long electionId) {
        MemberEntity member = new MemberEntity();
        member.setAccountId(accountId);
        member.setElectionId(electionId);

        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            Integer index = entry.getValue();
            if (index >= fields.length || fields[index].isEmpty()) continue;
            String value = fields[index].trim();

            switch (header) {
                case "member_name": member.setMemberName(value); break;
                case "relation_name": member.setRelationName(value); break;
                case "relation_type": member.setRelationType(value); break;
                case "gender": member.setGender(value.toLowerCase()); break;
                case "date_of_birth": member.setDateOfBirth(LocalDate.parse(value)); break;
                case "age": member.setAge(Integer.parseInt(value)); break;
                case "occupation": member.setOccupation(value); break;
                case "education": member.setEducation(value); break;
                case "full_address": member.setFullAddress(value); break;
                case "mobile_number": member.setMobileNumber(value); break;
                case "member_since_year": member.setMemberSinceYear(Integer.parseInt(value)); break;
                case "membership_no": member.setMembershipNo(value); break;
                case "state_name_en": member.setStateNameEn(value); break;
                case "state_name_l1": member.setStateNameL1(value); break;
                case "state_name_l2": member.setStateNameL2(value); break;
                case "district_code": member.setDistrictCode(value); break;
                case "district_name_en": member.setDistrictNameEn(value); break;
                case "district_name_l1": member.setDistrictNameL1(value); break;
                case "district_name_l2": member.setDistrictNameL2(value); break;
                case "pc_no": member.setPcNo(value); break;
                case "pc_name_en": member.setPcNameEn(value); break;
                case "pc_name_l1": member.setPcNameL1(value); break;
                case "pc_name_l2": member.setPcNameL2(value); break;
                case "ac_no": member.setAcNo(value); break;
                case "ac_name_en": member.setAcNameEn(value); break;
                case "ac_name_l1": member.setAcNameL1(value); break;
                case "ac_name_l2": member.setAcNameL2(value); break;
                case "urban_no": member.setUrbanNo(value); break;
                case "urban_name_en": member.setUrbanNameEn(value); break;
                case "urban_name_l1": member.setUrbanNameL1(value); break;
                case "urban_ward_no": member.setUrbanWardNo(value); break;
                case "rur_district_union_no": member.setRurDistrictUnionNo(value); break;
                case "rur_district_union_name_en": member.setRurDistrictUnionNameEn(value); break;
                case "rur_district_union_name_l1": member.setRurDistrictUnionNameL1(value); break;
                case "rur_district_union_name_l2": member.setRurDistrictUnionNameL2(value); break;
                case "rur_district_union_ward_no": member.setRurDistrictUnionWardNo(value); break;
                case "pan_union_no": member.setPanUnionNo(value); break;
                case "pan_union_name_en": member.setPanUnionNameEn(value); break;
                case "pan_union_name_l1": member.setPanUnionNameL1(value); break;
                case "pan_union_name_l2": member.setPanUnionNameL2(value); break;
                case "pan_union_ward_no": member.setPanUnionWardNo(value); break;
                case "vill_pan_no": member.setVillPanNo(value); break;
                case "vill_pan_name_en": member.setVillPanNameEn(value); break;
                case "vill_pan_name_l1": member.setVillPanNameL1(value); break;
                case "vill_pan_ward_no": member.setVillPanWardNo(value); break;
                case "state_code": member.setStateCode(value); break;
                case "epic_number": member.setEpicNumber(value); break;
            }
        }
        return member;
    }
}
