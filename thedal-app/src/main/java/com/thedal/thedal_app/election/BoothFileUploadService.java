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

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.BulkUploadStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BoothFileUploadService {

    @Autowired
    private BoothBulkUploadRepository boothBulkUploadRepository;
    
    @Autowired
    private ElectionBoothRepository  electionBoothRepository;


    @Transactional
    public void processBoothExcelFileAsync(Long bulkUploadId, AccountEntity account, String fileUrl, ElectionEntity election, BoothBulkUploadEntity bulkUploadEntity) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(new URL(fileUrl).openStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Map<String, Integer> headerMapping = buildHeaderMapping(sheet.getRow(0));
            processBoothFileAsync(sheet.iterator(), headerMapping, account, election, bulkUploadEntity);
        } catch (Exception e) {
            log.error("Error processing Excel file: ", e);
            throw new ThedalException(ThedalError.FILE_PROCESSING_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Integer> buildHeaderMapping(Row headerRow) {
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
        String normalized = header.trim()
                                 .replaceAll("[^a-zA-Z0-9]", "_")
                                 .replaceAll("_+", "_")
                                 .toLowerCase();
        log.debug("Normalized header: {} -> {}", header, normalized);
        return normalized;
    }

    @Transactional
    public void processBoothCsvFileAsync(Long bulkUploadId, AccountEntity account, String fileUrl, ElectionEntity election, BoothBulkUploadEntity bulkUploadEntity) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            String[] headers = br.readLine().split(",");
            Map<String, Integer> headerMapping = buildCsvHeaderMapping(headers);
            processBoothFileAsync(br.lines().iterator(), headerMapping, account, election, bulkUploadEntity);
        } catch (Exception e) {
            log.error("Error processing CSV file: ", e);
            throw new ThedalException(ThedalError.FILE_PROCESSING_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Integer> buildCsvHeaderMapping(String[] headers) {
        Map<String, Integer> headerMapping = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalizedHeader = normalizeHeader(headers[i]);
            headerMapping.put(normalizedHeader, i);
        }
        return headerMapping;
    }

    @Async
    @Transactional
    public void processBoothFileAsync(Iterator<?> rowIterator, Map<String, Integer> headerMapping, AccountEntity account, ElectionEntity election, BoothBulkUploadEntity bulkUploadEntity) {
        List<ElectionBooth> booths = new ArrayList<>();

        while (rowIterator.hasNext()) {
            Object rowObj = rowIterator.next();

            if (rowObj instanceof Row) {
                // Handle Excel row (Row)
                Row row = (Row) rowObj;
                if (row.getRowNum() == 0) continue; // Skip header row

                Integer boothNumberIndex = headerMapping.get("booth_number");
                Integer vulnerabilityIndex = headerMapping.get("booth_vulnerability");

                if (boothNumberIndex == null || vulnerabilityIndex == null) {
                    throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
                }

                int boothNumber = (int) row.getCell(boothNumberIndex).getNumericCellValue();
                String boothVulnerability = row.getCell(vulnerabilityIndex).getStringCellValue();

                ElectionBooth booth = new ElectionBooth();
                booth.setElection(election);
                booth.setAccountId(account.getId());
                booth.setBoothNumber(boothNumber);
                booth.setBoothVulnerability(boothVulnerability);
                booths.add(booth);
            } else if (rowObj instanceof CsvRow) {
                // Handle CSV row (CsvRow)
                CsvRow row = (CsvRow) rowObj;

                Integer boothNumberIndex = headerMapping.get("booth_number");
                Integer vulnerabilityIndex = headerMapping.get("booth_vulnerability");

                if (boothNumberIndex == null || vulnerabilityIndex == null) {
                    throw new ThedalException(ThedalError.INVALID_FILE_FORMAT, HttpStatus.BAD_REQUEST);
                }

                int boothNumber = Integer.parseInt(row.getCell(boothNumberIndex));
                String boothVulnerability = row.getCell(vulnerabilityIndex);

                ElectionBooth booth = new ElectionBooth();
                booth.setElection(election);
                booth.setAccountId(account.getId());
                booth.setBoothNumber(boothNumber);
                booth.setBoothVulnerability(boothVulnerability);
                booths.add(booth);
            }
        }

        // Bulk insert booths
        electionBoothRepository.saveAll(booths);

        // Update upload entity status
        bulkUploadEntity.setStatus(BulkUploadStatus.COMPLETED);
        bulkUploadEntity.setEndTime(LocalDateTime.now());
        boothBulkUploadRepository.save(bulkUploadEntity);

        log.info("Successfully uploaded {} booths.", booths.size());
    }
}




