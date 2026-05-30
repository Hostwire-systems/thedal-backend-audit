package com.thedal.thedal_app.sirreport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Ultra-fast bulk loader using PostgreSQL COPY command
 * 10-20x faster than batch INSERT statements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostgresCopyBulkLoader {
    
    private final DataSource dataSource;
    
    /**
     * Bulk load data using PostgreSQL COPY command (50k-200k rows/sec)
     * 
     * @param csvData CSV formatted data (job_id,epic_number,part_no)
     * @param tableName Target table name
     * @param rowCount Expected row count for logging
     * @return Actual rows loaded
     */
    public long bulkLoadFromCsv(String csvData, String tableName, long rowCount) throws SQLException, IOException {
        long startTime = System.currentTimeMillis();
        
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false); // Disable auto-commit for explicit control
            
            Reader reader = new StringReader(csvData);
            
            // Unwrap HikariCP proxy to get the actual PostgreSQL connection
            Connection unwrappedConn = conn.unwrap(org.postgresql.core.BaseConnection.class);
            CopyManager copyManager = new CopyManager((BaseConnection) unwrappedConn);
            
            String copyCommand = String.format(
                "COPY %s (job_id, epic_number, part_no, voter_name_en, serial_no, section_no, house_no_en, age, gender) FROM STDIN WITH (FORMAT CSV, HEADER false)",
                tableName
            );
            
            long loaded = copyManager.copyIn(copyCommand, reader);
            
            // CRITICAL: Explicitly commit the transaction
            conn.commit();
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("Bulk loaded {} rows to {} in {} ms ({} rows/sec)", 
                loaded, tableName, duration, loaded * 1000 / Math.max(duration, 1));
            
            return loaded;
        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.error("Transaction rolled back due to error", e);
                } catch (SQLException rollbackEx) {
                    log.error("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    log.error("Failed to close connection", closeEx);
                }
            }
        }
    }
    
    /**
     * Convert Excel rows to CSV format in memory-efficient streaming manner
     * 
     * @param file Excel file
     * @param jobId Job UUID
     * @return CSV string
     */
    public String convertExcelToCsv(File file, UUID jobId, ExcelReaderService excelReaderService) throws IOException {
        StringBuilder csv = new StringBuilder(50_000_000); // Pre-allocate ~50MB for 1M rows
        long rowCount = 0;
        long skippedRows = 0;
        
        try (var workbook = com.github.pjfanning.xlsx.StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(file)) {
            
            var sheet = workbook.getSheetAt(0);
            var rowIterator = sheet.iterator();
            
            // Read header
            if (!rowIterator.hasNext()) {
                throw new IOException("Excel file is empty");
            }
            var headerRow = rowIterator.next();
            var columnIndices = excelReaderService.findColumnIndices(headerRow);
            
            log.info("Detected columns: {}", columnIndices);
            
            // Validate required columns
            if (!columnIndices.containsKey("EPIC")) {
                throw new IOException("EPIC column not found in Excel file");
            }
            if (!columnIndices.containsKey("PART_NO")) {
                throw new IOException("PART_NO column not found in Excel file");
            }
            
            // Process rows
            while (rowIterator.hasNext()) {
                var row = rowIterator.next();
                
                try {
                    var voter = excelReaderService.extractVoterFromRow(row, columnIndices, file.getName());
                    if (voter != null && voter.getEpicNumber() != null && voter.getPartNo() != null) {
                        // CSV format: job_id,epic_number,part_no,voter_name_en,serial_no,section_no,house_no_en,age,gender
                        csv.append(jobId).append(',')
                           .append(escapeCsv(voter.getEpicNumber())).append(',')
                           .append(voter.getPartNo()).append(',')
                           .append(escapeCsv(voter.getVoterNameEn())).append(',')
                           .append(voter.getSerialNo() != null ? voter.getSerialNo() : "").append(',')
                           .append(voter.getSectionNo() != null ? voter.getSectionNo() : "").append(',')
                           .append(escapeCsv(voter.getHouseNoEn())).append(',')
                           .append(voter.getAge() != null ? voter.getAge() : "").append(',')
                           .append(escapeCsv(voter.getGender())).append('\n');
                        rowCount++;
                        
                        if (rowCount % 50000 == 0) {
                            log.info("Converted {} rows to CSV (skipped: {})", rowCount, skippedRows);
                        }
                    } else {
                        skippedRows++;
                        if (skippedRows <= 5) { // Log first 5 skipped rows
                            log.warn("Skipped row - epic: {}, part_no: {}", 
                                voter != null ? voter.getEpicNumber() : "null",
                                voter != null ? voter.getPartNo() : "null");
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error converting row: {}", e.getMessage());
                }
            }
        }
        
        log.info("CSV conversion complete - Total rows: {}, Skipped rows: {}", rowCount, skippedRows);
        
        if (rowCount == 0) {
            throw new IOException("No valid rows found in Excel file. Check column headers and data format.");
        }
        
        return csv.toString();
    }
    
    /**
     * Escape CSV values
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
