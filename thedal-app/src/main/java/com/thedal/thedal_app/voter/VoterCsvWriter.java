package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight CSV builder for voter export.
 * Keeps formatting simple (RFC 4180 style) and relies on VoterColumnMapper for value resolution.
 * Supports both standard and dynamic custom fields.
 */
final class VoterCsvWriter {

    private VoterCsvWriter() {
    }

    /**
     * Resolve requested columns and merge with dynamic fields to create the canonical ordered list.
     */
    static List<String> resolveColumns(List<String> requestedColumns, List<String> dynamicFieldNames) {
        List<String> standardFields = VoterColumnMapper.validateAndFilterFields(requestedColumns);
        return VoterColumnMapper.mergeStandardAndDynamicFields(standardFields, dynamicFieldNames);
    }

    static String header(List<String> columns) {
        return columns.stream()
                .map(VoterColumnMapper::getExcelHeader)
                .map(VoterCsvWriter::escape)
                .collect(Collectors.joining(","));
    }

    static String row(VoterEntity voter, List<String> columns) {
        return columns.stream()
                .map(column -> VoterColumnMapper.getFieldValue(voter, column))
                .map(VoterCsvWriter::escape)
                .collect(Collectors.joining(","));
    }

    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        boolean needsQuoting = text.contains(",") || text.contains("\n") || text.contains("\r") || text.contains("\"");
        if (needsQuoting) {
            text = '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
