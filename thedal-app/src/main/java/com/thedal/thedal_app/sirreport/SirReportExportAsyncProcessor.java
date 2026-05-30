package com.thedal.thedal_app.sirreport;

import com.thedal.thedal_app.awsfilestore.AwsFileUpload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SirReportExportAsyncProcessor {
    
    private final SirReportExportJobRepository exportJobRepository;
    private final SirReportAdditionRepository additionRepository;
    private final SirReportDeletionRepository deletionRepository;
    private final SirReportShiftRepository shiftRepository;
    private final SirReportJobRepository jobRepository;
    private final AwsFileUpload awsFileUpload;
    
    @Value("${aws.s3.export.bucket}")
    private String s3ExportBucket;
    
    private static final int BATCH_SIZE = 1000;
    
    @Async("taskExecutor")
    public void processExportAsync(Long exportJobId) {
        SirReportExportJob exportJob = exportJobRepository.findById(exportJobId)
                .orElseThrow(() -> new RuntimeException("Export job not found: " + exportJobId));
        
        try {
            log.info("Starting export processing for job: {}", exportJobId);
            exportJob.setStatus("PROCESSING");
            exportJob.setMessage("Generating export file...");
            exportJobRepository.save(exportJob);
            
            byte[] fileContent;
            String contentType;
            String fileExtension;
            
            if (exportJob.getFormat() == SirReportExportFormat.EXCEL) {
                fileContent = generateExcelExport(exportJob);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                fileExtension = "xlsx";
            } else {
                fileContent = generatePdfExport(exportJob);
                contentType = "application/pdf";
                fileExtension = "pdf";
            }
            
            // Upload to S3
            String fileName = String.format("sir-exports/%s/%s_%s_%s.%s",
                    exportJob.getAccountId(),
                    exportJob.getExportType().name().toLowerCase(),
                    exportJob.getJobId(),
                    System.currentTimeMillis(),
                    fileExtension);
            
            awsFileUpload.uploadBytesToAWS(fileContent, fileName, s3ExportBucket, contentType);
            
            // Generate pre-signed URL valid for 24 hours (matching expires_at)
            String s3Url = awsFileUpload.generatePresignedUrl(s3ExportBucket, fileName, 24 * 3600);
            
            // Update job as completed
            exportJob.setStatus("COMPLETED");
            exportJob.setMessage("Export completed successfully");
            exportJob.setAwsS3DownloadUrl(s3Url);
            exportJob.setTimeCompleted(LocalDateTime.now());
            exportJobRepository.save(exportJob);
            
            log.info("Export job {} completed successfully. S3 URL: {}", exportJobId, s3Url);
            
        } catch (Exception e) {
            log.error("Error processing export job: {}", exportJobId, e);
            exportJob.setStatus("FAILED");
            exportJob.setMessage("Export failed: " + e.getMessage());
            exportJob.setTimeCompleted(LocalDateTime.now());
            exportJobRepository.save(exportJob);
        }
    }
    
    private byte[] generateExcelExport(SirReportExportJob exportJob) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet(exportJob.getExportType().name());
            
            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            
            // Get data and record count
            int totalRecords = getTotalRecords(exportJob);
            exportJob.setRecordCount(totalRecords);
            
            // Create headers
            Row headerRow = sheet.createRow(0);
            String[] headers = getHeaders(exportJob.getExportType());
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // Fetch and write data in batches
            int rowNum = 1;
            int pageNum = 0;
            
            while (true) {
                Page<?> page = fetchDataPage(exportJob, PageRequest.of(pageNum, BATCH_SIZE));
                if (page.isEmpty()) break;
                
                for (Object record : page.getContent()) {
                    Row row = sheet.createRow(rowNum++);
                    writeRecordToRow(row, record, exportJob.getExportType());
                }
                
                if (page.isLast()) break;
                pageNum++;
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        }
    }
    
    private byte[] generatePdfExport(SirReportExportJob exportJob) throws Exception {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Add title
            Paragraph title = new Paragraph("SIR Report - " + exportJob.getExportType().name())
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            // Add metadata
            SirReportJobEntity sirJob = jobRepository.findByJobId(exportJob.getJobId())
                    .orElseThrow(() -> new RuntimeException("SIR job not found"));
            
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
            document.add(new Paragraph("Base File: " + sirJob.getBaseFileName()));
            document.add(new Paragraph("New File: " + sirJob.getNewFileName()));
            document.add(new Paragraph(" "));
            
            // Get total records
            int totalRecords = getTotalRecords(exportJob);
            exportJob.setRecordCount(totalRecords);
            
            // Create table
            String[] headers = getHeaders(exportJob.getExportType());
            Table table = new Table(UnitValue.createPercentArray(headers.length)).useAllAvailableWidth();
            
            // Add headers
            for (String header : headers) {
                table.addHeaderCell(header);
            }
            
            // Fetch and write data in batches
            int pageNum = 0;
            
            while (true) {
                Page<?> page = fetchDataPage(exportJob, PageRequest.of(pageNum, BATCH_SIZE));
                if (page.isEmpty()) break;
                
                for (Object record : page.getContent()) {
                    addRecordToTable(table, record, exportJob.getExportType());
                }
                
                if (page.isLast()) break;
                pageNum++;
            }
            
            document.add(table);
            document.close();
            
            return out.toByteArray();
        }
    }
    
    private String[] getHeaders(SirReportExportType type) {
        if (type == SirReportExportType.SHIFTS) {
            return new String[]{"EPIC Number", "Name", "Old Part No", "New Part No", "Serial No", "Section No", "House No", "Age", "Gender"};
        } else {
            return new String[]{"EPIC Number", "Name", "Part No", "Serial No", "Section No", "House No", "Age", "Gender"};
        }
    }
    
    private int getTotalRecords(SirReportExportJob exportJob) {
        switch (exportJob.getExportType()) {
            case ADDITIONS:
                return additionRepository.countByJobId(exportJob.getJobId()).intValue();
            case DELETIONS:
                return deletionRepository.countByJobId(exportJob.getJobId()).intValue();
            case SHIFTS:
                return shiftRepository.countByJobId(exportJob.getJobId()).intValue();
            default:
                return 0;
        }
    }
    
    private Page<?> fetchDataPage(SirReportExportJob exportJob, Pageable pageable) {
        switch (exportJob.getExportType()) {
            case ADDITIONS:
                return additionRepository.findByJobId(exportJob.getJobId(), pageable);
            case DELETIONS:
                return deletionRepository.findByJobId(exportJob.getJobId(), pageable);
            case SHIFTS:
                return shiftRepository.findByJobId(exportJob.getJobId(), pageable);
            default:
                throw new IllegalArgumentException("Invalid export type");
        }
    }
    
    private void writeRecordToRow(Row row, Object record, SirReportExportType type) {
        int cellNum = 0;
        
        if (type == SirReportExportType.SHIFTS) {
            SirReportShiftEntity shift = (SirReportShiftEntity) record;
            row.createCell(cellNum++).setCellValue(shift.getEpicNumber());
            row.createCell(cellNum++).setCellValue(shift.getVoterNameEn());
            row.createCell(cellNum++).setCellValue(shift.getOldPartNo());
            row.createCell(cellNum++).setCellValue(shift.getNewPartNo());
            row.createCell(cellNum++).setCellValue(shift.getSerialNo());
            row.createCell(cellNum++).setCellValue(shift.getSectionNo());
            row.createCell(cellNum++).setCellValue(shift.getHouseNoEn());
            row.createCell(cellNum++).setCellValue(shift.getAge());
            row.createCell(cellNum++).setCellValue(shift.getGender());
        } else if (type == SirReportExportType.ADDITIONS) {
            SirReportAdditionEntity addition = (SirReportAdditionEntity) record;
            row.createCell(cellNum++).setCellValue(addition.getEpicNumber());
            row.createCell(cellNum++).setCellValue(addition.getVoterNameEn());
            row.createCell(cellNum++).setCellValue(addition.getPartNo());
            row.createCell(cellNum++).setCellValue(addition.getSerialNo());
            row.createCell(cellNum++).setCellValue(addition.getSectionNo());
            row.createCell(cellNum++).setCellValue(addition.getHouseNoEn());
            row.createCell(cellNum++).setCellValue(addition.getAge());
            row.createCell(cellNum++).setCellValue(addition.getGender());
        } else {
            SirReportDeletionEntity deletion = (SirReportDeletionEntity) record;
            row.createCell(cellNum++).setCellValue(deletion.getEpicNumber());
            row.createCell(cellNum++).setCellValue(deletion.getVoterNameEn());
            row.createCell(cellNum++).setCellValue(deletion.getPartNo());
            row.createCell(cellNum++).setCellValue(deletion.getSerialNo());
            row.createCell(cellNum++).setCellValue(deletion.getSectionNo());
            row.createCell(cellNum++).setCellValue(deletion.getHouseNoEn());
            row.createCell(cellNum++).setCellValue(deletion.getAge());
            row.createCell(cellNum++).setCellValue(deletion.getGender());
        }
    }
    
    private void addRecordToTable(Table table, Object record, SirReportExportType type) {
        if (type == SirReportExportType.SHIFTS) {
            SirReportShiftEntity shift = (SirReportShiftEntity) record;
            table.addCell(shift.getEpicNumber());
            table.addCell(shift.getVoterNameEn());
            table.addCell(String.valueOf(shift.getOldPartNo()));
            table.addCell(String.valueOf(shift.getNewPartNo()));
            table.addCell(String.valueOf(shift.getSerialNo()));
            table.addCell(String.valueOf(shift.getSectionNo()));
            table.addCell(shift.getHouseNoEn());
            table.addCell(String.valueOf(shift.getAge()));
            table.addCell(shift.getGender());
        } else if (type == SirReportExportType.ADDITIONS) {
            SirReportAdditionEntity addition = (SirReportAdditionEntity) record;
            table.addCell(addition.getEpicNumber() != null ? addition.getEpicNumber() : "");
            table.addCell(addition.getVoterNameEn() != null ? addition.getVoterNameEn() : "");
            table.addCell(String.valueOf(addition.getPartNo()));
            table.addCell(String.valueOf(addition.getSerialNo()));
            table.addCell(String.valueOf(addition.getSectionNo()));
            table.addCell(addition.getHouseNoEn() != null ? addition.getHouseNoEn() : "");
            table.addCell(String.valueOf(addition.getAge()));
            table.addCell(addition.getGender() != null ? addition.getGender() : "");
        } else {
            SirReportDeletionEntity deletion = (SirReportDeletionEntity) record;
            table.addCell(deletion.getEpicNumber() != null ? deletion.getEpicNumber() : "");
            table.addCell(deletion.getVoterNameEn() != null ? deletion.getVoterNameEn() : "");
            table.addCell(String.valueOf(deletion.getPartNo()));
            table.addCell(String.valueOf(deletion.getSerialNo()));
            table.addCell(String.valueOf(deletion.getSectionNo()));
            table.addCell(deletion.getHouseNoEn() != null ? deletion.getHouseNoEn() : "");
            table.addCell(String.valueOf(deletion.getAge()));
            table.addCell(deletion.getGender() != null ? deletion.getGender() : "");
        }
    }
}
