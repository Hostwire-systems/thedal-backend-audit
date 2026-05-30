package com.thedal.thedal_app.migration.migrators;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.migration.MigrationJob;
import com.thedal.thedal_app.migration.MigrationJobStatus;
import com.thedal.thedal_app.migration.MigrationStats;
import com.thedal.thedal_app.migration.ValidationReport;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongo;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongo;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteMongo;
import com.thedal.thedal_app.settings.electionsettings.CasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongo;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueRepository;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongo;
import com.thedal.thedal_app.settings.electionsettings.LanguageMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.LanguageRepository;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyMongo;
import com.thedal.thedal_app.settings.electionsettings.PartyMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongo;
import com.thedal.thedal_app.settings.electionsettings.ReligionMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongo;
import com.thedal.thedal_app.settings.electionsettings.SubCasteMongoRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SettingsMigrator {

    // Religion
    @Autowired
    private ReligionRepository religionRepository;
    @Autowired
    private ReligionMongoRepository religionMongoRepository;

    // Caste
    @Autowired
    private CasteRepository casteRepository;
    @Autowired
    private CasteMongoRepository casteMongoRepository;

    // SubCaste
    @Autowired
    private SubCasteRepository subCasteRepository;
    @Autowired
    private SubCasteMongoRepository subCasteMongoRepository;

    // Party
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private PartyMongoRepository partyMongoRepository;

    // Language
    @Autowired
    private LanguageRepository languageRepository;
    @Autowired
    private LanguageMongoRepository languageMongoRepository;

    // BenefitSchemes
    @Autowired
    private BenefitSchemesRepository benefitSchemesRepository;
    @Autowired
    private BenefitSchemesMongoRepository benefitSchemesMongoRepository;

    // FeedbackIssue
    @Autowired
    private FeedbackIssueRepository feedbackIssueRepository;
    @Autowired
    private FeedbackIssueMongoRepository feedbackIssueMongoRepository;

    // Availability
    @Autowired
    private AvailabilityRepository availabilityRepository;
    @Autowired
    private AvailabilityMongoRepository availabilityMongoRepository;

    @Transactional(readOnly = true)
    public void migrate(MigrationJob job, int batchSize, boolean overwriteExisting) {
        log.info("Starting settings migration for accountId: {}, electionId: {}", 
            job.getAccountId(), job.getElectionId());

        try {
            // Migrate in dependency order
            migrateReligion(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateCaste(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateSubCaste(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateParty(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateLanguage(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateBenefitSchemes(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateFeedbackIssue(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            migrateAvailability(job, batchSize, overwriteExisting);
            if (job.isCancelled()) return;

            log.info("Settings migration completed for accountId: {}, electionId: {}", 
                job.getAccountId(), job.getElectionId());

        } catch (Exception e) {
            log.error("Error during settings migration: {}", e.getMessage(), e);
            job.setStatus(MigrationJobStatus.FAILED);
            job.setErrorMessage("Settings migration failed: " + e.getMessage());
            throw e;
        }
    }

    public void migrateModule(MigrationJob job, String module, int batchSize, boolean overwriteExisting) {
        log.info("Starting {} migration for accountId: {}, electionId: {}", 
            module, job.getAccountId(), job.getElectionId());

        try {
            switch (module.toLowerCase()) {
                case "religion":
                    migrateReligion(job, batchSize, overwriteExisting);
                    break;
                case "caste":
                    migrateCaste(job, batchSize, overwriteExisting);
                    break;
                case "subcaste":
                    migrateSubCaste(job, batchSize, overwriteExisting);
                    break;
                case "party":
                    migrateParty(job, batchSize, overwriteExisting);
                    break;
                case "language":
                    migrateLanguage(job, batchSize, overwriteExisting);
                    break;
                case "benefitschemes":
                    migrateBenefitSchemes(job, batchSize, overwriteExisting);
                    break;
                case "feedbackissue":
                    migrateFeedbackIssue(job, batchSize, overwriteExisting);
                    break;
                case "availability":
                    migrateAvailability(job, batchSize, overwriteExisting);
                    break;
                default:
                    log.warn("Unknown settings module: {}", module);
            }
        } catch (Exception e) {
            log.error("Error during {} migration: {}", module, e.getMessage(), e);
            throw e;
        }
    }

    // Individual module migration methods

    private void migrateReligion(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<ReligionEntity> religions = religionRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (religions.isEmpty()) {
            log.info("No religions found to migrate");
            return;
        }

        if (overwriteExisting) {
            religionMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} religions", religions.size());
        
        for (ReligionEntity religion : religions) {
            if (job.isCancelled()) return;
            
            try {
                ReligionMongo religionMongo = new ReligionMongo(religion);
                religionMongoRepository.save(religionMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate religion {}: {}", religion.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateCaste(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<CasteEntity> castes = casteRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (castes.isEmpty()) {
            log.info("No castes found to migrate");
            return;
        }

        if (overwriteExisting) {
            casteMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} castes", castes.size());
        
        for (CasteEntity caste : castes) {
            if (job.isCancelled()) return;
            
            try {
                CasteMongo casteMongo = new CasteMongo(caste);
                casteMongoRepository.save(casteMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate caste {}: {}", caste.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateSubCaste(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<SubCasteEntity> subCastes = subCasteRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (subCastes.isEmpty()) {
            log.info("No sub-castes found to migrate");
            return;
        }

        if (overwriteExisting) {
            subCasteMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} sub-castes", subCastes.size());
        
        for (SubCasteEntity subCaste : subCastes) {
            if (job.isCancelled()) return;
            
            try {
                SubCasteMongo subCasteMongo = new SubCasteMongo(subCaste);
                subCasteMongoRepository.save(subCasteMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate sub-caste {}: {}", subCaste.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateParty(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<Party> parties = partyRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (parties.isEmpty()) {
            log.info("No parties found to migrate");
            return;
        }

        if (overwriteExisting) {
            partyMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} parties", parties.size());
        
        for (Party party : parties) {
            if (job.isCancelled()) return;
            
            try {
                PartyMongo partyMongo = new PartyMongo(party);
                partyMongoRepository.save(partyMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate party {}: {}", party.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateLanguage(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<Language> languages = languageRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (languages.isEmpty()) {
            log.info("No languages found to migrate");
            return;
        }

        if (overwriteExisting) {
            languageMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} languages", languages.size());
        
        for (Language language : languages) {
            if (job.isCancelled()) return;
            
            try {
                LanguageMongo languageMongo = new LanguageMongo(language);
                languageMongoRepository.save(languageMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate language {}: {}", language.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateBenefitSchemes(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<BenefitSchemes> benefitSchemes = benefitSchemesRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (benefitSchemes.isEmpty()) {
            log.info("No benefit schemes found to migrate");
            return;
        }

        if (overwriteExisting) {
            benefitSchemesMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} benefit schemes", benefitSchemes.size());
        
        for (BenefitSchemes benefitScheme : benefitSchemes) {
            if (job.isCancelled()) return;
            
            try {
                BenefitSchemesMongo benefitSchemeMongo = new BenefitSchemesMongo(benefitScheme);
                benefitSchemesMongoRepository.save(benefitSchemeMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate benefit scheme {}: {}", benefitScheme.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateFeedbackIssue(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<FeedbackIssue> feedbackIssues = feedbackIssueRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (feedbackIssues.isEmpty()) {
            log.info("No feedback issues found to migrate");
            return;
        }

        if (overwriteExisting) {
            feedbackIssueMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} feedback issues", feedbackIssues.size());
        
        for (FeedbackIssue feedbackIssue : feedbackIssues) {
            if (job.isCancelled()) return;
            
            try {
                FeedbackIssueMongo feedbackIssueMongo = new FeedbackIssueMongo(feedbackIssue);
                feedbackIssueMongoRepository.save(feedbackIssueMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate feedback issue {}: {}", feedbackIssue.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    private void migrateAvailability(MigrationJob job, int batchSize, boolean overwriteExisting) {
        List<Availability> availabilities = availabilityRepository.findByAccountIdAndElectionId(
            job.getAccountId(), job.getElectionId());
        
        if (availabilities.isEmpty()) {
            log.info("No availabilities found to migrate");
            return;
        }

        if (overwriteExisting) {
            availabilityMongoRepository.deleteByAccountIdAndElectionId(job.getAccountId(), job.getElectionId());
        }

        log.info("Migrating {} availabilities", availabilities.size());
        
        for (Availability availability : availabilities) {
            if (job.isCancelled()) return;
            
            try {
                AvailabilityMongo availabilityMongo = new AvailabilityMongo(availability);
                availabilityMongoRepository.save(availabilityMongo);
                job.setProcessedRecords(job.getProcessedRecords() + 1);
            } catch (Exception e) {
                log.error("Failed to migrate availability {}: {}", availability.getId(), e.getMessage());
                job.setFailedRecords(job.getFailedRecords() + 1);
            }
        }
    }

    // Validation methods

    public Map<String, ValidationReport.EntityValidationResult> validateConsistency(Long accountId, Long electionId) {
        Map<String, ValidationReport.EntityValidationResult> results = new HashMap<>();

        results.put("religion", validateEntityConsistency("religion", 
            religionRepository.countByAccountIdAndElectionId(accountId, electionId),
            religionMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("caste", validateEntityConsistency("caste", 
            casteRepository.countByAccountIdAndElectionId(accountId, electionId),
            casteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("subcaste", validateEntityConsistency("subcaste", 
            subCasteRepository.countByAccountIdAndElectionId(accountId, electionId),
            subCasteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("party", validateEntityConsistency("party", 
            partyRepository.countByAccountIdAndElectionId(accountId, electionId),
            partyMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("language", validateEntityConsistency("language", 
            languageRepository.countByAccountIdAndElectionId(accountId, electionId),
            languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("benefitschemes", validateEntityConsistency("benefitschemes", 
            benefitSchemesRepository.countByAccountIdAndElectionId(accountId, electionId),
            benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("feedbackissue", validateEntityConsistency("feedbackissue", 
            feedbackIssueRepository.countByAccountIdAndElectionId(accountId, electionId),
            feedbackIssueMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        results.put("availability", validateEntityConsistency("availability", 
            availabilityRepository.countByAccountIdAndElectionId(accountId, electionId),
            availabilityMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size()));

        return results;
    }

    private ValidationReport.EntityValidationResult validateEntityConsistency(String entityName, long postgresCount, long mongoCount) {
        boolean isConsistent = (postgresCount == mongoCount);
        String discrepancyDetails = isConsistent ? "Counts match" : 
            String.format("PostgreSQL: %d, MongoDB: %d (difference: %d)", 
                postgresCount, mongoCount, Math.abs(postgresCount - mongoCount));
        
        return new ValidationReport.EntityValidationResult(entityName, postgresCount, mongoCount, 
            isConsistent, discrepancyDetails);
    }

    // Stats methods

    public Map<String, MigrationStats.EntityStats> getEntityStats(Long accountId, Long electionId) {
        Map<String, MigrationStats.EntityStats> stats = new HashMap<>();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        stats.put("religion", new MigrationStats.EntityStats("religion", 
            religionRepository.countByAccountIdAndElectionId(accountId, electionId),
            religionMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            religionMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("caste", new MigrationStats.EntityStats("caste", 
            casteRepository.countByAccountIdAndElectionId(accountId, electionId),
            casteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            casteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("subcaste", new MigrationStats.EntityStats("subcaste", 
            subCasteRepository.countByAccountIdAndElectionId(accountId, electionId),
            subCasteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            subCasteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("party", new MigrationStats.EntityStats("party", 
            partyRepository.countByAccountIdAndElectionId(accountId, electionId),
            partyMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            partyMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("language", new MigrationStats.EntityStats("language", 
            languageRepository.countByAccountIdAndElectionId(accountId, electionId),
            languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("benefitschemes", new MigrationStats.EntityStats("benefitschemes", 
            benefitSchemesRepository.countByAccountIdAndElectionId(accountId, electionId),
            benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("feedbackissue", new MigrationStats.EntityStats("feedbackissue", 
            feedbackIssueRepository.countByAccountIdAndElectionId(accountId, electionId),
            feedbackIssueMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            feedbackIssueMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        stats.put("availability", new MigrationStats.EntityStats("availability", 
            availabilityRepository.countByAccountIdAndElectionId(accountId, electionId),
            availabilityMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size(),
            availabilityMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size() > 0, timestamp));

        return stats;
    }

    // Cleanup methods

    public Map<String, Long> cleanupMongoData(Long accountId, Long electionId, boolean dryRun) {
        Map<String, Long> deletedCounts = new HashMap<>();

        if (!dryRun) {
            deletedCounts.put("religion", (long) religionMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            religionMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("caste", (long) casteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            casteMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("subcaste", (long) subCasteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            subCasteMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("party", (long) partyMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            partyMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("language", (long) languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            languageMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("benefitschemes", (long) benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            benefitSchemesMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("feedbackissue", (long) feedbackIssueMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            feedbackIssueMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId);

            deletedCounts.put("availability", availabilityMongoRepository.deleteByAccountIdAndElectionId(accountId, electionId));
        } else {
            deletedCounts.put("religion", (long) religionMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("caste", (long) casteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("subcaste", (long) subCasteMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("party", (long) partyMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("language", (long) languageMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("benefitschemes", (long) benefitSchemesMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("feedbackissue", (long) feedbackIssueMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
            deletedCounts.put("availability", (long) availabilityMongoRepository.findByAccountIdAndElectionId(accountId, electionId).size());
        }

        return deletedCounts;
    }
}
