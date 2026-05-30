package com.thedal.thedal_app.election;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.dtos.StaticFieldStatusDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class StaticFieldStatusService {

    @Autowired
    private StaticFieldStatusRepository staticFieldStatusRepository;
    
    @Autowired
    private ElectionRepository electionRepository;

    /**
     * Initialize default static field statuses for an election.
     * This should be called when a new election is created.
     */
    @Transactional
    public void initializeDefaultStaticFields(Long accountId, Long electionId) {
        log.info("Initializing default static fields for accountId={}, electionId={}", accountId, electionId);
        
        List<StaticFieldStatusEntity> defaultFields = getDefaultStaticFieldDefinitions()
                .stream()
                .map(dto -> {
                    StaticFieldStatusEntity entity = new StaticFieldStatusEntity();
                    entity.setAccountId(accountId);
                    entity.setElectionId(electionId);
                    entity.setFieldName(dto.getFieldName());
                    entity.setFieldLabel(dto.getFieldLabel());
                    entity.setFieldCategory(dto.getFieldCategory());
                    entity.setStatus(true); // Default enabled
                    entity.setMandatory(false); // Default optional
                    return entity;
                })
                .collect(Collectors.toList());

        staticFieldStatusRepository.saveAll(defaultFields);
        log.info("Initialized {} static fields for election {}", defaultFields.size(), electionId);
    }

    /**
     * Get static field status for a specific field in an election.
     * Returns true by default if no status is explicitly set.
     */
    public Boolean isFieldEnabled(Long accountId, Long electionId, String fieldName) {
        return staticFieldStatusRepository.isFieldEnabled(accountId, electionId, fieldName);
    }

    /**
     * Update static field status for an election
     */
    @Transactional
    public void updateFieldStatus(Long accountId, Long electionId, String fieldName, Boolean status) {
        log.info("Updating field status for accountId={}, electionId={}, fieldName={}, status={}", 
                accountId, electionId, fieldName, status);
        
        Optional<StaticFieldStatusEntity> existing = staticFieldStatusRepository
                .findByAccountIdAndElectionIdAndFieldName(accountId, electionId, fieldName);
        
        if (existing.isPresent()) {
            StaticFieldStatusEntity entity = existing.get();
            entity.setStatus(status);
            staticFieldStatusRepository.save(entity);
        } else {
            // Create new entry if not exists
            StaticFieldStatusEntity newEntity = new StaticFieldStatusEntity();
            newEntity.setAccountId(accountId);
            newEntity.setElectionId(electionId);
            newEntity.setFieldName(fieldName);
            newEntity.setStatus(status);
            
            // Try to get field label and category from defaults
            getDefaultStaticFieldDefinitions().stream()
                    .filter(dto -> dto.getFieldName().equals(fieldName))
                    .findFirst()
                    .ifPresent(dto -> {
                        newEntity.setFieldLabel(dto.getFieldLabel());
                        newEntity.setFieldCategory(dto.getFieldCategory());
                    });
            
            staticFieldStatusRepository.save(newEntity);
        }
    }

    /**
     * Update static field mandatory status for an election
     */
    @Transactional
    public void updateFieldMandatory(Long accountId, Long electionId, String fieldName, Boolean mandatory) {
        log.info("Updating field mandatory status for accountId={}, electionId={}, fieldName={}, mandatory={}", 
                accountId, electionId, fieldName, mandatory);
        
        Optional<StaticFieldStatusEntity> existing = staticFieldStatusRepository
                .findByAccountIdAndElectionIdAndFieldName(accountId, electionId, fieldName);
        
        if (existing.isPresent()) {
            StaticFieldStatusEntity entity = existing.get();
            entity.setMandatory(mandatory);
            staticFieldStatusRepository.save(entity);
        } else {
            // Create new entry if not exists
            StaticFieldStatusEntity newEntity = new StaticFieldStatusEntity();
            newEntity.setAccountId(accountId);
            newEntity.setElectionId(electionId);
            newEntity.setFieldName(fieldName);
            newEntity.setMandatory(mandatory);
            
            // Try to get field label and category from defaults
            getDefaultStaticFieldDefinitions().stream()
                    .filter(dto -> dto.getFieldName().equals(fieldName))
                    .findFirst()
                    .ifPresent(dto -> {
                        newEntity.setFieldLabel(dto.getFieldLabel());
                        newEntity.setFieldCategory(dto.getFieldCategory());
                    });
            
            staticFieldStatusRepository.save(newEntity);
        }
    }

    /**
     * Bulk update static field statuses
     */
    @Transactional
    public void updateFieldStatuses(Long accountId, Long electionId, List<StaticFieldStatusDTO> fieldStatuses) {
        for (StaticFieldStatusDTO dto : fieldStatuses) {
            updateFieldStatus(accountId, electionId, dto.getFieldName(), dto.getStatus());
            if (dto.getMandatory() != null) {
                updateFieldMandatory(accountId, electionId, dto.getFieldName(), dto.getMandatory());
            }
        }
    }

    /**
     * Initialize static field statuses for all existing elections that don't have them yet.
     * This ensures backward compatibility for existing elections.
     */
    @Transactional
    public int initializeAllExistingElections(Long accountId) {
        log.info("Initializing static fields for all existing elections for accountId={}", accountId);
        
        // Get all elections for this account that are not deleted
        List<ElectionEntity> allElections = electionRepository.findAllActiveElections(accountId);
        
        int initializedCount = 0;
        
        for (ElectionEntity election : allElections) {
            Long electionId = election.getId();
            
            // Check if this election already has static field configurations
            List<StaticFieldStatusEntity> existingFields = staticFieldStatusRepository
                    .findByAccountIdAndElectionId(accountId, electionId);
            
            if (existingFields.isEmpty()) {
                // Election doesn't have static field configurations, initialize them
                log.info("Initializing static fields for existing election: electionId={}, name={}", 
                        electionId, election.getElectionName());
                
                try {
                    initializeDefaultStaticFields(accountId, electionId);
                    initializedCount++;
                } catch (Exception e) {
                    log.error("Failed to initialize static fields for election {}: {}", electionId, e.getMessage());
                    // Continue with other elections even if one fails
                }
            } else {
                log.debug("Election {} already has {} static field configurations, skipping", 
                        electionId, existingFields.size());
            }
        }
        
        log.info("Completed initializing static fields for {} out of {} existing elections", 
                initializedCount, allElections.size());
        
        return initializedCount;
    }

    /**
     * Reconcile missing static fields for a specific election.
     * This adds any missing fields from the default definitions that are not yet configured.
     */
    @Transactional
    public int reconcileMissingFieldsForElection(Long accountId, Long electionId) {
        log.info("Reconciling missing static fields for accountId={}, electionId={}", accountId, electionId);
        
        // Get all default field definitions
        List<StaticFieldStatusDTO> allDefaultFields = getDefaultStaticFieldDefinitions();
        
        // Get existing fields for this election
        List<StaticFieldStatusEntity> existingFields = staticFieldStatusRepository
                .findByAccountIdAndElectionId(accountId, electionId);
        
        // Create a set of existing field names for quick lookup
        Set<String> existingFieldNames = existingFields.stream()
                .map(StaticFieldStatusEntity::getFieldName)
                .collect(Collectors.toSet());
        
        // Find missing fields
        List<StaticFieldStatusDTO> missingFields = allDefaultFields.stream()
                .filter(defaultField -> !existingFieldNames.contains(defaultField.getFieldName()))
                .collect(Collectors.toList());
        
        if (missingFields.isEmpty()) {
            log.info("No missing fields found for election {}", electionId);
            return 0;
        }
        
        // Create entities for missing fields
        List<StaticFieldStatusEntity> newEntities = missingFields.stream()
                .map(dto -> {
                    StaticFieldStatusEntity entity = new StaticFieldStatusEntity();
                    entity.setAccountId(accountId);
                    entity.setElectionId(electionId);
                    entity.setFieldName(dto.getFieldName());
                    entity.setFieldLabel(dto.getFieldLabel());
                    entity.setFieldCategory(dto.getFieldCategory());
                    entity.setStatus(true); // Default enabled
                    return entity;
                })
                .collect(Collectors.toList());
        
        // Save the new entities
        staticFieldStatusRepository.saveAll(newEntities);
        
        log.info("Added {} missing static fields for election {}", newEntities.size(), electionId);
        return newEntities.size();
    }

    /**
     * Reconcile missing static fields for all elections of an account.
     * This is useful for ensuring all elections have the complete set of static fields.
     */
    @Transactional
    public int reconcileMissingFieldsForAllElections(Long accountId) {
        log.info("Reconciling missing static fields for all elections for accountId={}", accountId);
        
        // Get all elections for this account that are not deleted
        List<ElectionEntity> allElections = electionRepository.findAllActiveElections(accountId);
        
        int totalAddedFields = 0;
        
        for (ElectionEntity election : allElections) {
            Long electionId = election.getId();
            
            try {
                int addedFields = reconcileMissingFieldsForElection(accountId, electionId);
                totalAddedFields += addedFields;
                
                if (addedFields > 0) {
                    log.info("Added {} missing fields for election: electionId={}, name={}", 
                            addedFields, electionId, election.getElectionName());
                }
            } catch (Exception e) {
                log.error("Failed to reconcile missing fields for election {}: {}", electionId, e.getMessage());
                // Continue with other elections even if one fails
            }
        }
        
        log.info("Completed reconciling missing fields for all elections. Total fields added: {}", totalAddedFields);
        return totalAddedFields;
    }

    /**
     * Get all static field statuses for an election
     */
    public List<StaticFieldStatusDTO> getFieldStatuses(Long accountId, Long electionId) {
        List<StaticFieldStatusEntity> entities = staticFieldStatusRepository
                .findByAccountIdAndElectionId(accountId, electionId);
        
        // Get all default field definitions
        List<StaticFieldStatusDTO> allDefaultFields = getDefaultStaticFieldDefinitions();
        
        if (entities.isEmpty()) {
            // Return default field definitions if no custom statuses are set
            return allDefaultFields;
        }
        
        // Create a map of existing entities by field name for quick lookup
        Map<String, StaticFieldStatusEntity> existingFields = entities.stream()
                .collect(Collectors.toMap(StaticFieldStatusEntity::getFieldName, entity -> entity));
        
        // Merge defaults with existing entities to ensure all fields are present
        return allDefaultFields.stream()
                .map(defaultField -> {
                    StaticFieldStatusEntity existingEntity = existingFields.get(defaultField.getFieldName());
                    if (existingEntity != null) {
                        // Use existing entity data
                        return new StaticFieldStatusDTO(
                                existingEntity.getFieldName(),
                                existingEntity.getFieldLabel(),
                                existingEntity.getFieldCategory(),
                                existingEntity.getStatus(),
                                existingEntity.getMandatory()
                        );
                    } else {
                        // Use default field with default enabled status
                        return new StaticFieldStatusDTO(
                                defaultField.getFieldName(),
                                defaultField.getFieldLabel(),
                                defaultField.getFieldCategory(),
                                true, // Default to enabled
                                false // Default to optional
                        );
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all static field statuses for an election with optional mandatory filter
     */
    public List<StaticFieldStatusDTO> getFieldStatuses(Long accountId, Long electionId, Boolean mandatoryFilter) {
        List<StaticFieldStatusDTO> allFields = getFieldStatuses(accountId, electionId);
        
        // If no filter specified, return all fields
        if (mandatoryFilter == null) {
            return allFields;
        }
        
        // Filter by mandatory status
        return allFields.stream()
                .filter(field -> mandatoryFilter.equals(field.getMandatory()))
                .collect(Collectors.toList());
    }

    /**
     * Get enabled static fields for an election
     */
    public List<String> getEnabledFieldNames(Long accountId, Long electionId) {
        List<StaticFieldStatusEntity> enabledFields = staticFieldStatusRepository
                .findEnabledFieldsByAccountIdAndElectionId(accountId, electionId);
        
        // Get all default field definitions
        List<StaticFieldStatusDTO> allDefaultFields = getDefaultStaticFieldDefinitions();
        
        if (enabledFields.isEmpty()) {
            // Return all default field names if no custom statuses are set (all enabled by default)
            return allDefaultFields.stream()
                    .map(StaticFieldStatusDTO::getFieldName)
                    .collect(Collectors.toList());
        }
        
        // Create a map of existing entities by field name for quick lookup
        Map<String, StaticFieldStatusEntity> existingFields = staticFieldStatusRepository
                .findByAccountIdAndElectionId(accountId, electionId)
                .stream()
                .collect(Collectors.toMap(StaticFieldStatusEntity::getFieldName, entity -> entity));
        
        // Return enabled fields from existing entities + default enabled fields that are missing
        return allDefaultFields.stream()
                .filter(defaultField -> {
                    StaticFieldStatusEntity existingEntity = existingFields.get(defaultField.getFieldName());
                    if (existingEntity != null) {
                        // Return based on existing entity status
                        return existingEntity.getStatus();
                    } else {
                        // Default to enabled for missing fields
                        return true;
                    }
                })
                .map(StaticFieldStatusDTO::getFieldName)
                .collect(Collectors.toList());
    }

    /**
     * Get static field statuses grouped by category
     */
    public Map<String, List<StaticFieldStatusDTO>> getFieldStatusesByCategory(Long accountId, Long electionId) {
        List<StaticFieldStatusDTO> allStatuses = getFieldStatuses(accountId, electionId);
        return allStatuses.stream()
                .collect(Collectors.groupingBy(StaticFieldStatusDTO::getFieldCategory));
    }

    /**
     * Define all default static fields that can be managed.
     * This comprehensive list covers all the main static fields in VoterEntity
     * and includes the MergeField enum compatible field names.
     */
    private List<StaticFieldStatusDTO> getDefaultStaticFieldDefinitions() {
        List<StaticFieldStatusDTO> fields = new ArrayList<>();

        // Mergeable Fields (Compatible with MergeField enum)
        fields.addAll(Arrays.asList(
            // Contact Information
            new StaticFieldStatusDTO("MOBILE_NUMBER", "Mobile Number", "contact"),
            new StaticFieldStatusDTO("WHATSAPP_NUMBER", "WhatsApp Number", "contact"),
            new StaticFieldStatusDTO("EMAIL_ID", "Email ID", "contact"),
            
            // Personal Information
            new StaticFieldStatusDTO("DATE_OF_BIRTH", "Date of Birth", "personal"),
            new StaticFieldStatusDTO("LOCATION", "Location", "address"),
            
            // Demographics
            new StaticFieldStatusDTO("RELIGION", "Religion", "demographics"),
            new StaticFieldStatusDTO("CASTE_CATEGORY", "Caste Category", "demographics"),
            new StaticFieldStatusDTO("CASTE", "Caste", "demographics"),
            new StaticFieldStatusDTO("SUB_CASTE", "Sub Caste", "demographics"),
            
            // Political Information
            new StaticFieldStatusDTO("PARTY", "Political Party", "political"),
            new StaticFieldStatusDTO("VOTER_CATEGORY", "Voter Category", "political"),
            
            // Multi-valued Fields
            new StaticFieldStatusDTO("LANGUAGE", "Languages Known", "personal"),
            new StaticFieldStatusDTO("BENEFIT_SCHEMES", "Benefit Schemes", "schemes"),
            new StaticFieldStatusDTO("VOTER_HISTORY", "Election History", "political"),
            new StaticFieldStatusDTO("FEEDBACK", "Feedback Issues", "feedback"),
            
            // Identity Documents
            new StaticFieldStatusDTO("AADHAAR_NUMBER", "Aadhaar Number", "identity"),
            new StaticFieldStatusDTO("PAN_NUMBER", "PAN Number", "identity"),
            new StaticFieldStatusDTO("MEMBERSHIP_NUMBER", "Membership Number", "identity"),
            
            // Additional Information
            new StaticFieldStatusDTO("REMARKS", "Remarks", "notes"),
            
            // Relationship Mappings
            new StaticFieldStatusDTO("FAMILY_MAPPING", "Family Mapping", "relationships"),
            new StaticFieldStatusDTO("FRIENDS_MAPPING", "Friends Network", "relationships")
        ));

        // Basic Information (Database column names for compatibility)
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("voterId", "Voter ID", "basic"),
            new StaticFieldStatusDTO("epicNumber", "EPIC Number", "basic"),
            new StaticFieldStatusDTO("gender", "Gender", "basic"),
            new StaticFieldStatusDTO("age", "Age", "basic"),
            new StaticFieldStatusDTO("dob", "Date of Birth", "basic"),
            new StaticFieldStatusDTO("starNumber", "Star Number", "basic")
        ));

        // Names
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("voterFnameEn", "First Name (English)", "names"),
            new StaticFieldStatusDTO("voterLnameEn", "Last Name (English)", "names"),
            new StaticFieldStatusDTO("voterFnameL1", "First Name (L1)", "names"),
            new StaticFieldStatusDTO("voterFnameL2", "First Name (L2)", "names"),
            new StaticFieldStatusDTO("voterLnameL1", "Last Name (L1)", "names"),
            new StaticFieldStatusDTO("voterLnameL2", "Last Name (L2)", "names")
        ));

        // Relations
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("rlnType", "Relation Type", "relations"),
            new StaticFieldStatusDTO("rlnFnameEn", "Relation First Name (English)", "relations"),
            new StaticFieldStatusDTO("rlnLnameEn", "Relation Last Name (English)", "relations"),
            new StaticFieldStatusDTO("rlnFnameL1", "Relation First Name (L1)", "relations"),
            new StaticFieldStatusDTO("rlnFnameL2", "Relation First Name (L2)", "relations"),
            new StaticFieldStatusDTO("rlnLnameL1", "Relation Last Name (L1)", "relations"),
            new StaticFieldStatusDTO("rlnLnameL2", "Last Name (L2)", "relations")
        ));

        // Contact Information (Database column names for compatibility)
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("mobileNo", "Mobile Number (DB)", "contact"),
            new StaticFieldStatusDTO("whatsappNo", "WhatsApp Number (DB)", "contact"),
            new StaticFieldStatusDTO("eMail", "Email (DB)", "contact")
        ));

        // Address Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("fullAddress", "Full Address", "address"),
            new StaticFieldStatusDTO("pincode", "Pincode", "address"),
            new StaticFieldStatusDTO("houseNoEn", "House Number (English)", "address"),
            new StaticFieldStatusDTO("houseNoL1", "House Number (L1)", "address"),
            new StaticFieldStatusDTO("houseNoL2", "House Number (L2)", "address")
        ));

        // Geographic/Administrative
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("partNo", "Part Number", "geographic"),
            new StaticFieldStatusDTO("sectionNo", "Section Number", "geographic"),
            new StaticFieldStatusDTO("serialNo", "Serial Number", "geographic"),
            new StaticFieldStatusDTO("boothNumber", "Booth Number", "geographic"),
            new StaticFieldStatusDTO("pageNumber", "Page Number", "geographic")
        ));

        // State Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("stateCode", "State Code", "state"),
            new StaticFieldStatusDTO("stateNameEn", "State Name (English)", "state"),
            new StaticFieldStatusDTO("stateNameL1", "State Name (L1)", "state"),
            new StaticFieldStatusDTO("stateNameL2", "State Name (L2)", "state")
        ));

        // District Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("districtCode", "District Code", "district"),
            new StaticFieldStatusDTO("districtNameEn", "District Name (English)", "district"),
            new StaticFieldStatusDTO("districtNameL1", "District Name (L1)", "district"),
            new StaticFieldStatusDTO("districtNameL2", "District Name (L2)", "district")
        ));

        // PC (Parliamentary Constituency) Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("pcNo", "PC Number", "pc"),
            new StaticFieldStatusDTO("pcNameEn", "PC Name (English)", "pc"),
            new StaticFieldStatusDTO("pcNameL1", "PC Name (L1)", "pc"),
            new StaticFieldStatusDTO("pcNameL2", "PC Name (L2)", "pc")
        ));

        // AC (Assembly Constituency) Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("acNo", "AC Number", "ac"),
            new StaticFieldStatusDTO("acNameEn", "AC Name (English)", "ac"),
            new StaticFieldStatusDTO("acNameL1", "AC Name (L1)", "ac"),
            new StaticFieldStatusDTO("acNameL2", "AC Name (L2)", "ac")
        ));

        // Urban Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("urbanNo", "Urban Number", "urban"),
            new StaticFieldStatusDTO("urbanNameEn", "Urban Name (English)", "urban"),
            new StaticFieldStatusDTO("urbanNameL1", "Urban Name (L1)", "urban"),
            new StaticFieldStatusDTO("urbanWardNo", "Urban Ward Number", "urban")
        ));

        // Rural Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("rurDistrictUnionNo", "Rural District Union Number", "rural"),
            new StaticFieldStatusDTO("rurDistrictUnionNameEn", "Rural District Union Name (English)", "rural"),
            new StaticFieldStatusDTO("rurDistrictUnionNameL1", "Rural District Union Name (L1)", "rural"),
            new StaticFieldStatusDTO("rurDistrictUnionNameL2", "Rural District Union Name (L2)", "rural"),
            new StaticFieldStatusDTO("rurDistrictUnionWardNo", "Rural District Union Ward Number", "rural")
        ));

        // Panchayat Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("panUnionNo", "Panchayat Union Number", "panchayat"),
            new StaticFieldStatusDTO("panUnionNameEn", "Panchayat Union Name (English)", "panchayat"),
            new StaticFieldStatusDTO("panUnionNameL1", "Panchayat Union Name (L1)", "panchayat"),
            new StaticFieldStatusDTO("panUnionNameL2", "Panchayat Union Name (L2)", "panchayat"),
            new StaticFieldStatusDTO("panUnionWardNo", "Panchayat Union Ward Number", "panchayat")
        ));

        // Village Panchayat Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("villPanNo", "Village Panchayat Number", "village"),
            new StaticFieldStatusDTO("villPanNameEn", "Village Panchayat Name (English)", "village"),
            new StaticFieldStatusDTO("villPanNameL1", "Village Panchayat Name (L1)", "village"),
            new StaticFieldStatusDTO("villPanWardNo", "Village Panchayat Ward Number", "village")
        ));

        // Family Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("familyId", "Family ID", "family"),
            new StaticFieldStatusDTO("familyCount", "Family Count", "family"),
            new StaticFieldStatusDTO("familySequenceNumber", "Family Sequence Number", "family"),
            new StaticFieldStatusDTO("isFamilyHead", "Is Family Head", "family")
        ));

        // Verification Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("mobileVerified", "Mobile Verified", "verification"),
            new StaticFieldStatusDTO("aadhaarVerified", "Aadhaar Verified", "verification"),
            new StaticFieldStatusDTO("memberVerified", "Member Verified", "verification")
        ));

        // Additional Information (Database column names for compatibility)
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("aadhaarNumber", "Aadhaar Number (DB)", "documents"),
            new StaticFieldStatusDTO("panNumber", "PAN Number (DB)", "documents"),
            new StaticFieldStatusDTO("partyRegistrationNumber", "Party Registration Number", "documents"),
            new StaticFieldStatusDTO("availability", "Availability", "status"),
            new StaticFieldStatusDTO("partyAffiliation", "Party Affiliation", "status"),
            new StaticFieldStatusDTO("scheme", "Scheme", "status"),
            new StaticFieldStatusDTO("remarks", "Remarks (DB)", "additional"),
            new StaticFieldStatusDTO("photoUrl", "Photo URL", "additional"),
            new StaticFieldStatusDTO("videoUrl", "Video URL", "additional")
        ));

        // Location Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("partLati", "Part Latitude", "location"),
            new StaticFieldStatusDTO("partLong", "Part Longitude", "location"),
            new StaticFieldStatusDTO("voterLati", "Voter Latitude", "location"),
            new StaticFieldStatusDTO("voterLongi", "Voter Longitude", "location")
        ));

        // Section Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("sectionNameEn", "Section Name (English)", "section"),
            new StaticFieldStatusDTO("sectionNameL1", "Section Name (L1)", "section"),
            new StaticFieldStatusDTO("sectionNameL2", "Section Name (L2)", "section")
        ));

        // Part Information
        fields.addAll(Arrays.asList(
            new StaticFieldStatusDTO("partNameEn", "Part Name (English)", "part"),
            new StaticFieldStatusDTO("partNameL1", "Part Name (L1)", "part"),
            new StaticFieldStatusDTO("partNameL2", "Part Name (L2)", "part")
        ));

        return fields;
    }

    /**
     * Enable all static fields for an election (set status = true for all fields)
     */
    @Transactional
    public int enableAllFields(Long accountId, Long electionId) {
        log.info("Enabling all static fields for accountId={}, electionId={}", accountId, electionId);
        
        List<StaticFieldStatusDTO> allFields = getDefaultStaticFieldDefinitions();
        int updatedCount = 0;
        
        for (StaticFieldStatusDTO field : allFields) {
            updateFieldStatus(accountId, electionId, field.getFieldName(), true);
            updatedCount++;
        }
        
        log.info("Successfully enabled {} static fields for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    /**
     * Disable all static fields for an election (set status = false for all fields)
     */
    @Transactional
    public int disableAllFields(Long accountId, Long electionId) {
        log.info("Disabling all static fields for accountId={}, electionId={}", accountId, electionId);
        
        List<StaticFieldStatusDTO> allFields = getDefaultStaticFieldDefinitions();
        int updatedCount = 0;
        
        for (StaticFieldStatusDTO field : allFields) {
            updateFieldStatus(accountId, electionId, field.getFieldName(), false);
            updatedCount++;
        }
        
        log.info("Successfully disabled {} static fields for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    /**
     * Make all static fields required for an election (set mandatory = true for all fields)
     */
    @Transactional
    public int requireAllFields(Long accountId, Long electionId) {
        log.info("Making all static fields required for accountId={}, electionId={}", accountId, electionId);
        
        List<StaticFieldStatusDTO> allFields = getDefaultStaticFieldDefinitions();
        int updatedCount = 0;
        
        for (StaticFieldStatusDTO field : allFields) {
            updateFieldMandatory(accountId, electionId, field.getFieldName(), true);
            updatedCount++;
        }
        
        log.info("Successfully made {} static fields required for electionId={}", updatedCount, electionId);
        return updatedCount;
    }

    /**
     * Make all static fields optional for an election (set mandatory = false for all fields)
     */
    @Transactional
    public int optionalAllFields(Long accountId, Long electionId) {
        log.info("Making all static fields optional for accountId={}, electionId={}", accountId, electionId);
        
        List<StaticFieldStatusDTO> allFields = getDefaultStaticFieldDefinitions();
        int updatedCount = 0;
        
        for (StaticFieldStatusDTO field : allFields) {
            updateFieldMandatory(accountId, electionId, field.getFieldName(), false);
            updatedCount++;
        }
        
        log.info("Successfully made {} static fields optional for electionId={}", updatedCount, electionId);
        return updatedCount;
    }
}