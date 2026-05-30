package com.thedal.thedal_app.voter;

import org.apache.poi.ss.usermodel.Row;
import java.util.List;

public class VoterExcelHeader {
	
	/**
	 * Create header row with all 93 default columns
	 */
	public static void createHeaderRow(Row headerRow) {
        int colNum = 0;

        // Initial static fields (0-36)
        headerRow.createCell(colNum++).setCellValue("PART_NO");
        headerRow.createCell(colNum++).setCellValue("SECTION_NO");
        headerRow.createCell(colNum++).setCellValue("SERIAL_NO");
        headerRow.createCell(colNum++).setCellValue("HOUSE_NO_EN");
        headerRow.createCell(colNum++).setCellValue("HOUSE_NO_L1");
        headerRow.createCell(colNum++).setCellValue("HOUSE_NO_L2");
        headerRow.createCell(colNum++).setCellValue("VOTER_FNAME_EN");
        headerRow.createCell(colNum++).setCellValue("VOTER_LNAME_EN");
        headerRow.createCell(colNum++).setCellValue("VOTER_FNAME_L1");
        headerRow.createCell(colNum++).setCellValue("VOTER_LNAME_L1");
        headerRow.createCell(colNum++).setCellValue("VOTER_FNAME_L2");
        headerRow.createCell(colNum++).setCellValue("VOTER_LNAME_L2");
        headerRow.createCell(colNum++).setCellValue("RLN_FNAME_EN");
        headerRow.createCell(colNum++).setCellValue("RLN_LNAME_EN");
        headerRow.createCell(colNum++).setCellValue("RLN_FNAME_L1");
        headerRow.createCell(colNum++).setCellValue("RLN_LNAME_L1");
        headerRow.createCell(colNum++).setCellValue("RLN_FNAME_L2");
        headerRow.createCell(colNum++).setCellValue("RLN_LNAME_L2");
        headerRow.createCell(colNum++).setCellValue("RLN_TYPE");
        headerRow.createCell(colNum++).setCellValue("EPIC_NUMBER");
        headerRow.createCell(colNum++).setCellValue("GENDER");
        headerRow.createCell(colNum++).setCellValue("SECTION_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("SECTION_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("SECTION_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("FULL_ADDRESS");
        headerRow.createCell(colNum++).setCellValue("PART_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("PART_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("PART_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("PINCODE");
        headerRow.createCell(colNum++).setCellValue("PART_LATI");
        headerRow.createCell(colNum++).setCellValue("PART_LONG");
        headerRow.createCell(colNum++).setCellValue("AGE");
        headerRow.createCell(colNum++).setCellValue("DOB");
        headerRow.createCell(colNum++).setCellValue("MOBILE_NO");
        headerRow.createCell(colNum++).setCellValue("WHATSAPP_NO");
        headerRow.createCell(colNum++).setCellValue("E-MAIL");
        headerRow.createCell(colNum++).setCellValue("VOTER_LATI");
        headerRow.createCell(colNum++).setCellValue("VOTER_LONGI");

        // Remaining static fields (37-63)
        headerRow.createCell(colNum++).setCellValue("STATE_CODE");
        headerRow.createCell(colNum++).setCellValue("STATE_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("STATE_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("STATE_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("DISTRICT_CODE");
        headerRow.createCell(colNum++).setCellValue("DISTRICT_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("DISTRICT_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("DISTRICT_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("PC_NO");
        headerRow.createCell(colNum++).setCellValue("PC_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("PC_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("PC_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("AC_NO");
        headerRow.createCell(colNum++).setCellValue("AC_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("AC_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("AC_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("URBAN_NO");
        headerRow.createCell(colNum++).setCellValue("URBAN_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("URBAN_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("URBAN_WARD_NO");
        headerRow.createCell(colNum++).setCellValue("RUR_DISTRICT_UNION_NO");
        headerRow.createCell(colNum++).setCellValue("RUR_DISTRICT_UNION_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("RUR_DISTRICT_UNION_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("RUR_DISTRICT_UNION_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("RUR_DISTRICT_UNION_WARD_NO");
        headerRow.createCell(colNum++).setCellValue("PAN_UNION_NO");
        headerRow.createCell(colNum++).setCellValue("PAN_UNION_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("PAN_UNION_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("PAN_UNION_NAME_L2");
        headerRow.createCell(colNum++).setCellValue("PAN_UNION_WARD_NO");
        headerRow.createCell(colNum++).setCellValue("VILL_PAN_NO");
        headerRow.createCell(colNum++).setCellValue("VILL_PAN_NAME_EN");
        headerRow.createCell(colNum++).setCellValue("VILL_PAN_NAME_L1");
        headerRow.createCell(colNum++).setCellValue("VILL_PAN_WARD_NO");

        // Dynamic fields (64-73)
        headerRow.createCell(colNum++).setCellValue("RELIGION_NAME");
        headerRow.createCell(colNum++).setCellValue("CASTE_NAME");
        headerRow.createCell(colNum++).setCellValue("SUB_CASTE_NAME");
        headerRow.createCell(colNum++).setCellValue("LANGUAGES");
        headerRow.createCell(colNum++).setCellValue("BENEFIT_SCHEMES_NAME");
        headerRow.createCell(colNum++).setCellValue("BENEFIT_SCHEMES_BY");
        headerRow.createCell(colNum++).setCellValue("SCHEME");
        headerRow.createCell(colNum++).setCellValue("AVAILABILITY_DESCRIPTION");
        headerRow.createCell(colNum++).setCellValue("AVAILABILITY_CATEGORY_NAME");
        headerRow.createCell(colNum++).setCellValue("PARTY_NAME");
        headerRow.createCell(colNum++).setCellValue("PARTY_SHORT_NAME");
        headerRow.createCell(colNum++).setCellValue("FAMILY_ID");
        headerRow.createCell(colNum++).setCellValue("FAMILY_COUNT");
        headerRow.createCell(colNum++).setCellValue("FEEDBACK_ISSUE_NAMES");
        headerRow.createCell(colNum++).setCellValue("VOTER_HISTORY_NAMES");
        
        headerRow.createCell(colNum++).setCellValue("STAR_NUMBER");
        headerRow.createCell(colNum++).setCellValue("AADHAAR_NUMBER");
        headerRow.createCell(colNum++).setCellValue("PAN_NUMBER");
        headerRow.createCell(colNum++).setCellValue("PARTY_REGISTRATION_NUMBER");
        headerRow.createCell(colNum++).setCellValue("PAGE_NUMBER");
        headerRow.createCell(colNum++).setCellValue("REMARKS");
        headerRow.createCell(colNum++).setCellValue("CASTE_CATEGORY_NAME");
        headerRow.createCell(colNum++).setCellValue("AADHAAR_VERIFIED");
        headerRow.createCell(colNum++).setCellValue("PHOTO_URL");
        headerRow.createCell(colNum++).setCellValue("FRIEND_COUNT");
        
    }
    
    /**
     * Create selective header row with only specified columns
     * @param headerRow The Excel row to populate with headers
     * @param columnFields List of field names to include (e.g., ["epicNumber", "voterFnameEn"])
     */
    public static void createSelectiveHeaderRow(Row headerRow, List<String> columnFields) {
        if (columnFields == null || columnFields.isEmpty()) {
            // Fallback to all columns
            createHeaderRow(headerRow);
            return;
        }
        
        int colNum = 0;
        for (String fieldName : columnFields) {
            String headerName = VoterColumnMapper.getExcelHeader(fieldName);
            headerRow.createCell(colNum++).setCellValue(headerName);
        }
    }

}
