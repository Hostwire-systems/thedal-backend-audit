package com.thedal.thedal_app.voter.duplicate;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DuplicateVoterExcelExportService {
    private final VoterDuplicateRunRepository runRepo;
    private final VoterDuplicateMemberRepository memberRepo;

    public void export(Long accountId, Long electionId, Long runId, Integer partNo, HttpServletResponse response) throws IOException {
        VoterDuplicateRun run = resolveRun(accountId, electionId, runId);
        if (run == null) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        // Basic safety check
        if (!run.getAccountId().equals(accountId) || !run.getElectionId().equals(electionId)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Run does not match account/election");
            return;
        }
        List<DuplicateVoterExportRow> rows = partNo != null ?
                memberRepo.findExportRowsByRunIdAndPartNo(run.getId(), partNo) :
                memberRepo.findExportRowsByRunId(run.getId());

        String filename = buildFilename(electionId, run.getId(), partNo);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        SXSSFWorkbook wb = new SXSSFWorkbook(100); // keep 100 rows in memory
        try {
            SXSSFSheet sheet = wb.createSheet("DuplicateVoters");
            String[] headers = {"Group ID","Group Size","Voter ID","First Name","Last Name","Relative First Name","Relative Last Name","Part No","Serial No","EPIC Number","Age","Gender","Section No"};
            int rowIdx = 0;
            Row header = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(headers[i]);
            }
            for (DuplicateVoterExportRow r : rows) {
                Row excelRow = sheet.createRow(rowIdx++);
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
            // Optional: autosize limited columns (can be expensive on huge sheets)
            for (int i = 0; i < headers.length && i < 14; i++) {
                sheet.trackAllColumnsForAutoSizing();
                sheet.autoSizeColumn(i);
            }
            wb.write(response.getOutputStream());
            response.flushBuffer();
        } finally {
            wb.dispose();
        }
    }

    private VoterDuplicateRun resolveRun(Long accountId, Long electionId, Long runId) {
        if (runId != null) {
            return runRepo.findById(runId).orElse(null);
        }
        return runRepo.findTopByAccountIdAndElectionIdOrderByStartedAtDesc(accountId, electionId).orElse(null);
    }

    private String buildFilename(Long electionId, Long runId, Integer partNo) {
        String base = "duplicate_voters_e" + electionId + "_run" + runId;
        if (partNo != null) base += "_part" + partNo;
        base += "_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(java.time.LocalDateTime.now()) + ".xlsx";
        return URLEncoder.encode(base, StandardCharsets.UTF_8);
    }

    private String nz(String v) { return v == null ? "" : v; }
    private long safeLong(Long v) { return v == null ? 0L : v; }
    private int safeInt(Integer v) { return v == null ? 0 : v; }
}
