package com.thedal.thedal_app.report.pollday.export;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import com.thedal.thedal_app.export.FamilyVoterCardHtmlPdfRenderer;
import com.thedal.thedal_app.report.pollday.export.PollDayExportJob.ExportStatus;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PollDayExportAsyncService {

    private final PollDayExportJobRepository jobRepository;
    private final VoterRepo voterRepository;
    private final AwsFileUpload awsFileUpload;
    private final FamilyVoterCardHtmlPdfRenderer htmlPdfRenderer;

    @Value("${aws.s3.files.bucket}")
    private String s3Filesbucket;

    private static final int BATCH_SIZE = 1000;
    private static final int EXCEL_WINDOW_SIZE = 100;

    /**
     * Process export job asynchronously
     * Uses @Transactional to keep Hibernate session open for lazy loading
     * EntityGraph in repository ensures relationships are eagerly fetched
     */
    @Async
    @Transactional
    public void execute(Long jobId) {
        log.info("Starting async processing for jobId={}", jobId);

        PollDayExportJob job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.error("Job not found: {}", jobId);
            return;
        }
        
        if (job.getStatus() != ExportStatus.PENDING) {
            log.warn("Job {} already processed with status {}", jobId, job.getStatus());
            return;
        }

        try {
            job.setStatus(ExportStatus.RUNNING);
            jobRepository.save(job);

            // Build specification based on filters
            Specification<VoterEntity> spec = buildSpecification(job);

            // Count total records
            long totalRecords = voterRepository.count(spec);
            log.info("JobId={}: Found {} records to export", jobId, totalRecords);

            if (totalRecords == 0) {
                job.setStatus(ExportStatus.COMPLETED);
                job.setRowCount(0);
                job.setFinishedAt(LocalDateTime.now());
                jobRepository.save(job);
                log.info("JobId={}: No records found, marked as completed", jobId);
                return;
            }

            // Generate file based on format
            File exportFile;
            if (job.getFormat() == PollDayExportJob.ExportFormat.excel) {
                exportFile = generateExcelFile(job, spec, totalRecords);
            } else {
                exportFile = generatePdfFile(job, spec, totalRecords);
            }

            // Upload to S3
            String fileName = String.format("pollday_export_%d_%d.%s", 
                jobId, System.currentTimeMillis(), 
                job.getFormat() == PollDayExportJob.ExportFormat.excel ? "xlsx" : "pdf");
            
            String s3Url = awsFileUpload.uploadToAWS(exportFile, fileName, s3Filesbucket);
            log.info("JobId={}: Uploaded to S3: {}", jobId, s3Url);

            // Update job with success
            job.setStatus(ExportStatus.COMPLETED);
            job.setS3Url(s3Url);
            job.setRowCount((int) totalRecords);
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);

            // Cleanup temp file
            if (exportFile.exists()) {
                exportFile.delete();
            }

            log.info("JobId={}: Export completed successfully", jobId);

        } catch (Exception e) {
            log.error("JobId={}: Export failed", jobId, e);
            job.setStatus(ExportStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : e.getClass().getName());
            job.setFinishedAt(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    /**
     * Generate Excel file with streaming
     */
    private File generateExcelFile(PollDayExportJob job, Specification<VoterEntity> spec, long totalRecords) throws Exception {
        log.info("JobId={}: Generating Excel file with {} records", job.getId(), totalRecords);

        File tempFile = File.createTempFile("pollday_export_", ".xlsx");
        
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(EXCEL_WINDOW_SIZE);
             FileOutputStream fos = new FileOutputStream(tempFile)) {

            Sheet sheet = workbook.createSheet("Voter Export");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Serial No", "Part No", "Name", "Age", "DOB", "Gender", 
                "Father/Guardian", "House No", "Party", "Religion", "Caste", "Sub-caste", 
                "Languages", "Mobile", "Poll Status"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Process data in batches
            int rowNum = 1;
            int page = 0;
            boolean hasMore = true;

            while (hasMore) {
                List<VoterEntity> batch = voterRepository.findAll(spec, 
                    PageRequest.of(page, BATCH_SIZE)).getContent();
                
                if (batch.isEmpty()) {
                    hasMore = false;
                } else {
                    for (VoterEntity voter : batch) {
                        Row row = sheet.createRow(rowNum++);
                        fillExcelRow(row, voter, job.getPollingDate() != null);
                    }
                    page++;
                }
            }

            workbook.write(fos);
            log.info("JobId={}: Excel file generated successfully", job.getId());
        }

        return tempFile;
    }

    /**
     * Fill Excel row with voter data
     */
    private void fillExcelRow(Row row, VoterEntity voter, boolean includeVotedStatus) {
        int col = 0;
        row.createCell(col++).setCellValue(voter.getSerialNo() != null ? String.valueOf(voter.getSerialNo()) : "");
        row.createCell(col++).setCellValue(voter.getPartNo() != null ? voter.getPartNo().doubleValue() : 0.0);
        
        String name = buildName(voter.getVoterFnameEn(), voter.getVoterLnameEn());
        row.createCell(col++).setCellValue(name);
        
        row.createCell(col++).setCellValue(voter.getAge() != null ? String.valueOf(voter.getAge()) : "");
        row.createCell(col++).setCellValue(voter.getDob() != null ? voter.getDob().toString() : "");
        row.createCell(col++).setCellValue(voter.getGender() != null ? voter.getGender() : "");
        
        String relName = buildName(voter.getRlnFnameEn(), voter.getRlnLnameEn());
        row.createCell(col++).setCellValue(relName);
        
        row.createCell(col++).setCellValue(voter.getHouseNoEn() != null ? voter.getHouseNoEn() : "");
        row.createCell(col++).setCellValue(voter.getParty() != null ? voter.getParty().getPartyName() : "");
        row.createCell(col++).setCellValue(voter.getReligion() != null ? voter.getReligion().getReligionName() : "");
        row.createCell(col++).setCellValue(voter.getCaste() != null ? voter.getCaste().getCasteName() : "");
        row.createCell(col++).setCellValue(voter.getSubCaste() != null ? voter.getSubCaste().getSubCasteName() : "");
        
        // Languages - comma separated
        String languages = voter.getLanguages() != null && !voter.getLanguages().isEmpty()
            ? voter.getLanguages().stream()
                .map(lang -> lang.getLanguageName())
                .reduce((a, b) -> a + ", " + b)
                .orElse("")
            : "";
        row.createCell(col++).setCellValue(languages);
        
        row.createCell(col++).setCellValue(voter.getMobileNo() != null ? voter.getMobileNo() : "");
        
        if (includeVotedStatus) {
            String votedStatus = voter.getHasVoted() != null && voter.getHasVoted() ? "Voted" : "Not Voted";
            row.createCell(col++).setCellValue(votedStatus);
        }
    }

    /**
     * Generate PDF file using HTML renderer (same as family export)
     */
    private File generatePdfFile(PollDayExportJob job, Specification<VoterEntity> spec, long totalRecords) throws Exception {
        log.info("JobId={}: Generating PDF file with {} records using HTML renderer", job.getId(), totalRecords);

        // Collect all voters
        List<VoterEntity> allVoters = new ArrayList<>();
        int page = 0;
        boolean hasMore = true;

        while (hasMore) {
            List<VoterEntity> batch = voterRepository.findAll(spec, PageRequest.of(page, BATCH_SIZE)).getContent();
            if (batch.isEmpty()) {
                hasMore = false;
            } else {
                allVoters.addAll(batch);
                page++;
            }
        }

        // Convert voters to maps for HTML renderer
        List<Map<String, Object>> voterMaps = new ArrayList<>();
        boolean includeVotedStatus = job.getPollingDate() != null;

        for (VoterEntity voter : allVoters) {
            Map<String, Object> voterMap = new HashMap<>();
            
            put(voterMap, "serialNo", voter.getSerialNo());
            put(voterMap, "partNo", voter.getPartNo());
            put(voterMap, "voterFnameEn", voter.getVoterFnameEn());
            put(voterMap, "voterLnameEn", voter.getVoterLnameEn());
            put(voterMap, "voterFnameL1", voter.getVoterFnameL1());
            put(voterMap, "voterLnameL1", voter.getVoterLnameL1());
            put(voterMap, "rlnType", voter.getRlnType());
            put(voterMap, "rlnFnameEn", voter.getRlnFnameEn());
            put(voterMap, "rlnLnameEn", voter.getRlnLnameEn());
            put(voterMap, "rlnFnameL1", voter.getRlnFnameL1());
            put(voterMap, "rlnLnameL1", voter.getRlnLnameL1());
            put(voterMap, "age", voter.getAge());
            put(voterMap, "gender", voter.getGender());
            put(voterMap, "dob", voter.getDob());
            put(voterMap, "houseNoEn", voter.getHouseNoEn());
            put(voterMap, "mobileNo", voter.getMobileNo());
            
            if (voter.getParty() != null) {
                put(voterMap, "party", voter.getParty());
            }
            if (voter.getReligion() != null) {
                put(voterMap, "religion", voter.getReligion());
            }
            if (voter.getCaste() != null) {
                put(voterMap, "caste", voter.getCaste());
            }
            
            if (includeVotedStatus) {
                put(voterMap, "hasVoted", voter.getHasVoted());
            }
            
            voterMaps.add(voterMap);
        }

        // Generate PDF using HTML renderer
        byte[] pdfBytes = htmlPdfRenderer.render(voterMaps, 2);

        // Write to temp file
        File tempFile = File.createTempFile("pollday_export_", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(pdfBytes);
        }

        log.info("JobId={}: PDF file generated successfully", job.getId());
        return tempFile;
    }

    /**
     * Helper to put non-null values in map
     */
    private void put(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Build specification based on job filters
     */
    private Specification<VoterEntity> buildSpecification(PollDayExportJob job) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            // Account and election filter
            predicate = cb.and(predicate, cb.equal(root.get("accountId"), job.getAccountId()));
            predicate = cb.and(predicate, cb.equal(root.get("electionId"), job.getElectionId()));

            // Part numbers filter
            if (job.getSelectedParts() != null && job.getSelectedParts().length > 0) {
                Expression<Integer> partNoExpression = root.get("partNo");
                CriteriaBuilder.In<Integer> inClause = cb.in(partNoExpression);
                for (int partNo : job.getSelectedParts()) {
                    inClause.value(partNo);
                }
                predicate = cb.and(predicate, inClause);
            }

            ExportFilters filters = job.getFilters();
            if (filters != null) {
                // Party filter
                if (filters.getParties() != null && !filters.getParties().isEmpty()) {
                    predicate = cb.and(predicate, root.get("party").get("id").in(filters.getParties()));
                }

                // Religion filter
                if (filters.getReligions() != null && !filters.getReligions().isEmpty()) {
                    predicate = cb.and(predicate, root.get("religion").get("id").in(filters.getReligions()));
                }

                // Gender filter
                if (filters.getGenders() != null && !filters.getGenders().isEmpty()) {
                    predicate = cb.and(predicate, root.get("gender").in(filters.getGenders()));
                }

                // Age range filter
                if (filters.getMinAge() != null || filters.getMaxAge() != null) {
                    Predicate agePredicate = cb.conjunction();
                    
                    if (filters.getMinAge() != null) {
                        agePredicate = cb.and(agePredicate, 
                            cb.greaterThanOrEqualTo(root.get("age"), filters.getMinAge().toString()));
                    }
                    if (filters.getMaxAge() != null) {
                        agePredicate = cb.and(agePredicate, 
                            cb.lessThanOrEqualTo(root.get("age"), filters.getMaxAge().toString()));
                    }

                    // Include unknown age if specified
                    if (filters.getIncludeUnknownAge() != null && filters.getIncludeUnknownAge()) {
                        agePredicate = cb.or(agePredicate, root.get("age").isNull());
                    }

                    predicate = cb.and(predicate, agePredicate);
                }

                // Caste category filter
                if (filters.getCasteCategories() != null && !filters.getCasteCategories().isEmpty()) {
                    predicate = cb.and(predicate, root.get("caste").get("casteCat").in(filters.getCasteCategories()));
                }

                // Caste filter
                if (filters.getCastes() != null && !filters.getCastes().isEmpty()) {
                    predicate = cb.and(predicate, root.get("caste").get("id").in(filters.getCastes()));
                }

                // Sub-caste filter
                if (filters.getSubCastes() != null && !filters.getSubCastes().isEmpty()) {
                    predicate = cb.and(predicate, root.get("subCaste").get("id").in(filters.getSubCastes()));
                }
            }

            return predicate;
        };
    }

    /**
     * Build full name from first and last name
     */
    private String buildName(String firstName, String lastName) {
        StringBuilder name = new StringBuilder();
        if (firstName != null && !firstName.trim().isEmpty()) {
            name.append(firstName);
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(lastName);
        }
        return name.length() > 0 ? name.toString() : "N/A";
    }
}
