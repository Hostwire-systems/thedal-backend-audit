package com.thedal.thedal_app.voter.duplicate;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DuplicateVoterExportAsyncService {
    private final VoterDuplicateRunRepository runRepo;
    private final VoterDuplicateMemberRepository memberRepo;
    private final DuplicateVoterExportJobRepository jobRepo;
    private final AwsFileUpload awsFileUpload;

    private static final String BUCKET = "thedalnew"; // TODO: externalize via property if needed
    private static final String PREFIX = "exports/duplicate-voters/";

    @Async
    public void execute(Long jobId) {
        DuplicateVoterExportJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return;
        if (job.getStatus() != DuplicateVoterExportJob.Status.PENDING) return;
        job.setStatus(DuplicateVoterExportJob.Status.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        jobRepo.save(job);
        try {
            VoterDuplicateRun run = runRepo.findById(job.getRunId()).orElse(null);
            if (run == null) throw new IllegalStateException("Run not found");
            List<DuplicateVoterExportRow> rows;
            if (job.hasMultipleParts()) {
                java.util.Set<Integer> parts = job.parsedPartNos();
                rows = parts.isEmpty() ? memberRepo.findExportRowsByRunId(run.getId())
                        : memberRepo.findExportRowsByRunIdAndParts(run.getId(), parts);
            } else if (job.getPartNo() != null) {
                rows = memberRepo.findExportRowsByRunIdAndPartNo(run.getId(), job.getPartNo());
            } else {
                rows = memberRepo.findExportRowsByRunId(run.getId());
            }
            File tempFile = File.createTempFile("dup_voters_",".xlsx");
            writeWorkbook(rows, tempFile);
            String key = buildKey(run.getElectionId(), run.getId(), job);
            String url = awsFileUpload.uploadToAWSForvoter(tempFile, key, BUCKET);
            job.setS3Key(key);
            job.setS3Url(url);
            job.setRowCount((long) rows.size());
            job.setStatus(DuplicateVoterExportJob.Status.COMPLETED);
        } catch (Exception ex) {
            log.error("Duplicate voter export job {} failed", jobId, ex);
            job.setStatus(DuplicateVoterExportJob.Status.FAILED);
            job.setErrorMessage(ex.getMessage());
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            jobRepo.save(job);
        }
    }

    private void writeWorkbook(List<DuplicateVoterExportRow> rows, File file) throws IOException {
        try (SXSSFWorkbook wb = new SXSSFWorkbook(100); FileOutputStream fos = new FileOutputStream(file)) {
            SXSSFSheet sheet = wb.createSheet("DuplicateVoters");
            String[] headers = {"Group ID","Group Size","Voter ID","First Name","Last Name","Relative First Name","Relative Last Name","Part No","Serial No","EPIC Number","Age","Gender","Section No"};
            int rowIdx = 0;
            var header = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (DuplicateVoterExportRow r : rows) {
                var excelRow = sheet.createRow(rowIdx++);
                int col = 0;
                excelRow.createCell(col++).setCellValue(safeLong(r.getGroupId()));
                excelRow.createCell(col++).setCellValue(safeInt(r.getGroupSize()));
                excelRow.createCell(col++).setCellValue(safeLong(r.getVoterId()));
                excelRow.createCell(col++).setCellValue(nz(r.getVoterFnameEn()));
                excelRow.createCell(col++).setCellValue(nz(r.getVoterLnameEn()));
                excelRow.createCell(col++).setCellValue(nz(r.getRlnFnameEn()));
                excelRow.createCell(col++).setCellValue(nz(r.getRlnLnameEn()));
                excelRow.createCell(col++).setCellValue(safeInt(r.getPartNo()));
                excelRow.createCell(col++).setCellValue(safeLong(r.getSerialNo()));
                excelRow.createCell(col++).setCellValue(nz(r.getEpicNumber()));
                excelRow.createCell(col++).setCellValue(safeInt(r.getAge()));
                excelRow.createCell(col++).setCellValue(nz(r.getGender()));
                excelRow.createCell(col++).setCellValue(safeInt(r.getSectionNo()));
            }
            wb.write(fos);
        }
    }

    private String buildKey(Long electionId, Long runId, DuplicateVoterExportJob job) {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        String suffix;
        if (job.hasMultipleParts()) {
            // shorten filename by limiting parts list length
            java.util.Set<Integer> parts = job.parsedPartNos();
            String joined = parts.stream().limit(5).map(String::valueOf).collect(java.util.stream.Collectors.joining("-"));
            suffix = "_parts" + joined + (parts.size() > 5 ? "+" : "");
        } else if (job.getPartNo() != null) {
            suffix = "_part" + job.getPartNo();
        } else {
            suffix = "";
        }
        String base = "e" + electionId + "_run" + runId + suffix;
        return PREFIX + base + "_" + ts + ".xlsx";
    }

    private String nz(String v) { return v == null ? "" : v; }
    private long safeLong(Long v) { return v == null ? 0L : v; }
    private int safeInt(Integer v) { return v == null ? 0 : v; }
}
