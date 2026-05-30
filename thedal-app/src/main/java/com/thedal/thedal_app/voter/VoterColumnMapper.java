package com.thedal.thedal_app.voter;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for mapping voter field names to Excel headers and extracting field values.
 * Supports selective column exports by providing bidirectional mappings.
 */
@Slf4j
public class VoterColumnMapper {

    private static final Map<String, String> FIELD_TO_HEADER = new LinkedHashMap<>();
    private static final Map<String, Integer> FIELD_TO_INDEX = new LinkedHashMap<>();
    private static final List<String> ALL_FIELD_NAMES = new ArrayList<>();
    private static final Map<String, String> FIELD_ALIASES = new HashMap<>();
    
    static {
        initializeFieldMappings();
        initializeFieldAliases();
    }
    
    /**
     * Initialize common field aliases for user convenience
     */
    private static void initializeFieldAliases() {
        // Religion aliases
        FIELD_ALIASES.put("religion", "religionName");
        
        // Caste aliases
        FIELD_ALIASES.put("caste", "casteName");
        FIELD_ALIASES.put("subCaste", "subCasteName");
        FIELD_ALIASES.put("casteCategory", "casteCategoryName");
        
        // Party aliases
        FIELD_ALIASES.put("party", "partyName");
        
        // Scheme aliases (already correct as "scheme")
        
        // Availability aliases
        FIELD_ALIASES.put("availability", "availabilityDescription");
        
        // Voter history aliases
        FIELD_ALIASES.put("voterHistory", "voterHistoryNames");
        
        // Feedback aliases
        FIELD_ALIASES.put("feedback", "feedbackIssueNames");
        
        // Language aliases
        FIELD_ALIASES.put("language", "languages");
        
        // Benefit scheme aliases
        FIELD_ALIASES.put("benefitSchemes", "benefitSchemesName");
        
        // Name aliases
        FIELD_ALIASES.put("dateOfBirth", "dob");
        FIELD_ALIASES.put("email", "eMail");
        
        // Number aliases
        FIELD_ALIASES.put("partNumber", "partNo");
        FIELD_ALIASES.put("sectionNumber", "sectionNo");
        FIELD_ALIASES.put("serialNumber", "serialNo");
        
        // Geographic aliases
        FIELD_ALIASES.put("latitude", "voterLati");
        FIELD_ALIASES.put("longitude", "voterLongi");
        
        // Caste category alias
        FIELD_ALIASES.put("casteCategory", "casteCategoryName");
        
        // Photo/URL, aadhaarVerified, friendCount are direct mappings (no alias needed)
    }
    
    private static void initializeFieldMappings() {
        // Initialize in exact order matching VoterExcelHeader
        addMapping(0, "partNo", "PART_NO");
        addMapping(1, "sectionNo", "SECTION_NO");
        addMapping(2, "serialNo", "SERIAL_NO");
        addMapping(3, "houseNoEn", "HOUSE_NO_EN");
        addMapping(4, "houseNoL1", "HOUSE_NO_L1");
        addMapping(5, "houseNoL2", "HOUSE_NO_L2");
        addMapping(6, "voterFnameEn", "VOTER_FNAME_EN");
        addMapping(7, "voterLnameEn", "VOTER_LNAME_EN");
        addMapping(8, "voterFnameL1", "VOTER_FNAME_L1");
        addMapping(9, "voterLnameL1", "VOTER_LNAME_L1");
        addMapping(10, "voterFnameL2", "VOTER_FNAME_L2");
        addMapping(11, "voterLnameL2", "VOTER_LNAME_L2");
        addMapping(12, "rlnFnameEn", "RLN_FNAME_EN");
        addMapping(13, "rlnLnameEn", "RLN_LNAME_EN");
        addMapping(14, "rlnFnameL1", "RLN_FNAME_L1");
        addMapping(15, "rlnLnameL1", "RLN_LNAME_L1");
        addMapping(16, "rlnFnameL2", "RLN_FNAME_L2");
        addMapping(17, "rlnLnameL2", "RLN_LNAME_L2");
        addMapping(18, "rlnType", "RLN_TYPE");
        addMapping(19, "epicNumber", "EPIC_NUMBER");
        addMapping(20, "gender", "GENDER");
        addMapping(21, "sectionNameEn", "SECTION_NAME_EN");
        addMapping(22, "sectionNameL1", "SECTION_NAME_L1");
        addMapping(23, "sectionNameL2", "SECTION_NAME_L2");
        addMapping(24, "fullAddress", "FULL_ADDRESS");
        addMapping(25, "partNameEn", "PART_NAME_EN");
        addMapping(26, "partNameL1", "PART_NAME_L1");
        addMapping(27, "partNameL2", "PART_NAME_L2");
        addMapping(28, "pincode", "PINCODE");
        addMapping(29, "partLati", "PART_LATI");
        addMapping(30, "partLong", "PART_LONG");
        addMapping(31, "age", "AGE");
        addMapping(32, "dob", "DOB");
        addMapping(33, "mobileNo", "MOBILE_NO");
        addMapping(34, "whatsappNo", "WHATSAPP_NO");
        addMapping(35, "eMail", "E-MAIL");
        addMapping(36, "voterLati", "VOTER_LATI");
        addMapping(37, "voterLongi", "VOTER_LONGI");
        addMapping(38, "stateCode", "STATE_CODE");
        addMapping(39, "stateNameEn", "STATE_NAME_EN");
        addMapping(40, "stateNameL1", "STATE_NAME_L1");
        addMapping(41, "stateNameL2", "STATE_NAME_L2");
        addMapping(42, "districtCode", "DISTRICT_CODE");
        addMapping(43, "districtNameEn", "DISTRICT_NAME_EN");
        addMapping(44, "districtNameL1", "DISTRICT_NAME_L1");
        addMapping(45, "districtNameL2", "DISTRICT_NAME_L2");
        addMapping(46, "pcNo", "PC_NO");
        addMapping(47, "pcNameEn", "PC_NAME_EN");
        addMapping(48, "pcNameL1", "PC_NAME_L1");
        addMapping(49, "pcNameL2", "PC_NAME_L2");
        addMapping(50, "acNo", "AC_NO");
        addMapping(51, "acNameEn", "AC_NAME_EN");
        addMapping(52, "acNameL1", "AC_NAME_L1");
        addMapping(53, "acNameL2", "AC_NAME_L2");
        addMapping(54, "urbanNo", "URBAN_NO");
        addMapping(55, "urbanNameEn", "URBAN_NAME_EN");
        addMapping(56, "urbanNameL1", "URBAN_NAME_L1");
        addMapping(57, "urbanWardNo", "URBAN_WARD_NO");
        addMapping(58, "rurDistrictUnionNo", "RUR_DISTRICT_UNION_NO");
        addMapping(59, "rurDistrictUnionNameEn", "RUR_DISTRICT_UNION_NAME_EN");
        addMapping(60, "rurDistrictUnionNameL1", "RUR_DISTRICT_UNION_NAME_L1");
        addMapping(61, "rurDistrictUnionNameL2", "RUR_DISTRICT_UNION_NAME_L2");
        addMapping(62, "rurDistrictUnionWardNo", "RUR_DISTRICT_UNION_WARD_NO");
        addMapping(63, "panUnionNo", "PAN_UNION_NO");
        addMapping(64, "panUnionNameEn", "PAN_UNION_NAME_EN");
        addMapping(65, "panUnionNameL1", "PAN_UNION_NAME_L1");
        addMapping(66, "panUnionNameL2", "PAN_UNION_NAME_L2");
        addMapping(67, "panUnionWardNo", "PAN_UNION_WARD_NO");
        addMapping(68, "villPanNo", "VILL_PAN_NO");
        addMapping(69, "villPanNameEn", "VILL_PAN_NAME_EN");
        addMapping(70, "villPanNameL1", "VILL_PAN_NAME_L1");
        addMapping(71, "villPanWardNo", "VILL_PAN_WARD_NO");
        addMapping(72, "religionName", "RELIGION_NAME");
        addMapping(73, "casteName", "CASTE_NAME");
        addMapping(74, "subCasteName", "SUB_CASTE_NAME");
        addMapping(75, "languages", "LANGUAGES");
        addMapping(76, "benefitSchemesName", "BENEFIT_SCHEMES_NAME");
        addMapping(77, "benefitSchemesBy", "BENEFIT_SCHEMES_BY");
        addMapping(78, "scheme", "SCHEME");
        addMapping(79, "availabilityDescription", "AVAILABILITY_DESCRIPTION");
        addMapping(80, "availabilityCategoryName", "AVAILABILITY_CATEGORY_NAME");
        addMapping(81, "partyName", "PARTY_NAME");
        addMapping(82, "partyShortName", "PARTY_SHORT_NAME");
        addMapping(83, "familyId", "FAMILY_ID");
        addMapping(84, "familyCount", "FAMILY_COUNT");
        addMapping(85, "feedbackIssueNames", "FEEDBACK_ISSUE_NAMES");
        addMapping(86, "voterHistoryNames", "VOTER_HISTORY_NAMES");
        addMapping(87, "starNumber", "STAR_NUMBER");
        addMapping(88, "aadhaarNumber", "AADHAAR_NUMBER");
        addMapping(89, "panNumber", "PAN_NUMBER");
        addMapping(90, "partyRegistrationNumber", "PARTY_REGISTRATION_NUMBER");
        addMapping(91, "pageNumber", "PAGE_NUMBER");
        addMapping(92, "remarks", "REMARKS");
        addMapping(93, "casteCategoryName", "CASTE_CATEGORY_NAME");
        addMapping(94, "aadhaarVerified", "AADHAAR_VERIFIED");
        addMapping(95, "photoUrl", "PHOTO_URL");
        addMapping(96, "friendCount", "FRIEND_COUNT");
    }
    
    private static void addMapping(int index, String fieldName, String headerName) {
        FIELD_TO_HEADER.put(fieldName, headerName);
        FIELD_TO_INDEX.put(fieldName, index);
        ALL_FIELD_NAMES.add(fieldName);
    }
    
    /**
     * Get column index for a field
     */
    public static Integer getColumnIndex(String fieldName) {
        return FIELD_TO_INDEX.get(fieldName);
    }
    
    /**
     * Get all field names in order
     */
    public static List<String> getAllFieldNames() {
        return new ArrayList<>(ALL_FIELD_NAMES);
    }
    
    /**
     * Validate field names and return only valid ones.
     * Accepts both standard fields and dynamic field names.
     */
    public static List<String> validateAndFilterFields(List<String> fieldNames) {
        if (fieldNames == null || fieldNames.isEmpty()) {
            return getAllFieldNames();
        }
        
        // Check for "ALL" keyword
        if (fieldNames.size() == 1 && "ALL".equalsIgnoreCase(fieldNames.get(0))) {
            return getAllFieldNames();
        }
        
        List<String> validFields = new ArrayList<>();
        List<String> invalidFields = new ArrayList<>();
        
        for (String fieldName : fieldNames) {
            // First check if it's a direct field name
            if (FIELD_TO_HEADER.containsKey(fieldName)) {
                validFields.add(fieldName);
            }
            // Then check if it's an alias
            else if (FIELD_ALIASES.containsKey(fieldName)) {
                String resolvedFieldName = FIELD_ALIASES.get(fieldName);
                validFields.add(resolvedFieldName);
                log.debug("Resolved field alias '{}' to '{}'", fieldName, resolvedFieldName);
            }
            // For dynamic fields, accept them (they'll be validated at runtime)
            else {
                // Assume it might be a dynamic field, add it with a warning
                validFields.add(fieldName);
                log.debug("Field '{}' not in standard mappings, treating as potential dynamic field", fieldName);
            }
        }
        
        return validFields;
    }
    
    /**
     * Get Excel header for a field (standard or dynamic)
     * Dynamic fields use their field name as the header
     */
    public static String getExcelHeader(String fieldName) {
        // Check if it's a standard field first
        if (FIELD_TO_HEADER.containsKey(fieldName)) {
            return FIELD_TO_HEADER.get(fieldName);
        }
        // For dynamic fields, convert to uppercase and use as-is
        return fieldName.toUpperCase().replace('_', ' ');
    }
    
    /**
     * Merge standard fields with dynamic field names for complete column list
     */
    public static List<String> mergeStandardAndDynamicFields(List<String> requestedFields, List<String> dynamicFieldNames) {
        List<String> allFields = new ArrayList<>();
        
        if (requestedFields == null || requestedFields.isEmpty()) {
            // If no specific fields requested, add all standard fields
            allFields.addAll(getAllFieldNames());
        } else if (requestedFields.size() == 1 && "ALL".equalsIgnoreCase(requestedFields.get(0))) {
            // "ALL" means all standard fields
            allFields.addAll(getAllFieldNames());
        } else {
            // Add requested fields (already validated)
            allFields.addAll(requestedFields);
        }
        
        // Add dynamic fields at the end
        if (dynamicFieldNames != null && !dynamicFieldNames.isEmpty()) {
            for (String dynamicField : dynamicFieldNames) {
                if (!allFields.contains(dynamicField)) {
                    allFields.add(dynamicField);
                }
            }
        }
        
        return allFields;
    }
    
    /**
     * Extract field value from VoterEntity, including dynamic fields
     */
    public static Object getFieldValue(VoterEntity voter, String fieldName) {
        if (voter == null) {
            return null;
        }
        
        // Check if this is a dynamic field first
        if (voter.getDynamicFields() != null && voter.getDynamicFields().containsKey(fieldName)) {
            return voter.getDynamicFields().get(fieldName);
        }
        
        try {
            switch (fieldName) {
                // Basic fields
                case "partNo":
                    return voter.getPartNo();
                case "sectionNo":
                    return voter.getSectionNo();
                case "serialNo":
                    return voter.getSerialNo();
                case "epicNumber":
                    return voter.getEpicNumber();
                case "gender":
                    return voter.getGender();
                case "age":
                    return voter.getAge();
                case "dob":
                    return formatDate(voter.getDob());
                case "starNumber":
                    return voter.getStarNumber();
                case "pageNumber":
                    return voter.getPageNumber();
                    
                // House numbers
                case "houseNoEn":
                    return voter.getHouseNoEn();
                case "houseNoL1":
                    return voter.getHouseNoL1();
                case "houseNoL2":
                    return voter.getHouseNoL2();
                    
                // Voter names
                case "voterFnameEn":
                    return voter.getVoterFnameEn();
                case "voterLnameEn":
                    return voter.getVoterLnameEn();
                case "voterFnameL1":
                    return voter.getVoterFnameL1();
                case "voterLnameL1":
                    return voter.getVoterLnameL1();
                case "voterFnameL2":
                    return voter.getVoterFnameL2();
                case "voterLnameL2":
                    return voter.getVoterLnameL2();
                    
                // Relation names
                case "rlnType":
                    return voter.getRlnType();
                case "rlnFnameEn":
                    return voter.getRlnFnameEn();
                case "rlnLnameEn":
                    return voter.getRlnLnameEn();
                case "rlnFnameL1":
                    return voter.getRlnFnameL1();
                case "rlnLnameL1":
                    return voter.getRlnLnameL1();
                case "rlnFnameL2":
                    return voter.getRlnFnameL2();
                case "rlnLnameL2":
                    return voter.getRlnLnameL2();
                    
                // Section names
                case "sectionNameEn":
                    return voter.getSectionNameEn();
                case "sectionNameL1":
                    return voter.getSectionNameL1();
                case "sectionNameL2":
                    return voter.getSectionNameL2();
                    
                // Address
                case "fullAddress":
                    return voter.getFullAddress();
                case "pincode":
                    return voter.getPincode();
                    
                // Part names
                case "partNameEn":
                    return voter.getPartNameEn();
                case "partNameL1":
                    return voter.getPartNameL1();
                case "partNameL2":
                    return voter.getPartNameL2();
                    
                // Location
                case "partLati":
                    return voter.getPartLati();
                case "partLong":
                    return voter.getPartLong();
                case "voterLati":
                    return voter.getVoterLati();
                case "voterLongi":
                    return voter.getVoterLongi();
                    
                // Contact
                case "mobileNo":
                    return voter.getMobileNo();
                case "whatsappNo":
                    return voter.getWhatsappNo();
                case "eMail":
                    return voter.getEMail();
                    
                // State
                case "stateCode":
                    return voter.getStateCode();
                case "stateNameEn":
                    return voter.getStateNameEn();
                case "stateNameL1":
                    return voter.getStateNameL1();
                case "stateNameL2":
                    return voter.getStateNameL2();
                    
                // District
                case "districtCode":
                    return voter.getDistrictCode();
                case "districtNameEn":
                    return voter.getDistrictNameEn();
                case "districtNameL1":
                    return voter.getDistrictNameL1();
                case "districtNameL2":
                    return voter.getDistrictNameL2();
                    
                // PC
                case "pcNo":
                    return voter.getPcNo();
                case "pcNameEn":
                    return voter.getPcNameEn();
                case "pcNameL1":
                    return voter.getPcNameL1();
                case "pcNameL2":
                    return voter.getPcNameL2();
                    
                // AC
                case "acNo":
                    return voter.getAcNo();
                case "acNameEn":
                    return voter.getAcNameEn();
                case "acNameL1":
                    return voter.getAcNameL1();
                case "acNameL2":
                    return voter.getAcNameL2();
                    
                // Urban
                case "urbanNo":
                    return voter.getUrbanNo();
                case "urbanNameEn":
                    return voter.getUrbanNameEn();
                case "urbanNameL1":
                    return voter.getUrbanNameL1();
                case "urbanWardNo":
                    return voter.getUrbanWardNo();
                    
                // Rural
                case "rurDistrictUnionNo":
                    return voter.getRurDistrictUnionNo();
                case "rurDistrictUnionNameEn":
                    return voter.getRurDistrictUnionNameEn();
                case "rurDistrictUnionNameL1":
                    return voter.getRurDistrictUnionNameL1();
                case "rurDistrictUnionNameL2":
                    return voter.getRurDistrictUnionNameL2();
                case "rurDistrictUnionWardNo":
                    return voter.getRurDistrictUnionWardNo();
                    
                // Panchayat
                case "panUnionNo":
                    return voter.getPanUnionNo();
                case "panUnionNameEn":
                    return voter.getPanUnionNameEn();
                case "panUnionNameL1":
                    return voter.getPanUnionNameL1();
                case "panUnionNameL2":
                    return voter.getPanUnionNameL2();
                case "panUnionWardNo":
                    return voter.getPanUnionWardNo();
                    
                // Village
                case "villPanNo":
                    return voter.getVillPanNo();
                case "villPanNameEn":
                    return voter.getVillPanNameEn();
                case "villPanNameL1":
                    return voter.getVillPanNameL1();
                case "villPanWardNo":
                    return voter.getVillPanWardNo();
                    
                // Religion
                case "religionName":
                    return voter.getReligion() != null ? voter.getReligion().getReligionName() : null;
                    
                // Caste
                case "casteName":
                    return voter.getCaste() != null ? voter.getCaste().getCasteName() : null;
                case "subCasteName":
                    return voter.getSubCaste() != null ? voter.getSubCaste().getSubCasteName() : null;
                    
                // Languages
                case "languages":
                    return voter.getLanguages() != null && !voter.getLanguages().isEmpty()
                            ? voter.getLanguages().stream()
                                    .map(lang -> lang.getLanguageName())
                                    .collect(Collectors.joining(", "))
                            : null;
                    
                // Benefit Schemes
                case "benefitSchemesName":
                    return voter.getVoterBenefitSchemes() != null && !voter.getVoterBenefitSchemes().isEmpty()
                            ? voter.getVoterBenefitSchemes().stream()
                                    .map(vbs -> vbs.getBenefitScheme() != null ? vbs.getBenefitScheme().getSchemeName() : null)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining(", "))
                            : null;
                case "benefitSchemesBy":
                    return voter.getVoterBenefitSchemes() != null && !voter.getVoterBenefitSchemes().isEmpty()
                            ? voter.getVoterBenefitSchemes().stream()
                                    .map(vbs -> vbs.getBenefitScheme() != null && vbs.getBenefitScheme().getSchemeBy() != null 
                                            ? vbs.getBenefitScheme().getSchemeBy().name() : null)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining(", "))
                            : null;
                case "scheme":
                    return voter.getScheme();
                    
                // Availability
                case "availabilityDescription":
                    return voter.getAvailability1() != null ? voter.getAvailability1().getDescription() : null;
                case "availabilityCategoryName":
                    return voter.getAvailability1() != null ? voter.getAvailability1().getCategoryName() : null;
                    
                // Party
                case "partyName":
                    return voter.getParty() != null ? voter.getParty().getPartyName() : null;
                case "partyShortName":
                    return voter.getParty() != null ? voter.getParty().getPartyShortName() : null;
                    
                // Family
                case "familyId":
                    return voter.getFamilyId() != null ? voter.getFamilyId().toString() : null;
                case "familyCount":
                    return voter.getFamilyCount();
                    
                // Feedback Issues
                case "feedbackIssueNames":
                    return voter.getFeedbackIssues() != null && !voter.getFeedbackIssues().isEmpty()
                            ? voter.getFeedbackIssues().stream()
                                    .map(issue -> issue.getIssueName())
                                    .collect(Collectors.joining(", "))
                            : null;
                    
                // Voter History
                case "voterHistoryNames":
                    return voter.getVoterHistories() != null && !voter.getVoterHistories().isEmpty()
                            ? voter.getVoterHistories().stream()
                                    .map(history -> history.getVoterHistoryName())
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.joining(", "))
                            : null;
                    
                // Documents
                case "aadhaarNumber":
                    return voter.getAadhaarNumber();
                case "panNumber":
                    return voter.getPanNumber();
                case "partyRegistrationNumber":
                    return voter.getPartyRegistrationNumber();
                    
                // Additional
                case "remarks":
                    return voter.getRemarks();
                case "casteCategoryName":
                    return voter.getCasteCategory() != null ? voter.getCasteCategory().getCasteCategoryName() : null;
                case "aadhaarVerified":
                    return voter.getAadhaarVerified();
                case "photoUrl":
                    return voter.getPhotoUrl();
                case "friendCount":
                    return voter.getFriendCount();
                    
                default:
                    log.warn("Unknown field name: {}", fieldName);
                    return null;
            }
        } catch (Exception e) {
            log.error("Error extracting field value for field: {}, error: {}", fieldName, e.getMessage());
            return null;
        }
    }
    
    /**
     * Format LocalDate to string
     */
    private static String formatDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        try {
            return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            return date.toString();
        }
    }
    
    /**
     * Format LocalDateTime to string
     */
    private static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        try {
            return dateTime.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        } catch (Exception e) {
            return dateTime.toString();
        }
    }
}
