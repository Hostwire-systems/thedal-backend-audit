package com.thedal.thedal_app.migration.migrators;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.migration.GlobalMigrationJob;
import com.thedal.thedal_app.account.AccountEntity;
import com.thedal.thedal_app.account.AccountRepository;
import com.thedal.thedal_app.notification.NotificationsEntity;
import com.thedal.thedal_app.notification.NotificationsLogRepository;
import com.thedal.thedal_app.image.ImageEntity;
// import com.thedal.thedal_app.image.ImageRepository; // Repository not found
import com.thedal.thedal_app.profileAPI.ProfileEntity;
import com.thedal.thedal_app.profileAPI.ProfileRepository;
import com.thedal.thedal_app.profileAPI.CampaignSettingsEntity;
import com.thedal.thedal_app.profileAPI.ProfileRepository;
import com.thedal.thedal_app.election.TemplateEntity;
import com.thedal.thedal_app.election.TemplateRepository;
import com.thedal.thedal_app.election.SurveyFormSubmissionEntity;
import com.thedal.thedal_app.election.SurveyFormSubmissionRepository;
import com.thedal.thedal_app.settings.electionsettings.ComplaintEntity;
import com.thedal.thedal_app.settings.electionsettings.ComplaintRepository;
import com.thedal.thedal_app.cpanel.GeneralCpanelEntity;
import com.thedal.thedal_app.cpanel.GeneralCpanelRepository;
import com.thedal.thedal_app.voter.AadhaarEntity;
import com.thedal.thedal_app.voter.AadhaarRepository;
import com.thedal.thedal_app.voter.BulkUploadEntity;
// import com.thedal.thedal_app.voter.BulkUploadRepository; // Repository not found
import com.thedal.thedal_app.voter.BulkUploadMemberEntity;
import com.thedal.thedal_app.voter.BulkUploadMemberRepository;
import com.thedal.thedal_app.voter.BulkUploadErrorEntity;
import com.thedal.thedal_app.voter.BulkUploadErrorRepository;
import com.thedal.thedal_app.election.BoothBulkUploadEntity;
import com.thedal.thedal_app.election.BoothBulkUploadRepository;
import com.thedal.thedal_app.election.PartManagerBulkUploadEntity;
import com.thedal.thedal_app.election.PartManagerBulkUploadRepository;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionBulkUploadRepository;
import com.thedal.thedal_app.volunteer.VolunteerBulkUploadEntity;
import com.thedal.thedal_app.volunteer.VolunteerBulkUploadRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Universal migrator for entities that don't have specific MongoDB equivalents
 * This creates generic MongoDB documents preserving all data
 */
@Component
@Slf4j
public class UniversalMigrator {

    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // Repositories for entities without MongoDB equivalents
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private NotificationsLogRepository notificationsLogRepository;
    
    // @Autowired
    // private ImageRepository imageRepository; // Repository not found
    
    @Autowired
    private ProfileRepository profileRepository;
    
    // @Autowired
    // private CampaignSettingsRepository campaignSettingsRepository; // Repository not found
    
    @Autowired
    private TemplateRepository templateRepository;
    
    @Autowired
    private SurveyFormSubmissionRepository surveyFormSubmissionRepository;
    
    @Autowired
    private ComplaintRepository complaintRepository;
    
    @Autowired
    private GeneralCpanelRepository generalCpanelRepository;
    
    @Autowired
    private AadhaarRepository aadhaarRepository;
    
    // Bulk Upload Repositories
    // @Autowired
    // private BulkUploadRepository bulkUploadRepository; // Repository not found
    
    @Autowired
    private BulkUploadMemberRepository bulkUploadMemberRepository;
    
    @Autowired
    private BulkUploadErrorRepository bulkUploadErrorRepository;
    
    @Autowired
    private BoothBulkUploadRepository boothBulkUploadRepository;
    
    @Autowired
    private PartManagerBulkUploadRepository partManagerBulkUploadRepository;
    
    @Autowired
    private SectionBulkUploadRepository sectionBulkUploadRepository;
    
    @Autowired
    private VolunteerBulkUploadRepository volunteerBulkUploadRepository;
    
    /**
     * Migrate all accounts
     */
    public void migrateAllAccounts(GlobalMigrationJob job) {
        log.info("Starting migration of all accounts");
        migrateGenericEntity(job, accountRepository, "accounts", AccountEntity.class);
    }
    
    /**
     * Migrate all notifications
     */
    public void migrateAllNotifications(GlobalMigrationJob job) {
        log.info("Starting migration of all notifications");
        migrateGenericEntity(job, notificationsLogRepository, "notifications", NotificationsEntity.class);
    }
    
    /**
     * Migrate all images
     */
    public void migrateAllImages(GlobalMigrationJob job) {
        log.info("Starting migration of all images");
        // migrateGenericEntity(job, imageRepository, "images", ImageEntity.class); // Repository not found
    }
    
    /**
     * Migrate all profiles
     */
    public void migrateAllProfiles(GlobalMigrationJob job) {
        log.info("Starting migration of all profiles");
        migrateGenericEntity(job, profileRepository, "profiles", ProfileEntity.class);
    }
    
    /**
     * Migrate all campaign settings
     */
    public void migrateAllCampaignSettings(GlobalMigrationJob job) {
        log.info("Starting migration of all campaign settings");
        // migrateGenericEntity(job, campaignSettingsRepository, "campaign_settings", CampaignSettingsEntity.class); // Repository not found
    }
    
    /**
     * Migrate all templates
     */
    public void migrateAllTemplates(GlobalMigrationJob job) {
        log.info("Starting migration of all templates");
        migrateGenericEntity(job, templateRepository, "templates", TemplateEntity.class);
    }
    
    /**
     * Migrate all survey form submissions
     */
    public void migrateAllSurveyFormSubmissions(GlobalMigrationJob job) {
        log.info("Starting migration of all survey form submissions");
        migrateGenericEntity(job, surveyFormSubmissionRepository, "survey_form_submissions", SurveyFormSubmissionEntity.class);
    }
    
    /**
     * Migrate all complaints
     */
    public void migrateAllComplaints(GlobalMigrationJob job) {
        log.info("Starting migration of all complaints");
        migrateGenericEntity(job, complaintRepository, "complaints", ComplaintEntity.class);
    }
    
    /**
     * Migrate all general cpanel data
     */
    public void migrateAllGeneralCpanel(GlobalMigrationJob job) {
        log.info("Starting migration of all general cpanel data");
        migrateGenericEntity(job, generalCpanelRepository, "general_cpanel", GeneralCpanelEntity.class);
    }
    
    /**
     * Migrate all aadhaar data
     */
    public void migrateAllAadhaar(GlobalMigrationJob job) {
        log.info("Starting migration of all aadhaar data");
        migrateGenericEntity(job, aadhaarRepository, "aadhaar", AadhaarEntity.class);
    }
    
    /**
     * Migrate all bulk upload data
     */
    public void migrateAllBulkUploads(GlobalMigrationJob job) {
        log.info("Starting migration of all bulk upload data");
        // migrateGenericEntity(job, bulkUploadRepository, "bulk_uploads", BulkUploadEntity.class); // Repository not found
    }
    
    /**
     * Migrate all bulk upload member data
     */
    public void migrateAllBulkUploadMembers(GlobalMigrationJob job) {
        log.info("Starting migration of all bulk upload member data");
        migrateGenericEntity(job, bulkUploadMemberRepository, "bulk_upload_members", BulkUploadMemberEntity.class);
    }
    
    /**
     * Migrate all bulk upload error data
     */
    public void migrateAllBulkUploadErrors(GlobalMigrationJob job) {
        log.info("Starting migration of all bulk upload error data");
        migrateGenericEntity(job, bulkUploadErrorRepository, "bulk_upload_errors", BulkUploadErrorEntity.class);
    }
    
    /**
     * Migrate all booth bulk upload data
     */
    public void migrateAllBoothBulkUploads(GlobalMigrationJob job) {
        log.info("Starting migration of all booth bulk upload data");
        migrateGenericEntity(job, boothBulkUploadRepository, "booth_bulk_uploads", BoothBulkUploadEntity.class);
    }
    
    /**
     * Migrate all part manager bulk upload data
     */
    public void migrateAllPartManagerBulkUploads(GlobalMigrationJob job) {
        log.info("Starting migration of all part manager bulk upload data");
        migrateGenericEntity(job, partManagerBulkUploadRepository, "part_manager_bulk_uploads", PartManagerBulkUploadEntity.class);
    }
    
    /**
     * Migrate all section bulk upload data
     */
    public void migrateAllSectionBulkUploads(GlobalMigrationJob job) {
        log.info("Starting migration of all section bulk upload data");
        migrateGenericEntity(job, sectionBulkUploadRepository, "section_bulk_uploads", SectionBulkUploadEntity.class);
    }
    
    /**
     * Migrate all volunteer bulk upload data
     */
    public void migrateAllVolunteerBulkUploads(GlobalMigrationJob job) {
        log.info("Starting migration of all volunteer bulk upload data");
        migrateGenericEntity(job, volunteerBulkUploadRepository, "volunteer_bulk_uploads", VolunteerBulkUploadEntity.class);
    }
    
    /**
     * Generic migration method for any entity
     */
    private <T> void migrateGenericEntity(GlobalMigrationJob job, org.springframework.data.repository.PagingAndSortingRepository<T, Long> repository, String collectionName, Class<T> entityClass) {
        AtomicLong processedCount = new AtomicLong(0);
        int batchSize = job.getBatchSize();
        int pageNumber = 0;
        
        try {
            while (!job.isCancelled()) {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<T> entityPage = repository.findAll(pageable);
                
                if (entityPage.isEmpty()) {
                    break;
                }
                
                List<T> entities = entityPage.getContent();
                
                for (T entity : entities) {
                    if (job.isCancelled()) break;
                    
                    try {
                        // Convert entity to MongoDB document
                        GenericMongoDocument mongoDoc = convertToMongoDocument(entity, entityClass);
                        
                        if (job.isOverwriteExisting()) {
                            mongoTemplate.save(mongoDoc, collectionName);
                        } else {
                            // Check if document exists
                            Query query = new Query(Criteria.where("originalId").is(mongoDoc.getOriginalId()));
                            if (!mongoTemplate.exists(query, collectionName)) {
                                mongoTemplate.save(mongoDoc, collectionName);
                            }
                        }
                        
                        processedCount.incrementAndGet();
                        
                        if (processedCount.get() % 100 == 0) {
                            log.info("Processed {} {} entities", processedCount.get(), entityClass.getSimpleName());
                        }
                        
                    } catch (Exception e) {
                        log.error("Error migrating {} entity: {}", entityClass.getSimpleName(), e.getMessage());
                        job.incrementFailedRecords(1);
                    }
                }
                
                pageNumber++;
            }
            
            log.info("{} migration completed. Processed: {}", entityClass.getSimpleName(), processedCount.get());
            
        } catch (Exception e) {
            log.error("Error during {} migration: {}", entityClass.getSimpleName(), e.getMessage(), e);
            throw new RuntimeException("Failed to migrate " + entityClass.getSimpleName(), e);
        }
    }
    
    /**
     * Convert any entity to a generic MongoDB document
     */
    private <T> GenericMongoDocument convertToMongoDocument(T entity, Class<T> entityClass) {
        try {
            GenericMongoDocument mongoDoc = new GenericMongoDocument();
            
            // Convert entity to map to preserve all fields
            String jsonString = objectMapper.writeValueAsString(entity);
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> entityMap = objectMapper.readValue(jsonString, java.util.Map.class);
            
            // Set basic fields
            mongoDoc.setOriginalId(extractId(entity));
            mongoDoc.setEntityType(entityClass.getSimpleName());
            mongoDoc.setData(entityMap);
            mongoDoc.setMigratedAt(java.time.LocalDateTime.now());
            
            return mongoDoc;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert entity to MongoDB document", e);
        }
    }
    
    /**
     * Extract ID from entity using reflection
     */
    private Long extractId(Object entity) {
        try {
            // Try common ID field names
            java.lang.reflect.Field idField = null;
            
            try {
                idField = entity.getClass().getDeclaredField("id");
            } catch (NoSuchFieldException e) {
                // Try other common ID field names
                for (java.lang.reflect.Field field : entity.getClass().getDeclaredFields()) {
                    if (field.getName().toLowerCase().contains("id") && 
                        (field.getType() == Long.class || field.getType() == long.class)) {
                        idField = field;
                        break;
                    }
                }
            }
            
            if (idField != null) {
                idField.setAccessible(true);
                Object idValue = idField.get(entity);
                return idValue instanceof Long ? (Long) idValue : null;
            }
            
            return null;
            
        } catch (Exception e) {
            log.warn("Could not extract ID from entity: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Generic MongoDB document structure
     */
    public static class GenericMongoDocument {
        private String id;
        private Long originalId;
        private String entityType;
        private java.util.Map<String, Object> data;
        private java.time.LocalDateTime migratedAt;
        
        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        
        public Long getOriginalId() { return originalId; }
        public void setOriginalId(Long originalId) { this.originalId = originalId; }
        
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        
        public java.util.Map<String, Object> getData() { return data; }
        public void setData(java.util.Map<String, Object> data) { this.data = data; }
        
        public java.time.LocalDateTime getMigratedAt() { return migratedAt; }
        public void setMigratedAt(java.time.LocalDateTime migratedAt) { this.migratedAt = migratedAt; }
    }
}
