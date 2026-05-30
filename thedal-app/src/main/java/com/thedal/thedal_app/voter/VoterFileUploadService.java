package com.thedal.thedal_app.voter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Array;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.apache.poi.util.IOUtils;

import com.github.pjfanning.xlsx.StreamingReader;
import com.thedal.thedal_app.election.ElectionBooth;
import com.thedal.thedal_app.election.ElectionBoothRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;
import com.thedal.thedal_app.election.DynamicFieldEntity;
import com.thedal.thedal_app.election.DynamicFieldRepository;
import com.thedal.thedal_app.settings.electionsettings.SectionRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionEntity;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteEntity;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryRepository;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.Language;
import com.thedal.thedal_app.settings.electionsettings.LanguageRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesRepository;
import com.thedal.thedal_app.settings.electionsettings.Availability;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueRepository;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.cpanel.VoterHistoryRepository;
import com.thedal.thedal_app.voter.VoterBenefitScheme;
import com.thedal.thedal_app.voter.VoterBenefitSchemeRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class VoterFileUploadService {
	
    // Static block to increase POI byte array limit for large files
    static {
        // Increase Apache POI's byte array limit from 100MB to 500MB for large Excel files
        IOUtils.setByteArrayMaxOverride(500_000_000); // 500MB limit
        log.info("POI byte array limit set to 500MB for large file processing");
    }
    
	private static final int BATCH_SIZE = 2000; // Reduced for better memory management with large files
    private final ExecutorService executorService = Executors.newFixedThreadPool(16);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final VoterRepo voterRepository;
    private final BulkUploadRepo bulkUploadRepo;
    private final BulkUploadErrorRepository bulkUploadErrorRepository;
    private final SectionRepository sectionRepository; 
    private final ElectionBoothRepository electionBoothRepository;
    private final PartManagerRepository partManagerRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ReligionRepository religionRepository;
    private final CasteRepository casteRepository;
    private final SubCasteRepository subCasteRepository;
    private final CasteCategoryRepository casteCategoryRepository;
    private final PartyRepository partyRepository;
    private final LanguageRepository languageRepository;
    private final BenefitSchemesRepository benefitSchemesRepository;
    private final AvailabilityRepository availabilityRepository;
    private final FeedbackIssueRepository feedbackIssueRepository;
    private final VoterHistoryRepository voterHistoryRepository;
    private final VoterBenefitSchemeRepository voterBenefitSchemeRepository;
    private final VoterReferenceDataService voterReferenceDataService;
    private final DynamicFieldRepository dynamicFieldRepository;
    

    @Autowired
    public VoterFileUploadService(VoterRepo voterRepository, BulkUploadRepo bulkUploadRepo,
            BulkUploadErrorRepository bulkUploadErrorRepository,
            SectionRepository sectionRepository, ElectionBoothRepository electionBoothRepository,
            PartManagerRepository partManagerRepository,
            VoterMongoRepository voterMongoRepository,
            JdbcTemplate jdbcTemplate,
            ReligionRepository religionRepository,
            CasteRepository casteRepository,
            SubCasteRepository subCasteRepository,
            CasteCategoryRepository casteCategoryRepository,
            PartyRepository partyRepository,
            LanguageRepository languageRepository,
            BenefitSchemesRepository benefitSchemesRepository,
            AvailabilityRepository availabilityRepository,
            FeedbackIssueRepository feedbackIssueRepository,
            VoterHistoryRepository voterHistoryRepository,
            VoterBenefitSchemeRepository voterBenefitSchemeRepository,
            VoterReferenceDataService voterReferenceDataService,
            DynamicFieldRepository dynamicFieldRepository) {
        this.voterRepository = voterRepository;
        this.bulkUploadRepo = bulkUploadRepo;
        this.bulkUploadErrorRepository = bulkUploadErrorRepository;
        this.sectionRepository = sectionRepository;
        this.electionBoothRepository = electionBoothRepository;
        this.partManagerRepository = partManagerRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.religionRepository = religionRepository;
        this.casteRepository = casteRepository;
        this.subCasteRepository = subCasteRepository;
        this.casteCategoryRepository = casteCategoryRepository;
        this.partyRepository = partyRepository;
        this.languageRepository = languageRepository;
        this.benefitSchemesRepository = benefitSchemesRepository;
        this.availabilityRepository = availabilityRepository;
        this.feedbackIssueRepository = feedbackIssueRepository;
        this.voterHistoryRepository = voterHistoryRepository;
        this.voterBenefitSchemeRepository = voterBenefitSchemeRepository;
        this.voterReferenceDataService = voterReferenceDataService;
        this.dynamicFieldRepository = dynamicFieldRepository;
    }
    
    // @Async - REMOVED: Let Quartz handle threading to avoid double async issues
public void processExcelFileAsync(Long bulkUploadId,
                                  Long accountId,
                                  Long electionId,
                                  String fileUrl,
                                  Map<String, Integer> headerMapping,
                                  Set<String> mandatoryHeaders) throws IOException {
    System.out.println("=== EXCEL PROCESSING STARTED ===");
    System.out.println("BulkUploadId: " + bulkUploadId + ", AccountId: " + accountId + ", ElectionId: " + electionId);
    
    long startTime = System.currentTimeMillis();
    BulkUploadEntity bulkUpload = bulkUploadRepo.findById(bulkUploadId)
        .orElseThrow(() -> new IllegalArgumentException(
            "BulkUploadEntity with ID " + bulkUploadId + " not found"));

    // Create dynamic field definitions for any custom fields in the CSV
    createDynamicFieldDefinitions(headerMapping, accountId, electionId);

    // ─── StreamingReader instead of XSSFWorkbook ────────────────────────────────
    try (InputStream is = new URL(fileUrl).openStream();
         Workbook wb = StreamingReader.builder()
             .rowCacheSize(50)       // Reduced cache for large files  
             .bufferSize(8 * 1024)   // Increased buffer for better I/O
             .open(is)) {

        int numSheets = wb.getNumberOfSheets();
        log.debug("Number of sheets in streaming workbook: {}", numSheets);

        // pick sheet 1 (index=1), or fallback if missing
        Sheet sheet = wb.getNumberOfSheets() > 1
            ? wb.getSheetAt(1)
            : wb.getSheetAt(0);

//        if (sheet == null || sheet.getRow(0) == null) {
//            throw new IOException("No header row found in any sheet");
//        }
        if (sheet == null) {
            throw new IOException("No sheets found in the workbook");
        }

        // Use iterator to check for header row
        Iterator<Row> rowIterator = sheet.iterator();
        if (!rowIterator.hasNext()) {
            throw new IOException("No header row found in sheet: " + sheet.getSheetName());
        }

        log.info("Processing sheet: {}", sheet.getSheetName());
        System.out.println("About to call processFileAsync with " + sheet.getSheetName());
        processFileAsync(
            sheet.iterator(),
            headerMapping,
            accountId,
            electionId,
            true,
            bulkUpload,
            mandatoryHeaders
        );
        log.info("Total processing time for Excel file upload: {} ms",
                 System.currentTimeMillis() - startTime);
        System.out.println("=== EXCEL PROCESSING COMPLETED ===");

    } catch (Exception e) {
        log.error("Error processing Excel file (streaming): {}", e.getMessage(), e);
        bulkUpload.setStatus(BulkUploadStatus.FAILED);
        bulkUpload.setEndTime(LocalDateTime.now());
        bulkUploadRepo.save(bulkUpload);
        throw new IOException("Failed to process Excel file: " + e.getMessage(), e);
    }
}

    // @Async - REMOVED: Let Quartz handle threading to avoid double async issues
    public void processCsvFileAsync(Long bulkUploadId, Long accountId, Long electionId, String fileUrl,
                                    Map<String, Integer> headerMapping, Set<String> mandatoryHeaders) throws IOException {
        long startTime = System.currentTimeMillis();
        BulkUploadEntity bulkUpload = bulkUploadRepo.findById(bulkUploadId)
                .orElseThrow(() -> new IllegalArgumentException("BulkUploadEntity with ID " + bulkUploadId + " not found"));

        // Create dynamic field definitions for any custom fields in the CSV
        createDynamicFieldDefinitions(headerMapping, accountId, electionId);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream()))) {
            br.readLine(); // Skip header row since headerMapping is provided
            processFileAsync(br.lines().iterator(), headerMapping, accountId, electionId, false, bulkUpload, mandatoryHeaders);
            log.info("Total processing time for CSV file upload: {} ms", (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error("Error processing CSV file: {}", e.getMessage(), e);
            bulkUpload.setStatus(BulkUploadStatus.FAILED);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUploadRepo.save(bulkUpload);
            throw new IOException("Failed to process CSV file: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void processFileAsync(Iterator<?> dataIterator, Map<String, Integer> headerMapping, Long accountId,
                                Long electionId, boolean isExcel, BulkUploadEntity bulkUpload, Set<String> mandatoryHeaders) throws IOException {
        System.out.println("=== CORE PROCESSING STARTED ===");
        System.out.println("Election: " + electionId + ", Account: " + accountId + ", BulkUpload: " + bulkUpload.getId());
        
        // Memory monitoring for large file uploads
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long initialUsedMemory = runtime.totalMemory() - runtime.freeMemory();
        log.info("MEMORY MONITORING - Max: {} MB, Initial Used: {} MB", 
                 maxMemory / (1024 * 1024), initialUsedMemory / (1024 * 1024));
        
        long startTime = System.currentTimeMillis();
    List<VoterEntity> voterBatch = new ArrayList<>();
    Map<String, List<String>> voterBenefitSchemesMap = new HashMap<>(); // Track benefit schemes per voter
    Set<String> uploadEpicNumbers = new HashSet<>();
        int totalRecords = 0;
        Integer preCount = null; // Track initial voter count for debugging

        // Validate database constraints before processing
        log.info("Validating database constraints for bulk upload {}", bulkUpload.getId());
        validateVoterConstraints();
        
        // Enhanced debugging: Check current voter count for this election before processing
        String preCountSql = "SELECT COUNT(*) FROM _voters WHERE election_id = ? AND account_id = ?";
        preCount = jdbcTemplate.queryForObject(preCountSql, Integer.class, electionId, accountId);
        log.info("PRE-PROCESSING: Election {} Account {} currently has {} voters", electionId, accountId, preCount != null ? preCount : 0);

        long cacheStart = System.currentTimeMillis();
        Map<String, PartManager> partManagerCache = partManagerRepository
                .findByAccountIdAndElectionId(accountId, electionId)
                .stream()
                .collect(Collectors.toMap(PartManager::getPartNo, pm -> pm));
        log.info("PartManager cache loading time: {} ms", System.currentTimeMillis() - cacheStart);
      
        Map<Integer, ElectionBooth> boothCache = electionBoothRepository.findByElectionIdAndAccountId(electionId, accountId)
                .stream().collect(Collectors.toMap(ElectionBooth::getBoothNumber, b -> b));
        Integer maxBoothOrderIndex = electionBoothRepository.findMaxOrderIndexByElectionId(electionId);
        AtomicInteger boothOrderCounter = new AtomicInteger((maxBoothOrderIndex != null) ? maxBoothOrderIndex + 1 : 0);
        Set<Integer> boothNumbersSet = new HashSet<>();
        
        // *** NEW: Pre-load reference data caches for dynamic fields (like election merge) ***
        Map<String, ReligionEntity> religionCache = religionRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(r -> normalize(r.getReligionName()), r -> r, (a,b)->a));
        Map<String, CasteEntity> casteCache = casteRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(c -> normalize(c.getCasteName()), c -> c, (a,b)->a));
        Map<String, SubCasteEntity> subCasteCache = subCasteRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(sc -> normalize(sc.getSubCasteName()), sc -> sc, (a,b)->a));
        Map<String, CasteCategoryEntity> casteCategoryCache = casteCategoryRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(cc -> normalize(cc.getCasteCategoryName()), cc -> cc, (a,b)->a));
        Map<String, Party> partyCache = partyRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(p -> normalize(p.getPartyName()), p -> p, (a,b)->a));
        Map<String, Language> languageCache = languageRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(l -> normalize(l.getLanguageName()), l -> l, (a,b)->a));
        Map<String, BenefitSchemes> benefitSchemeCache = benefitSchemesRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(bs -> normalize(bs.getSchemeName()), bs -> bs, (a,b)->a));
        Map<String, Availability> availabilityCache = availabilityRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(a -> normalize(a.getCategoryName()), a -> a, (a,b)->a));
        Map<String, FeedbackIssue> feedbackIssueCache = feedbackIssueRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(fi -> normalize(fi.getIssueName()), fi -> fi, (a,b)->a));
        Map<String, VoterHistoryEntity> voterHistoryCache = voterHistoryRepository
            .findByAccountIdAndElectionId(accountId, electionId)
            .stream()
            .collect(Collectors.toMap(vh -> normalize(vh.getVoterHistoryName()), vh -> vh, (a,b)->a));
        
        log.info("Cache loading time: {} ms", System.currentTimeMillis() - cacheStart);
        log.info("=== CACHE SIZES ===");
        log.info("Languages: {}", languageCache.size());
        log.info("Benefit Schemes: {}", benefitSchemeCache.size());
        log.info("Availability: {}", availabilityCache.size());
        log.info("Feedback Issues: {}", feedbackIssueCache.size());
        log.info("Voter Histories: {}", voterHistoryCache.size());
        log.info("=== HEADER MAPPING KEYS ===");
        log.info("Header mapping contains: {}", String.join(", ", headerMapping.keySet()));
        log.info("Looking for: languages, benefit_schemes_name, availability_category_name, feedback_issue_names, voter_history_names");

        try {
            if (isExcel) dataIterator.next();

            while (dataIterator.hasNext()) {
                totalRecords++;
                Object currentRow = dataIterator.next();

                VoterEntity voterEntity = isExcel
                        ? mapToVoterEntityDynamic((Row) currentRow, headerMapping, accountId, electionId, partManagerCache)
                        : mapToVoterEntityFromCsv(parseCsvLine((String) currentRow), headerMapping, accountId, electionId, partManagerCache);
                voterEntity.setElectionId(electionId);
                voterEntity.setVoterId(voterEntity.getEpicNumber());

                // Validate epic_number and other required fields
                if (voterEntity.getEpicNumber() == null || voterEntity.getEpicNumber().trim().isEmpty()) {
                    log.warn("Skipping voter with empty epic_number at record {}", totalRecords);
                    bulkUpload.setTotalFailedVoters(bulkUpload.getTotalFailedVoters() + 1);
                    continue;
                }
                
                // Additional validation for required fields
                if (voterEntity.getElectionId() == null || voterEntity.getAccountId() == null) {
                    log.warn("Skipping voter with missing election_id or account_id at record {}: epic={}", 
                             totalRecords, voterEntity.getEpicNumber());
                    bulkUpload.setTotalFailedVoters(bulkUpload.getTotalFailedVoters() + 1);
                    continue;
                }
                
                // *** NEW: Resolve dynamic field relationships (optional, creates missing data) ***
                try {
                    if (isExcel) {
                        resolveVoterRelationships((Row) currentRow, headerMapping, voterEntity, accountId, electionId,
                            religionCache, casteCache, subCasteCache, casteCategoryCache, partyCache,
                            languageCache, benefitSchemeCache, availabilityCache, feedbackIssueCache, voterHistoryCache);
                    } else {
                        // For CSV: resolve using parsed fields array
                        List<String> benefitSchemeNames = resolveVoterRelationshipsFromCsv(parseCsvLine((String) currentRow), headerMapping, voterEntity, accountId, electionId,
                            religionCache, casteCache, subCasteCache, casteCategoryCache, partyCache,
                            languageCache, benefitSchemeCache, availabilityCache, feedbackIssueCache, voterHistoryCache);
                        if (!benefitSchemeNames.isEmpty()) {
                            voterBenefitSchemesMap.put(voterEntity.getEpicNumber(), benefitSchemeNames);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to resolve relationships for voter {}: {}", voterEntity.getEpicNumber(), e.getMessage());
                    // Continue processing - relationships are optional
                }

                voterBatch.add(voterEntity);
                if (voterEntity.getEpicNumber() != null) {
                    uploadEpicNumbers.add(voterEntity.getEpicNumber());
                }

                if (voterBatch.size() >= BATCH_SIZE) {
                    // Memory check before processing large batches
                    long currentUsedMemory = runtime.totalMemory() - runtime.freeMemory();
                    log.info("Processing batch of {} voters. Current memory usage: {} MB", 
                             voterBatch.size(), currentUsedMemory / (1024 * 1024));
                    
                    try {
                        processBatch(voterBatch, bulkUpload, accountId, electionId, partManagerCache, boothCache, boothNumbersSet, boothOrderCounter);
                        // Create benefit scheme join entities after batch save
                        createBenefitSchemeJoinEntities(voterBatch, voterBenefitSchemesMap, accountId, electionId, benefitSchemeCache);
                    } catch (Exception e) {
                        log.error("Failed to process batch of {} voters for election {} account {}: {}", 
                                  voterBatch.size(), electionId, accountId, e.getMessage(), e);
                        // Mark all voters in this batch as failed
                        bulkUpload.setTotalFailedVoters(bulkUpload.getTotalFailedVoters() + voterBatch.size());
                        
                        // Save error details for failed batch
                        saveBatchErrorDetails(voterBatch, bulkUpload, e.getMessage());
                    }
                    voterBatch.clear(); // Critical: Clear batch to free memory
                    voterBenefitSchemesMap.clear(); // Clear benefit schemes tracking
                    
                    // Force garbage collection for large uploads  
                    System.gc();
                    
                    long afterGCMemory = runtime.totalMemory() - runtime.freeMemory();
                    log.info("Post-batch memory after GC: {} MB", afterGCMemory / (1024 * 1024));
                }
            }

            if (!voterBatch.isEmpty()) {
                try {
                    processBatch(voterBatch, bulkUpload, accountId, electionId, partManagerCache, boothCache, boothNumbersSet, boothOrderCounter);
                    // Create benefit scheme join entities after final batch save
                    createBenefitSchemeJoinEntities(voterBatch, voterBenefitSchemesMap, accountId, electionId, benefitSchemeCache);
                } catch (Exception e) {
                    log.error("Failed to process final batch of {} voters for election {} account {}: {}", 
                              voterBatch.size(), electionId, accountId, e.getMessage(), e);
                    // Mark all voters in this batch as failed
                    bulkUpload.setTotalFailedVoters(bulkUpload.getTotalFailedVoters() + voterBatch.size());
                    
                    // Save error details for failed batch
                    saveBatchErrorDetails(voterBatch, bulkUpload, e.getMessage());
                }
            }

            bulkUpload.setTotalRecords(totalRecords);
            bulkUpload.setTotalProcessedVoters(bulkUpload.getTotalSuccessVoters() + bulkUpload.getTotalFailedVoters());
            
            // Enhanced status determination with verification
            BulkUploadStatus finalStatus;
            if (bulkUpload.getTotalSuccessVoters() == 0) {
                finalStatus = BulkUploadStatus.FAILED;
                log.error("Bulk upload {} marked as FAILED - No voters successfully persisted. Total records: {}, Failed: {}", 
                          bulkUpload.getId(), totalRecords, bulkUpload.getTotalFailedVoters());
            } else if (bulkUpload.getTotalFailedVoters() > 0) {
                finalStatus = BulkUploadStatus.COMPLETED; // Partial success
                log.warn("Bulk upload {} marked as COMPLETED with partial success - Success: {}, Failed: {}, Total: {}", 
                         bulkUpload.getId(), bulkUpload.getTotalSuccessVoters(), bulkUpload.getTotalFailedVoters(), totalRecords);
            } else {
                finalStatus = BulkUploadStatus.COMPLETED;
                log.info("Bulk upload {} marked as COMPLETED - All {} voters successfully processed", 
                         bulkUpload.getId(), bulkUpload.getTotalSuccessVoters());
            }
            
            bulkUpload.setStatus(finalStatus);
            bulkUpload.setEndTime(LocalDateTime.now());
            bulkUploadRepo.save(bulkUpload);

            // Trigger batch duplicate run only for the voters inserted/updated in this upload
            try {
                List<Long> voterIdsInThisUpload = fetchVoterIdsForEpicSet(accountId, electionId, uploadEpicNumbers);
                if (!voterIdsInThisUpload.isEmpty()) {
                    triggerBatchDuplicateRun(accountId, electionId, bulkUpload.getId(), voterIdsInThisUpload);
                }
            } catch (Exception ex) {
                log.warn("Failed to trigger batch duplicate run for bulkUpload {}: {}", bulkUpload.getId(), ex.getMessage());
            }
            
            // Enhanced debugging: Check final voter count for this election after processing
            String postCountSql = "SELECT COUNT(*) FROM _voters WHERE election_id = ? AND account_id = ?";
            Integer postCount = jdbcTemplate.queryForObject(postCountSql, Integer.class, electionId, accountId);
            log.info("POST-PROCESSING: Election {} Account {} now has {} voters (was {})", 
                     electionId, accountId, postCount != null ? postCount : 0, preCount != null ? preCount : 0);
            
            Integer actualInserted = (postCount != null ? postCount : 0) - (preCount != null ? preCount : 0);
            if (actualInserted != bulkUpload.getTotalSuccessVoters()) {
                log.error("MISMATCH DETECTED: Expected {} successful inserts but actual database difference is {}", 
                          bulkUpload.getTotalSuccessVoters(), actualInserted);
            }

        } finally {
            // Final memory report for large file uploads
            long finalUsedMemory = runtime.totalMemory() - runtime.freeMemory();
            long memoryIncrease = finalUsedMemory - initialUsedMemory;
            log.info("MEMORY REPORT - Final Used: {} MB, Memory Increase: {} MB, Total Records: {}", 
                     finalUsedMemory / (1024 * 1024), memoryIncrease / (1024 * 1024), totalRecords);
            log.info("Processing completed in {} ms", System.currentTimeMillis() - startTime);
        }
    }

    @Transactional
private void processBatch(List<VoterEntity> voterBatch,
                          BulkUploadEntity bulkUpload,
                          Long accountId,
                          Long electionId,
                          Map<String, PartManager> partManagerCache,
                          Map<Integer, ElectionBooth> boothCache,
                          Set<Integer> boothNumbersSet,
                          AtomicInteger boothOrderCounter) {
    long batchStart = System.currentTimeMillis();
    int successCount = 0;
    int failedCount = 0;
    List<ElectionBooth> newBooths = new ArrayList<>();

    // ─── (Optional) fetch existing IDs if you still want that logging ─────────
    long fetchStart = System.currentTimeMillis();
    List<String> epicNumbers = voterBatch.stream()
        .map(VoterEntity::getEpicNumber)
        .collect(Collectors.toList());
    List<Object[]> existing = voterRepository
        .findIdsByEpicNumbersAndElectionIdAndAccountId(epicNumbers, electionId, accountId);
    Map<String, Long> epicToId = existing.stream().collect(Collectors.toMap(
        row -> (String) row[0],
        row -> (Long)   row[1]
    ));
    log.info("Fetch existing voters time for {} epic_numbers: {} ms",
             epicNumbers.size(), System.currentTimeMillis() - fetchStart);

//    // ─── Build JDBC batch upsert ────────────────────────────────────────────────
//    final String UPSERT_SQL =
//      "INSERT INTO _voters (" +
//      "  epic_number, election_id, account_id, voter_id," +
//      "  part_no, booth_number, gender, voter_fname_en," +
//      "  voter_lname_en, age, mobile_no, created_time," +
//      "  has_voted, family_count" +
//      ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
//      "ON CONFLICT (epic_number, election_id) DO UPDATE SET " +
//      "  part_no        = EXCLUDED.part_no,        " +
//      "  booth_number   = EXCLUDED.booth_number,   " +
//      "  gender         = EXCLUDED.gender,         " +
//      "  voter_fname_en = EXCLUDED.voter_fname_en, " +
//      "  voter_lname_en = EXCLUDED.voter_lname_en, " +
//      "  age            = EXCLUDED.age,            " +
//      "  mobile_no      = EXCLUDED.mobile_no";
//
//    List<Object[]> params = new ArrayList<>(voterBatch.size());
//    for (VoterEntity v : voterBatch) {
//        params.add(new Object[]{
//            v.getEpicNumber(),
//            electionId,
//            accountId,
//            /* voter_id */         v.getEpicNumber(),
//            /* part_no */          v.getPartNo(),
//            /* booth_number */     v.getBoothNumber(),
//            /* gender */           v.getGender(),
//            /* voter_fname_en */   v.getVoterFnameEn(),
//            /* voter_lname_en */   v.getVoterLnameEn(),
//            /* age */              v.getAge(),
//            /* mobile_no */        v.getMobileNo(),
//            /* created_time */     v.getCreatedTime(),
//            /* has_voted */        v.getHasVoted(),
//            /* family_count */     v.getFamilyCount()
//        });
//    }
        final String UPSERT_SQL =
            "INSERT INTO _voters (" +
            "epic_number, voter_id, election_id, account_id, part_no, booth_number, gender, voter_fname_en, " +
            "voter_lname_en, age, mobile_no, created_time, has_voted, family_count, serial_no, house_no_en, " +
            "house_no_l1, house_no_l2, voter_fname_l1, voter_fname_l2, voter_lname_l1, voter_lname_l2, " +
            "rln_type, rln_fname_en, rln_lname_en, rln_fname_l1, rln_lname_l1, rln_fname_l2, rln_lname_l2, " +
            "section_no, section_name_en, section_name_l1, full_address, dob, whatsapp_no, e_mail, voter_lati, voter_longi, state_code, " +
            "state_name_en, state_name_l1, state_name_l2, district_code, district_name_en, district_name_l1, " +
            "district_name_l2, pc_no, pc_name_en, pc_name_l1, pc_name_l2, ac_no, ac_name_en, ac_name_l1, " +
            "ac_name_l2, urban_no, urban_name_en, urban_name_l1, urban_ward_no, rur_district_union_no, " +
            "rur_district_union_name_en, rur_district_union_name_l1, rur_district_union_name_l2, " +
            "rur_district_union_ward_no, pan_union_no, pan_union_name_en, pan_union_name_l1, pan_union_name_l2, " +
            "pan_union_ward_no, vill_pan_no, vill_pan_name_en, vill_pan_name_l1, vill_pan_ward_no, " +
            "pincode, part_name_en, part_name_l1, part_name_l2, section_name_l2, star_number, page_number," +
            "part_lati, part_long, pan_number, aadhaar_number, remarks, part_manager_id, " +
            "religion_id, caste_id, sub_caste_id, caste_category_id, party_id, availability_id, " +
            "photo_url, aadhaar_verified, friend_count, family_id, friend_id, " +
            "family_sequence_number, family_display_part, is_family_head, friends_details, " +
            "family_slip_print_count, party_registration_number" +
            ") VALUES (" +
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 1-10: epic_number through mobile_no
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 11-20: created_time through voter_fname_l2
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 21-30: voter_lname_l1 through section_name_en
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 31-40: section_name_l1 through state_name_l1
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 41-50: state_name_l2 through ac_name_l1
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 51-60: ac_name_l2 through rur_district_union_name_l1
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 61-70: rur_district_union_name_l2 through vill_pan_name_l1
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 71-80: vill_pan_ward_no through part_long
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 81-90: pan_number through availability_id
            "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + // 91-100: photo_url through friends_details
            "?, ?" +                              // 101-102: family_slip_print_count, party_registration_number
            ") " +
            "ON CONFLICT (epic_number, election_id) DO UPDATE SET " +
            "voter_id = EXCLUDED.voter_id, part_no = EXCLUDED.part_no, booth_number = EXCLUDED.booth_number, gender = EXCLUDED.gender, " +
            "voter_fname_en = EXCLUDED.voter_fname_en, voter_lname_en = EXCLUDED.voter_lname_en, age = EXCLUDED.age, " +
            "mobile_no = EXCLUDED.mobile_no, serial_no = EXCLUDED.serial_no, house_no_en = EXCLUDED.house_no_en, " +
            "house_no_l1 = EXCLUDED.house_no_l1, house_no_l2 = EXCLUDED.house_no_l2, voter_fname_l1 = EXCLUDED.voter_fname_l1, " +
            "voter_fname_l2 = EXCLUDED.voter_fname_l2, voter_lname_l1 = EXCLUDED.voter_lname_l1, voter_lname_l2 = EXCLUDED.voter_lname_l2, " +
            "rln_type = EXCLUDED.rln_type, rln_fname_en = EXCLUDED.rln_fname_en, rln_lname_en = EXCLUDED.rln_lname_en, " +
            "rln_fname_l1 = EXCLUDED.rln_fname_l1, rln_lname_l1 = EXCLUDED.rln_lname_l1, rln_fname_l2 = EXCLUDED.rln_fname_l2, " +
            "rln_lname_l2 = EXCLUDED.rln_lname_l2, section_no = EXCLUDED.section_no, section_name_en = EXCLUDED.section_name_en, section_name_l1 = EXCLUDED.section_name_l1, full_address = EXCLUDED.full_address, " +
            "dob = EXCLUDED.dob, whatsapp_no = EXCLUDED.whatsapp_no, e_mail = EXCLUDED.e_mail, voter_lati = EXCLUDED.voter_lati, " +
            "voter_longi = EXCLUDED.voter_longi, state_code = EXCLUDED.state_code, state_name_en = EXCLUDED.state_name_en, " +
            "state_name_l1 = EXCLUDED.state_name_l1, state_name_l2 = EXCLUDED.state_name_l2, district_code = EXCLUDED.district_code, " +
            "district_name_en = EXCLUDED.district_name_en, district_name_l1 = EXCLUDED.district_name_l1, district_name_l2 = EXCLUDED.district_name_l2, " +
            "pc_no = EXCLUDED.pc_no, pc_name_en = EXCLUDED.pc_name_en, pc_name_l1 = EXCLUDED.pc_name_l1, pc_name_l2 = EXCLUDED.pc_name_l2, " +
            "ac_no = EXCLUDED.ac_no, ac_name_en = EXCLUDED.ac_name_en, ac_name_l1 = EXCLUDED.ac_name_l1, ac_name_l2 = EXCLUDED.ac_name_l2, " +
            "urban_no = EXCLUDED.urban_no, urban_name_en = EXCLUDED.urban_name_en, urban_name_l1 = EXCLUDED.urban_name_l1, " +
            "urban_ward_no = EXCLUDED.urban_ward_no, rur_district_union_no = EXCLUDED.rur_district_union_no, " +
            "rur_district_union_name_en = EXCLUDED.rur_district_union_name_en, rur_district_union_name_l1 = EXCLUDED.rur_district_union_name_l1, " +
            "rur_district_union_name_l2 = EXCLUDED.rur_district_union_name_l2, rur_district_union_ward_no = EXCLUDED.rur_district_union_ward_no, " +
            "pan_union_no = EXCLUDED.pan_union_no, pan_union_name_en = EXCLUDED.pan_union_name_en, pan_union_name_l1 = EXCLUDED.pan_union_name_l1, " +
            "pan_union_name_l2 = EXCLUDED.pan_union_name_l2, pan_union_ward_no = EXCLUDED.pan_union_ward_no, " +
            "vill_pan_no = EXCLUDED.vill_pan_no, vill_pan_name_en = EXCLUDED.vill_pan_name_en, vill_pan_name_l1 = EXCLUDED.vill_pan_name_l1, " +
            "vill_pan_ward_no = EXCLUDED.vill_pan_ward_no, pincode = EXCLUDED.pincode, part_name_en = EXCLUDED.part_name_en, part_name_l1 = EXCLUDED.part_name_l1, part_name_l2 = EXCLUDED.part_name_l2, section_name_l2 = EXCLUDED.section_name_l2, " +
            "star_number = EXCLUDED.star_number, page_number = EXCLUDED.page_number,"+
            "part_lati = EXCLUDED.part_lati, part_long = EXCLUDED.part_long, pan_number = EXCLUDED.pan_number, aadhaar_number = EXCLUDED.aadhaar_number, remarks = EXCLUDED.remarks, part_manager_id = EXCLUDED.part_manager_id, " +
            "religion_id = EXCLUDED.religion_id, caste_id = EXCLUDED.caste_id, sub_caste_id = EXCLUDED.sub_caste_id, " +
            "caste_category_id = EXCLUDED.caste_category_id, party_id = EXCLUDED.party_id, availability_id = EXCLUDED.availability_id, " +
            "photo_url = EXCLUDED.photo_url, aadhaar_verified = EXCLUDED.aadhaar_verified, friend_count = EXCLUDED.friend_count, " +
            "family_id = EXCLUDED.family_id, friend_id = EXCLUDED.friend_id, family_count = EXCLUDED.family_count, " +
            "family_sequence_number = EXCLUDED.family_sequence_number, family_display_part = EXCLUDED.family_display_part, " +
            "is_family_head = EXCLUDED.is_family_head, friends_details = EXCLUDED.friends_details, " +
            "family_slip_print_count = EXCLUDED.family_slip_print_count, party_registration_number = EXCLUDED.party_registration_number";

        List<Object[]> params = new ArrayList<>(voterBatch.size());
        ObjectMapper jsonMapper = new ObjectMapper();
        
        for (VoterEntity v : voterBatch) {
            // Convert friends_details List to JSON string for database
            String friendsDetailsJson = null;
            if (v.getFriendsDetails() != null && !v.getFriendsDetails().isEmpty()) {
                try {
                    friendsDetailsJson = jsonMapper.writeValueAsString(v.getFriendsDetails());
                } catch (Exception e) {
                    log.warn("Failed to serialize friends_details for voter {}: {}", v.getEpicNumber(), e.getMessage());
                }
            }
            
            params.add(new Object[]{
                v.getEpicNumber(), v.getVoterId(), electionId, accountId, v.getPartNo(), v.getBoothNumber(),
                v.getGender(), v.getVoterFnameEn(), v.getVoterLnameEn(), v.getAge(), v.getMobileNo(),
                v.getCreatedTime(), v.getHasVoted(), v.getFamilyCount(), v.getSerialNo(), v.getHouseNoEn(),
                v.getHouseNoL1(), v.getHouseNoL2(), v.getVoterFnameL1(), v.getVoterFnameL2(), v.getVoterLnameL1(),
                v.getVoterLnameL2(), v.getRlnType(), v.getRlnFnameEn(), v.getRlnLnameEn(), v.getRlnFnameL1(),
                v.getRlnLnameL1(), v.getRlnFnameL2(), v.getRlnLnameL2(), v.getSectionNo(), v.getSectionNameEn(), v.getSectionNameL1(), v.getFullAddress(),
                v.getDob(), v.getWhatsappNo(), v.getEMail(), v.getVoterLati(), v.getVoterLongi(), v.getStateCode(),
                v.getStateNameEn(), v.getStateNameL1(), v.getStateNameL2(), v.getDistrictCode(), v.getDistrictNameEn(),
                v.getDistrictNameL1(), v.getDistrictNameL2(), v.getPcNo(), v.getPcNameEn(), v.getPcNameL1(),
                v.getPcNameL2(), v.getAcNo(), v.getAcNameEn(), v.getAcNameL1(), v.getAcNameL2(), v.getUrbanNo(),
                v.getUrbanNameEn(), v.getUrbanNameL1(), v.getUrbanWardNo(), v.getRurDistrictUnionNo(),
                v.getRurDistrictUnionNameEn(), v.getRurDistrictUnionNameL1(), v.getRurDistrictUnionNameL2(),
                v.getRurDistrictUnionWardNo(), v.getPanUnionNo(), v.getPanUnionNameEn(), v.getPanUnionNameL1(),
                v.getPanUnionNameL2(), v.getPanUnionWardNo(), v.getVillPanNo(), v.getVillPanNameEn(),
                v.getVillPanNameL1(), v.getVillPanWardNo(), v.getPincode(), v.getPartNameEn(), v.getPartNameL1(), v.getPartNameL2(), v.getSectionNameL2(),
                v.getStarNumber(), v.getPageNumber(), v.getPartLati(), v.getPartLong(), v.getPanNumber(), v.getAadhaarNumber(), v.getRemarks(),
                v.getPartManager() != null ? v.getPartManager().getId() : null,
                // Relationship foreign keys
                v.getReligion() != null ? v.getReligion().getId() : null,
                v.getCaste() != null ? v.getCaste().getId() : null,
                v.getSubCaste() != null ? v.getSubCaste().getId() : null,
                v.getCasteCategory() != null ? v.getCasteCategory().getId() : null,
                v.getParty() != null ? v.getParty().getId() : null,
                v.getAvailability1() != null ? v.getAvailability1().getId() : null,
                // Additional fields
                v.getPhotoUrl(),
                v.getAadhaarVerified(),
                v.getFriendCount(),
                // Family and friend fields
                v.getFamilyId(),
                v.getFriendId(),
                v.getFamilySequenceNumber(),
                v.getFamilyDisplayPart(),
                v.getIsFamilyHead(),
                friendsDetailsJson,  // Use the serialized JSON string
                v.getFamilySlipPrintCount(),
                v.getPartyRegistrationNumber()
            });
        }
           
    // ─── Execute batch ─────────────────────────────────────────────────────────
    long upsertStart = System.currentTimeMillis();
    try {
        log.info("Executing batch upsert for {} voters with election_id={}, account_id={}", 
                 voterBatch.size(), electionId, accountId);
        
        // Log a sample of the data being inserted for debugging
        if (!voterBatch.isEmpty()) {
            VoterEntity sample = voterBatch.get(0);
            log.info("Sample voter data - epic: {}, election_id: {}, account_id: {}, part_no: {}", 
                     sample.getEpicNumber(), sample.getElectionId(), sample.getAccountId(), sample.getPartNo());
        }
        
        int[] results = jdbcTemplate.batchUpdate(UPSERT_SQL, params);
        
        // Enhanced result processing with detailed analysis
        int actualInserts = 0;
        int actualUpdates = 0;
        int noOperations = 0;
        int unknownResults = 0;
        
        for (int r : results) {
            if (r == 1) actualInserts++;        // New insert successful
            else if (r == 2) actualUpdates++;   // Existing record updated
            else if (r == 0) noOperations++;    // No operation (potential constraint issue)
            else unknownResults++;              // Any other result code
        }
        
        // Log detailed batch results
        log.info("Batch results - Inserts: {}, Updates: {}, No-ops: {}, Unknown: {}", 
                 actualInserts, actualUpdates, noOperations, unknownResults);
        
        // Verify actual database state after batch
        int verifiedCount = verifyBatchResults(voterBatch, electionId, accountId);
        
        // Update counts based on verification
        successCount = verifiedCount;
        failedCount = voterBatch.size() - verifiedCount;
        
        if (failedCount > 0) {
            log.error("Batch verification failed: Expected {} voters, verified {} in database. {} voters failed to persist.", 
                      voterBatch.size(), verifiedCount, failedCount);
        }
        
        log.info("Upsert time for batch of {} voters: {} ms, Verified success: {}, Failed: {}",
                 voterBatch.size(), System.currentTimeMillis() - upsertStart, successCount, failedCount);
                 
    } catch (DataIntegrityViolationException e) {
        log.error("Constraint violation during batch insert for election {} account {}: {}", 
                  electionId, accountId, e.getMessage());
        failedCount = voterBatch.size();
        successCount = 0;
        throw e;
    } catch (Exception e) {
        log.error("Unexpected error during batch insert for election {} account {}: {}", 
                  electionId, accountId, e.getMessage(), e);
        failedCount = voterBatch.size();
        successCount = 0;
        throw e;
    }

    // ─── Now do your booth‐creation, exactly as before ────────────────────────
    long boothProcessStart = System.currentTimeMillis();
    batchProcessVoterData(voterBatch, accountId, electionId, boothCache, newBooths, boothNumbersSet, boothOrderCounter);
    log.info("Batch process time for {} voters: {} ms",
             voterBatch.size(), System.currentTimeMillis() - boothProcessStart);

    // ─── Save any new booths in parallel ──────────────────────────────────────
    long boothSaveStart = System.currentTimeMillis();
    if (!newBooths.isEmpty()) {
        CompletableFuture.runAsync(() -> {
            try {
                batchSaveBooths(newBooths, electionId, accountId);
            } catch (Exception e) {
                log.error("Failed to save {} booths: {}", newBooths.size(), e.getMessage());
            }
        }, executorService).join();
    }
    log.info("Save time for {} booths: {} ms",
             newBooths.size(), System.currentTimeMillis() - boothSaveStart);

    // ─── Update bulk‐upload stats ─────────────────────────────────────────────
    bulkUpload.setTotalProcessedVoters(
        bulkUpload.getTotalProcessedVoters() + voterBatch.size());
    bulkUpload.setTotalSuccessVoters(
        bulkUpload.getTotalSuccessVoters() + successCount);
    bulkUpload.setTotalFailedVoters(
        bulkUpload.getTotalFailedVoters() + failedCount);
    bulkUpload.setTotalRecords(
        bulkUpload.getTotalRecords() + voterBatch.size());
    bulkUpload.setLastUpdatedTime(LocalDateTime.now());
    bulkUpload.setStatus(BulkUploadStatus.IN_PROGRESS);
    bulkUploadRepo.save(bulkUpload);

    log.info("Batch processed. Total: {}, Success: {}, Failed: {}, Time: {} ms",
             voterBatch.size(), successCount, failedCount, System.currentTimeMillis() - batchStart);
}

    /**
     * Create VoterBenefitScheme join entities after voters are saved
     */
    private void createBenefitSchemeJoinEntities(
            List<VoterEntity> voterBatch,
            Map<String, List<String>> voterBenefitSchemesMap,
            Long accountId,
            Long electionId,
            Map<String, BenefitSchemes> benefitSchemeCache) {
        
        if (voterBenefitSchemesMap.isEmpty()) {
            log.debug("No benefit schemes to process for this batch");
            return; // No benefit schemes to process
        }
        
        log.info("Processing benefit schemes for {} voters", voterBenefitSchemesMap.size());
        
        try {
            // Query voter IDs by epic numbers
            List<String> epicNumbers = new ArrayList<>(voterBenefitSchemesMap.keySet());
            log.debug("Querying voter IDs for {} epic numbers", epicNumbers.size());
            
            String epicList = epicNumbers.stream()
                .map(epic -> "'" + epic.replace("'", "''") + "'")
                .collect(Collectors.joining(","));
            
            String query = "SELECT id, epic_number FROM _voters WHERE epic_number IN (" + epicList + ") " +
                          "AND election_id = ? AND account_id = ?";
            
            Map<String, Long> epicToVoterId = new HashMap<>();
            jdbcTemplate.query(query, rs -> {
                epicToVoterId.put(rs.getString("epic_number"), rs.getLong("id"));
            }, electionId, accountId);
            
            log.info("Found {} voter IDs from database for benefit scheme processing", epicToVoterId.size());
            
            if (epicToVoterId.isEmpty()) {
                log.warn("No voter IDs found in database. Voters might not be committed yet.");
                return;
            }
            
            // Create VoterBenefitScheme join entities
            List<VoterBenefitScheme> joinEntities = new ArrayList<>();
            int schemeNotFoundCount = 0;
            
            for (Map.Entry<String, List<String>> entry : voterBenefitSchemesMap.entrySet()) {
                String epicNumber = entry.getKey();
                Long voterId = epicToVoterId.get(epicNumber);
                
                if (voterId == null) {
                    log.warn("Voter ID not found for epic {}, skipping benefit schemes", epicNumber);
                    continue;
                }
                
                // Find voter entity for join
                VoterEntity voter = voterRepository.findById(voterId).orElse(null);
                if (voter == null) {
                    log.warn("Voter entity not found for ID {}, skipping benefit schemes", voterId);
                    continue;
                }
                
                for (String schemeName : entry.getValue()) {
                    String key = normalize(schemeName);
                    BenefitSchemes scheme = benefitSchemeCache.get(key);
                    
                    if (scheme != null) {
                        VoterBenefitScheme joinEntity = new VoterBenefitScheme();
                        joinEntity.setVoter(voter);
                        joinEntity.setBenefitScheme(scheme);
                        joinEntity.setSelected(true);
                        joinEntities.add(joinEntity);
                    } else {
                        schemeNotFoundCount++;
                        log.warn("Benefit scheme '{}' (normalized: '{}') not found in cache for voter {}", 
                            schemeName, key, epicNumber);
                    }
                }
            }
            
            if (schemeNotFoundCount > 0) {
                log.warn("Total benefit schemes not found in cache: {}", schemeNotFoundCount);
            }
            
            // Batch save join entities
            if (!joinEntities.isEmpty()) {
                voterBenefitSchemeRepository.saveAll(joinEntities);
                log.info("✓ Successfully created {} benefit scheme associations for {} voters", 
                    joinEntities.size(), voterBenefitSchemesMap.size());
            } else {
                log.warn("No benefit scheme join entities were created. Check if schemes exist in cache.");
            }
            
        } catch (Exception e) {
            log.error("Error creating benefit scheme join entities: {}", e.getMessage(), e);
        }
    }

    // Collect IDs for this upload by looking up the epic_numbers from the most recent batch list
    private List<Long> fetchVoterIdsForEpicSet(Long accountId, Long electionId, Set<String> epicNumbersSet) {
        if (epicNumbersSet == null || epicNumbersSet.isEmpty()) return List.of();
        List<String> epicNumbers = new ArrayList<>(epicNumbersSet);
        int chunkSize = 1000;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < epicNumbers.size(); i += chunkSize) {
            int to = Math.min(i + chunkSize, epicNumbers.size());
            List<String> chunk = epicNumbers.subList(i, to);
            try {
                List<Object[]> rows = voterRepository.findIdsByEpicNumbersAndElectionIdAndAccountId(chunk, electionId, accountId);
                for (Object[] r : rows) {
                    ids.add((Long) r[1]);
                }
            } catch (Exception e) {
                log.debug("fetchVoterIdsForEpicSet chunk failed: {}", e.getMessage());
            }
        }
        return ids;
    }

    @Autowired(required = false)
    private com.thedal.thedal_app.voter.duplicate.DuplicateRunService duplicateRunService;

    private void triggerBatchDuplicateRun(Long accountId, Long electionId, Long bulkUploadId, List<Long> voterIds) {
        if (duplicateRunService == null) return; // service not available in some contexts/tests
        // Run in another thread to avoid blocking upload response
        executorService.submit(() -> {
            try {
                duplicateRunService.startBatchRun(accountId, electionId, bulkUploadId, voterIds, null);
            } catch (Exception ex) {
                log.warn("Batch duplicate run failed for upload {}: {}", bulkUploadId, ex.getMessage());
            }
        });
    }

    private void batchProcessVoterData(List<VoterEntity> savedVoters, Long accountId, Long electionId,
                                      Map<Integer, ElectionBooth> boothCache, List<ElectionBooth> newBooths, Set<Integer> boothNumbersSet,
                                      AtomicInteger boothOrderCounter) {
        for (VoterEntity voter : savedVoters) {
            Integer boothNumber = voter.getBoothNumber();
            if (boothNumber != null) {
                boothNumbersSet.add(boothNumber);
                if (!boothCache.containsKey(boothNumber)) {
                    ElectionBooth newBooth = new ElectionBooth();
                    newBooth.setElection(new ElectionEntity(electionId));
                    newBooth.setBoothNumber(boothNumber);
                    newBooth.setAccountId(accountId);
                    newBooth.setOrderIndex(boothOrderCounter.getAndIncrement());
                    newBooths.add(newBooth);
                    boothCache.put(boothNumber, newBooth);
                    log.info("Created new ElectionBooth for boothNumber: {}", boothNumber);
                }
            }
        }
    }

    private void batchSaveBooths(List<ElectionBooth> newBooths, Long electionId, Long accountId) {
        String sql = "INSERT INTO election_booth (election_id, account_id, booth_number, order_index) " +
                "VALUES (?, ?, ?, ?)";
        List<Object[]> batchArgs = newBooths.stream()
                .map(b -> new Object[]{
                        electionId, accountId, b.getBoothNumber(), b.getOrderIndex()
                })
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    private String getCellValue(Row row, int cellIndex) {
        Cell cell = row.getCell(cellIndex);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new java.text.SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                CellValue cellValue = evaluator.evaluate(cell);
                switch (cellValue.getCellType()) {
                    case STRING:
                        return cellValue.getStringValue();
                    case NUMERIC:
                        return String.valueOf((long) cellValue.getNumberValue());
                    case BOOLEAN:
                        return String.valueOf(cellValue.getBooleanValue());
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    private VoterEntity mapToVoterEntityDynamic(Row row, Map<String, Integer> headerMapping, Long accountId,
    		 Long electionId, Map<String, PartManager> partManagerCache) {
        VoterEntity voter = new VoterEntity();
        voter.setAccountId(accountId);
        voter.setElectionId(electionId);
        voter.setFamilyCount(1);
        voter.setCreatedTime(LocalDateTime.now());
        voter.setHasVoted(false);
        voter.setRemarks(null);

        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            Integer index = entry.getValue();
            String value = getCellValue(row, index);
            if (value == null) continue;

            try {
                switch (header.toLowerCase()) { // Use toLowerCase for case-insensitive matching              
                case "part_no":
                    Integer partNoValue = Integer.parseInt(value);
                    voter.setPartNo(partNoValue);
                    voter.setBoothNumber(partNoValue);
                    // Fetch and set PartManager from cache
                    PartManager partManager = partManagerCache.get(String.valueOf(partNoValue));
                    if (partManager != null) {
                        voter.setPartManager(partManager);
                    } else {
                        log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Skipping assignment.",
                                partNoValue, accountId, electionId);
                    }
                    break;                
                    case "epic_number":
                        voter.setEpicNumber(value);
                        break;                  
                    case "gender":
                        voter.setGender(value.toLowerCase());
                        break;
//                    case "part_no":
//                        Integer partNoValue = Integer.parseInt(value);
//                        voter.setPartNo(partNoValue);
//                        voter.setBoothNumber(partNoValue);
//                        break;
                    case "serial_no":
                        voter.setSerialNo(Long.parseLong(value));
                        break;
                    case "house_no_en":
                        voter.setHouseNoEn(value);
                        break;
                    case "voter_fname_en":
                        voter.setVoterFnameEn(value);
                        break;
                    case "rln_type":
                        voter.setRlnType(value);
                        break;
                    case "section_no":
                        voter.setSectionNo(Integer.parseInt(value));
                        break;
                    case "house_no_l1":
                        voter.setHouseNoL1(value);
                        break;
                    case "voter_lname_en":
                        voter.setVoterLnameEn(value);
                        break;
                    case "voter_fname_l1":
                        voter.setVoterFnameL1(value);
                        break;
                    case "rln_fname_en":
                        voter.setRlnFnameEn(value);
                        break;
                    case "rln_lname_en":
                        voter.setRlnLnameEn(value);
                        break;
                    case "rln_fname_l1":
                        voter.setRlnFnameL1(value);
                        break;
                    case "rln_lname_l1":
                        voter.setRlnLnameL1(value);
                        break;
                    case "full_address":
                        voter.setFullAddress(value);
                        break;
                    case "age":
                        voter.setAge(Integer.parseInt(value));
                        break;
                    case "dob":
                        voter.setDob(java.time.LocalDate.parse(value));
                        break;
                    case "mobile_no":
                        voter.setMobileNo(value);
                        break;
                    case "whatsapp_no":
                        voter.setWhatsappNo(value);
                        break;
                    case "e_mail":
                        voter.setEMail(value);
                        break;
                    case "voter_lati":
                        voter.setVoterLati(Double.parseDouble(value));
                        break;
                    case "voter_longi":
                        voter.setVoterLongi(Double.parseDouble(value));
                        break;
                    case "state_code":
                        voter.setStateCode(value);
                        break;
                    case "state_name_en":
                        voter.setStateNameEn(value);
                        break;
                    case "state_name_l1":
                        voter.setStateNameL1(value);
                        break;
                    case "district_code":
                        voter.setDistrictCode(value);
                        break;
                    case "district_name_en":
                        voter.setDistrictNameEn(value);
                        break;
                    case "district_name_l1":
                        voter.setDistrictNameL1(value);
                        break;
                    case "pc_no":
                        voter.setPcNo(value);
                        break;
                    case "pc_name_en":
                        voter.setPcNameEn(value);
                        break;
                    case "pc_name_l1":
                        voter.setPcNameL1(value);
                        break;
                    case "ac_no":
                        voter.setAcNo(value);
                        break;
                    case "ac_name_en":
                        voter.setAcNameEn(value);
                        break;
                    case "ac_name_l1":
                        voter.setAcNameL1(value);
                        break;
                    case "urban_no":
                        voter.setUrbanNo(value);
                        break;
                    case "urban_name_en":
                        voter.setUrbanNameEn(value);
                        break;
                    case "urban_name_l1":
                        voter.setUrbanNameL1(value);
                        break;
                    case "urban_ward_no":
                        voter.setUrbanWardNo(Integer.parseInt(value));
                        break;
                    case "rur_district_union_no":
                        voter.setRurDistrictUnionNo(value);
                        break;
                    case "rur_district_union_name_en":
                        voter.setRurDistrictUnionNameEn(value);
                        break;
                    case "rur_district_union_name_l1":
                        voter.setRurDistrictUnionNameL1(value);
                        break;
                    case "rur_district_union_ward_no":
                        voter.setRurDistrictUnionWardNo(value);
                        break;
                    case "pan_union_no":
                        voter.setPanUnionNo(value);
                        break;
                    case "pan_union_name_en":
                        voter.setPanUnionNameEn(value);
                        break;
                    case "pan_union_name_l1":
                        voter.setPanUnionNameL1(value);
                        break;
                    case "pan_union_ward_no":
                        voter.setPanUnionWardNo(value);
                        break;
                    case "vill_pan_no":
                        voter.setVillPanNo(value);
                        break;
                    case "vill_pan_name_en":
                        voter.setVillPanNameEn(value);
                        break;
                    case "vill_pan_name_l1":
                        voter.setVillPanNameL1(value);
                        break;
                    case "vill_pan_ward_no":
                        voter.setVillPanWardNo(value);
                        break;
                    case "house_no_l2":
                        voter.setHouseNoL2(value);
                        break;
                    case "voter_fname_l2":
                        voter.setVoterFnameL2(value);
                        break;
                    case "voter_lname_l1":
                        voter.setVoterLnameL1(value);
                        break;
                    case "voter_lname_l2":
                        voter.setVoterLnameL2(value);
                        break;
                    case "rln_fname_l2":
                        voter.setRlnFnameL2(value);
                        break;
                    case "rln_lname_l2":
                        voter.setRlnLnameL2(value);
                        break;
                    case "part_name_l2":
                        voter.setPartNameL2(value);
                        break;
                    case "state_name_l2":
                        voter.setStateNameL2(value);
                        break;
                    case "district_name_l2":
                        voter.setDistrictNameL2(value);
                        break;
                    case "pc_name_l2":
                        voter.setPcNameL2(value);
                        break;
                    case "ac_name_l2":
                        voter.setAcNameL2(value);
                        break;
                    case "rur_district_union_name_l2":
                        voter.setRurDistrictUnionNameL2(value);
                        break;
                    case "pan_union_name_l2":
                        voter.setPanUnionNameL2(value);
                        break;
                    case "section_name_l2":
                        voter.setSectionNameL2(value);
                        break;
                    case "star_number":
                        voter.setStarNumber(Boolean.parseBoolean(value));
                        break;
                    case "page_number":
                        voter.setPageNumber(Integer.parseInt(value));
                        break;
                    case "section_name_en":
                        voter.setSectionNameEn(value);
                        break;
                    case "section_name_l1":
                        voter.setSectionNameL1(value);
                        break;
                    // Add missing fields
                    case "part_name_en":
                        voter.setPartNameEn(value);
                        break;
                    case "part_name_l1":
                        voter.setPartNameL1(value);
                        break;
                    case "pincode":
                        voter.setPincode(value);
                        break;
                    case "part_lati":
                        voter.setPartLati(value != null ? Double.parseDouble(value) : null);
                        break;
                    case "part_long":
                        voter.setPartLong(value != null ? Double.parseDouble(value) : null);
                        break;
                    case "pan_number":
                        voter.setPanNumber(value);
                        break;
                    case "aadhaar_number":
                        voter.setAadhaarNumber(value);
                        break;
                    case "remarks":
                        voter.setRemarks(value);
                        break;
                    // *** NEW: Dynamic field support - all optional ***
                    case "family_id":
                        // PRESERVE existing family_id - only set if voter doesn't have one and file has one
                        if (voter.getFamilyId() == null && value != null && !value.trim().isEmpty()) {
                            try {
                                voter.setFamilyId(java.util.UUID.fromString(value.trim()));
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid family_id format: {}, skipping", value);
                            }
                        }
                        break;
                    case "family_count":
                        if (value != null && !value.trim().isEmpty()) {
                            try {
                                voter.setFamilyCount(Integer.parseInt(value.trim()));
                            } catch (NumberFormatException e) {
                                log.warn("Invalid family_count: {}, using default", value);
                            }
                        }
                        break;
                    // Note: Religion, Caste, Party, etc. will be resolved in processBatch
                    // For now, just log that we found them (they're handled below)
                    case "religion_name":
                    case "caste_name":
                    case "sub_caste_name":
                    case "caste_category_name":
                    case "party_name":
                    case "party_short_name":
                    case "availability_description":
                    case "availability_category_name":
                    case "languages":
                    case "benefit_schemes_name":
                    case "benefit_schemes_by":
                    case "scheme":
                    case "voter_history_names":
                    case "feedback_issue_names":
                    case "aadhaar_verified":
                    case "photo_url":
                    case "friend_count":
                        // These are handled in resolveVoterRelationships method during batch processing
                        // Just log for debugging
                        log.debug("Found dynamic field: {} = {}", header, value);
                        break;
                    case "party_registration_number":
                    case "membership_number":
                        voter.setPartyRegistrationNumber(value);
                        break;
                    default:
                        log.warn("Unknown header: {}", header);
                        break;
                }
            } catch (Exception e) {
                log.warn("Failed to parse value for header {}: {}", header, value);
            }
        }
        voter.setFamilyCount(voter.getFamilyCount() != null ? voter.getFamilyCount() : 1);
        voter.setCreatedTime(voter.getCreatedTime() != null ? voter.getCreatedTime() : LocalDateTime.now());
        log.info("Mapped VoterEntity: familyCount={}, createdTime={}", voter.getFamilyCount(), voter.getCreatedTime());
        return voter;
    }
    
//    private VoterEntity mapToVoterEntityFromCsv(String[] fields, Map<String, Integer> headerMapping, Long accountId) {
//        VoterEntity voter = new VoterEntity();
//        voter.setAccountId(accountId);
//        voter.setFamilyCount(1);
//
//        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
//            String header = entry.getKey();
//            Integer index = entry.getValue();
//            if (index >= fields.length || fields[index].isEmpty()) continue;
//            String value = fields[index].trim();
//
//            try {
//                switch (header) {
//                    case "epic_number":
//                        voter.setEpicNumber(value);
//                        break;
//                    case "gender":
//                        voter.setGender(value.toLowerCase());
//                        break;
//                    case "part_no":
//                        voter.setPartNo(Integer.parseInt(value));
//                        break;
//                    case "serial_no":
//                        voter.setSerialNo(Long.parseLong(value));
//                        break;
//                    case "house_no_en":
//                        voter.setHouseNoEn(value);
//                        break;
//                    case "voter_fname_en":
//                        voter.setVoterFnameEn(value);
//                        break;
//                    case "rln_type":
//                        voter.setRlnType(value);
//                        break;
//                    case "section_no":
//                        voter.setSectionNo(Integer.parseInt(value));
//                        break;
//                    case "house_no_l1":
//                        voter.setHouseNoL1(value);
//                        break;
//                    case "voter_lname_en":
//                        voter.setVoterLnameEn(value);
//                        break;
//                    case "voter_fname_l1":
//                        voter.setVoterFnameL1(value);
//                        break;
//                    case "rln_fname_en":
//                        voter.setRlnFnameEn(value);
//                        break;
//                    case "rln_lname_en":
//                        voter.setRlnLnameEn(value);
//                        break;
//                    case "rln_fname_l1":
//                        voter.setRlnFnameL1(value);
//                        break;
//                    case "rln_lname_l1":
//                        voter.setRlnLnameL1(value);
//                        break;
//                    case "section_name_en":
//                        voter.setSectionNameEn(value);
//                        break;
//                    case "section_name_l1":
//                        voter.setSectionNameL1(value);
//                        break;
//                    case "full_address":
//                        voter.setFullAddress(value);
//                        break;
//                    case "part_name_en":
//                        voter.setPartNameEn(value);
//                        break;
//                    case "part_name_l1":
//                        voter.setPartNameL1(value);
//                        break;
//                    case "pincode":
//                        voter.setPincode(value);
//                        break;
//                    case "part_lati":
//                        voter.setPartLati(Double.parseDouble(value));
//                        break;
//                    case "part_long":
//                        voter.setPartLong(Double.parseDouble(value));
//                        break;
//                    case "age":
//                        voter.setAge(Integer.parseInt(value));
//                        break;
//                    case "dob":
//                        voter.setDob(java.time.LocalDate.parse(value));
//                        break;
//                    case "mobile_no":
//                        voter.setMobileNo(value);
//                        break;
//                    case "whatsapp_no":
//                        voter.setWhatsappNo(value);
//                        break;
//                    case "e_mail":
//                        voter.setEMail(value);
//                        break;
//                    case "voter_lati":
//                        voter.setVoterLati(Double.parseDouble(value));
//                        break;
//                    case "voter_longi":
//                        voter.setVoterLongi(Double.parseDouble(value));
//                        break;
//                    case "state_code":
//                        voter.setStateCode(value);
//                        break;
//                    case "state_name_en":
//                        voter.setStateNameEn(value);
//                        break;
//                    case "state_name_l1":
//                        voter.setStateNameL1(value);
//                        break;
//                    case "district_code":
//                        voter.setDistrictCode(value);
//                        break;
//                    case "district_name_en":
//                        voter.setDistrictNameEn(value);
//                        break;
//                    case "district_name_l1":
//                        voter.setDistrictNameL1(value);
//                        break;
//                    case "pc_no":
//                        voter.setPcNo(value);
//                        break;
//                    case "pc_name_en":
//                        voter.setPcNameEn(value);
//                        break;
//                    case "pc_name_l1":
//                        voter.setPcNameL1(value);
//                        break;
//                    case "ac_no":
//                        voter.setAcNo(value);
//                        break;
//                    case "ac_name_en":
//                        voter.setAcNameEn(value);
//                        break;
//                    case "ac_name_l1":
//                        voter.setAcNameL1(value);
//                        break;
//                    case "urban_no":
//                        voter.setUrbanNo(value);
//                        break;
//                    case "urban_name_en":
//                        voter.setUrbanNameEn(value);
//                        break;
//                    case "urban_name_l1":
//                        voter.setUrbanNameL1(value);
//                        break;
//                    case "urban_ward_no":
//                        voter.setUrbanWardNo(Integer.parseInt(value));
//                        break;
//                    case "rur_district_union_no":
//                        voter.setRurDistrictUnionNo(value);
//                        break;
//                    case "rur_district_union_name_en":
//                        voter.setRurDistrictUnionNameEn(value);
//                        break;
//                    case "rur_district_union_name_l1":
//                        voter.setRurDistrictUnionNameL1(value);
//                        break;
//                    case "rur_district_union_ward_no":
//                        voter.setRurDistrictUnionWardNo(value);
//                        break;
//                    case "pan_union_no":
//                        voter.setPanUnionNo(value);
//                        break;
//                    case "pan_union_name_en":
//                        voter.setPanUnionNameEn(value);
//                        break;
//                    case "pan_union_name_l1":
//                        voter.setPanUnionNameL1(value);
//                        break;
//                    case "pan_union_ward_no":
//                        voter.setPanUnionWardNo(value);
//                        break;
//                    case "vill_pan_no":
//                        voter.setVillPanNo(value);
//                        break;
//                    case "vill_pan_name_en":
//                        voter.setVillPanNameEn(value);
//                        break;
//                    case "vill_pan_name_l1":
//                        voter.setVillPanNameL1(value);
//                        break;
//                    case "vill_pan_ward_no":
//                        voter.setVillPanWardNo(value);
//                        break;
//                    case "booth_number":
//                        voter.setBoothNumber(Integer.parseInt(value));
//                        break;
//                }
//            } catch (Exception e) {
//                log.warn("Failed to parse value for header {}: {}", header, value);
//            }
//        }
//        return voter;
//    }
    private VoterEntity mapToVoterEntityFromCsv(String[] fields, Map<String, Integer> headerMapping, Long accountId,
    		 Long electionId, Map<String, PartManager> partManagerCache) {
        VoterEntity voter = new VoterEntity();
        voter.setAccountId(accountId);
        voter.setElectionId(electionId);
        voter.setFamilyCount(1);
        voter.setCreatedTime(LocalDateTime.now());
        voter.setHasVoted(false);
        voter.setRemarks(null);

        for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
            String header = entry.getKey();
            Integer index = entry.getValue();
            if (index >= fields.length || fields[index].isEmpty()) continue;
            String value = fields[index].trim();

            try {
                switch (header.toLowerCase()) {
                    case "part_no":
                        Integer partNoValue = Integer.parseInt(value);
                        voter.setPartNo(partNoValue);
                        voter.setBoothNumber(partNoValue);
                        // Fetch and set PartManager from cache
                        PartManager partManager = partManagerCache.get(String.valueOf(partNoValue));
                        if (partManager != null) {
                            voter.setPartManager(partManager);
                        } else {
                            log.warn("PartManager not found for partNo: {}, accountId: {}, electionId: {}. Skipping assignment.",
                                    partNoValue, accountId, electionId);
                        }
                        break;
                    case "epic_number":
                        voter.setEpicNumber(value);
                        break;
                    case "gender":
                        voter.setGender(value.toLowerCase());
                        break;
                    case "voter_fname_en":
                        voter.setVoterFnameEn(value);
                        break;
                    case "voter_lname_en":
                        voter.setVoterLnameEn(value);
                        break;
                    case "age":
                        voter.setAge(Integer.parseInt(value));
                        break;
                    case "mobile_no":
                        voter.setMobileNo(value);
                        break;
                    case "serial_no":
                        voter.setSerialNo(Long.parseLong(value));
                        break;
                    case "house_no_en":
                        voter.setHouseNoEn(value);
                        break;
                    case "house_no_l1":
                        voter.setHouseNoL1(value);
                        break;
                    case "house_no_l2":
                        voter.setHouseNoL2(value);
                        break;
                    case "voter_fname_l1":
                        voter.setVoterFnameL1(value);
                        break;
                    case "voter_fname_l2":
                        voter.setVoterFnameL2(value);
                        break;
                    case "voter_lname_l1":
                        voter.setVoterLnameL1(value);
                        break;
                    case "voter_lname_l2":
                        voter.setVoterLnameL2(value);
                        break;
                    case "rln_type":
                        voter.setRlnType(value);
                        break;
                    case "rln_fname_en":
                        voter.setRlnFnameEn(value);
                        break;
                    case "rln_lname_en":
                        voter.setRlnLnameEn(value);
                        break;
                    case "rln_fname_l1":
                        voter.setRlnFnameL1(value);
                        break;
                    case "rln_lname_l1":
                        voter.setRlnLnameL1(value);
                        break;
                    case "rln_fname_l2":
                        voter.setRlnFnameL2(value);
                        break;
                    case "rln_lname_l2":
                        voter.setRlnLnameL2(value);
                        break;
                    case "section_no":
                        voter.setSectionNo(Integer.parseInt(value));
                        break;
                    case "section_name_en":
                        voter.setSectionNameEn(value);
                        break;
                    case "section_name_l1":
                        voter.setSectionNameL1(value);
                        break;
                    case "section_name_l2":
                        voter.setSectionNameL2(value);
                        break;
                    case "full_address":
                        voter.setFullAddress(value);
                        break;
                    case "dob":
                        voter.setDob(java.time.LocalDate.parse(value));
                        break;
                    case "whatsapp_no":
                        voter.setWhatsappNo(value);
                        break;
                    case "e_mail":
                        voter.setEMail(value);
                        break;
                    case "voter_lati":
                        voter.setVoterLati(Double.parseDouble(value));
                        break;
                    case "voter_longi":
                        voter.setVoterLongi(Double.parseDouble(value));
                        break;
                    case "state_code":
                        voter.setStateCode(value);
                        break;
                    case "state_name_en":
                        voter.setStateNameEn(value);
                        break;
                    case "state_name_l1":
                        voter.setStateNameL1(value);
                        break;
                    case "state_name_l2":
                        voter.setStateNameL2(value);
                        break;
                    case "district_code":
                        voter.setDistrictCode(value);
                        break;
                    case "district_name_en":
                        voter.setDistrictNameEn(value);
                        break;
                    case "district_name_l1":
                        voter.setDistrictNameL1(value);
                        break;
                    case "district_name_l2":
                        voter.setDistrictNameL2(value);
                        break;
                    case "pc_no":
                        voter.setPcNo(value);
                        break;
                    case "pc_name_en":
                        voter.setPcNameEn(value);
                        break;
                    case "pc_name_l1":
                        voter.setPcNameL1(value);
                        break;
                    case "pc_name_l2":
                        voter.setPcNameL2(value);
                        break;
                    case "ac_no":
                        voter.setAcNo(value);
                        break;
                    case "ac_name_en":
                        voter.setAcNameEn(value);
                        break;
                    case "ac_name_l1":
                        voter.setAcNameL1(value);
                        break;
                    case "ac_name_l2":
                        voter.setAcNameL2(value);
                        break;
                    case "urban_no":
                        voter.setUrbanNo(value);
                        break;
                    case "urban_name_en":
                        voter.setUrbanNameEn(value);
                        break;
                    case "urban_name_l1":
                        voter.setUrbanNameL1(value);
                        break;
                    case "urban_ward_no":
                        voter.setUrbanWardNo(Integer.parseInt(value));
                        break;
                    case "rur_district_union_no":
                        voter.setRurDistrictUnionNo(value);
                        break;
                    case "rur_district_union_name_en":
                        voter.setRurDistrictUnionNameEn(value);
                        break;
                    case "rur_district_union_name_l1":
                        voter.setRurDistrictUnionNameL1(value);
                        break;
                    case "rur_district_union_name_l2":
                        voter.setRurDistrictUnionNameL2(value);
                        break;
                    case "rur_district_union_ward_no":
                        voter.setRurDistrictUnionWardNo(value);
                        break;
                    case "pan_union_no":
                        voter.setPanUnionNo(value);
                        break;
                    case "pan_union_name_en":
                        voter.setPanUnionNameEn(value);
                        break;
                    case "pan_union_name_l1":
                        voter.setPanUnionNameL1(value);
                        break;
                    case "pan_union_name_l2":
                        voter.setPanUnionNameL2(value);
                        break;
                    case "pan_union_ward_no":
                        voter.setPanUnionWardNo(value);
                        break;
                    case "vill_pan_no":
                        voter.setVillPanNo(value);
                        break;
                    case "vill_pan_name_en":
                        voter.setVillPanNameEn(value);
                        break;
                    case "vill_pan_name_l1":
                        voter.setVillPanNameL1(value);
                        break;
                    case "vill_pan_ward_no":
                        voter.setVillPanWardNo(value);
                        break;
                    case "part_name_en":
                        voter.setPartNameEn(value);
                        break;
                    case "part_name_l1":
                        voter.setPartNameL1(value);
                        break;
                    case "part_name_l2":
                        voter.setPartNameL2(value);
                        break;
                    case "pincode":
                        voter.setPincode(value);
                        break;
                    case "part_lati":
                        voter.setPartLati(value != null ? Double.parseDouble(value) : null);
                        break;
                    case "part_long":
                        voter.setPartLong(value != null ? Double.parseDouble(value) : null);
                        break;
                    case "star_number":
                        voter.setStarNumber(Boolean.parseBoolean(value));
                        break;
                    case "page_number":
                        voter.setPageNumber(Integer.parseInt(value));
                        break;
                    case "pan_number":
                        voter.setPanNumber(value);
                        break;
                    case "aadhaar_number":
                        voter.setAadhaarNumber(value);
                        break;
                    case "remarks":
                        voter.setRemarks(value);
                        break;
                    case "family_id":
                        // Only set if voter doesn't have one (protect existing family mappings)
                        if (voter.getFamilyId() == null && !value.isEmpty()) {
                            try {
                                voter.setFamilyId(java.util.UUID.fromString(value));
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid family_id format: {}", value);
                            }
                        }
                        break;
                    case "family_count":
                        if (!value.isEmpty()) {
                            try {
                                voter.setFamilyCount(Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                log.warn("Invalid family_count: {}", value);
                            }
                        }
                        break;
                    case "party_registration_number":
                    case "membership_number":
                        voter.setPartyRegistrationNumber(value);
                        break;
                    case "photo_url":
                        voter.setPhotoUrl(value);
                        break;
                    case "aadhaar_verified":
                        voter.setAadhaarVerified(Boolean.parseBoolean(value));
                        break;
                    case "friend_count":
                        voter.setFriendCount(Integer.parseInt(value));
                        break;
                    // Dynamic fields handled in resolveVoterRelationshipsFromCsv
                    case "religion_name":
                    case "caste_name":
                    case "sub_caste_name":
                    case "caste_category_name":
                    case "party_name":
                    case "party_short_name":
                    case "availability_description":
                    case "availability_category_name":
                    case "languages":
                    case "benefit_schemes_name":
                    case "benefit_schemes_by":
                    case "scheme":
                    case "voter_history_names":
                    case "feedback_issue_names":
                        // These are handled in resolveVoterRelationshipsFromCsv method
                        log.debug("Found dynamic field: {} = {}", header, value);
                        break;
                    default:
                        log.debug("Unmapped CSV header: {}", header);
                        break;
                }
            } catch (Exception e) {
                log.warn("Failed to parse value for header {}: {}", header, value, e);
            }
        }
        
        voter.setFamilyCount(voter.getFamilyCount() != null ? voter.getFamilyCount() : 1);
        voter.setCreatedTime(voter.getCreatedTime() != null ? voter.getCreatedTime() : LocalDateTime.now());
        
        return voter;
    }
    
    /**
     * Verify that the batch of voters was actually persisted to the database
     */
    private int verifyBatchResults(List<VoterEntity> voterBatch, Long electionId, Long accountId) {
        if (voterBatch.isEmpty()) return 0;
        
        try {
            List<String> epicNumbers = voterBatch.stream()
                .map(VoterEntity::getEpicNumber)
                .collect(Collectors.toList());
            
            String verificationSql = "SELECT COUNT(*) FROM _voters WHERE epic_number = ANY(?) AND election_id = ? AND account_id = ?";
            
            // Convert List to Array for PostgreSQL ANY operator
            String[] epicArray = epicNumbers.toArray(new String[0]);
            
            Integer verifiedCount = jdbcTemplate.queryForObject(
                verificationSql, 
                Integer.class, 
                epicArray, 
                electionId, 
                accountId
            );
            
            log.debug("Verified {} out of {} voters persisted in database", verifiedCount, voterBatch.size());
            return verifiedCount != null ? verifiedCount : 0;
            
        } catch (Exception e) {
            log.error("Failed to verify batch results for election {} account {}: {}", 
                      electionId, accountId, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Save error details for failed batch operations
     */
    private void saveBatchErrorDetails(List<VoterEntity> failedVoters, BulkUploadEntity bulkUpload, String errorMessage) {
        try {
            for (VoterEntity voter : failedVoters) {
                BulkUploadErrorEntity errorEntity = new BulkUploadErrorEntity();
                errorEntity.setBulkUploadId(bulkUpload.getId());
                errorEntity.setElectionId(bulkUpload.getElectionId());
                errorEntity.setAccountId(bulkUpload.getAccountId());
                errorEntity.setRowError("Epic: " + voter.getEpicNumber() + " - " + errorMessage);
                errorEntity.setCreatedAt(LocalDateTime.now());
                bulkUploadErrorRepository.save(errorEntity);
            }
            log.info("Saved error details for {} failed voters in bulk upload {}", 
                     failedVoters.size(), bulkUpload.getId());
        } catch (Exception e) {
            log.error("Failed to save error details for bulk upload {}: {}", 
                      bulkUpload.getId(), e.getMessage(), e);
        }
    }

    /**
     * Validate that the required database constraints exist for voter operations
     * Auto-creates the constraint if it doesn't exist
     */
    private void validateVoterConstraints() {
        try {
            String constraintCheckSql = 
                "SELECT COUNT(*) FROM pg_constraint c " +
                "JOIN pg_class t ON t.oid = c.conrelid " +
                "WHERE t.relname = '_voters' " +
                "AND c.contype = 'u' " +
                "AND pg_get_constraintdef(c.oid) LIKE '%epic_number%election_id%'";
            
            Integer constraintCount = jdbcTemplate.queryForObject(constraintCheckSql, Integer.class);
            
            if (constraintCount == null || constraintCount == 0) {
                log.warn("Required unique constraint on (epic_number, election_id) not found. Attempting to create it...");
                
                try {
                    // Attempt to create the constraint
                    String createConstraintSql = 
                        "ALTER TABLE _voters ADD CONSTRAINT uk_voters_epic_election UNIQUE (epic_number, election_id)";
                    jdbcTemplate.execute(createConstraintSql);
                    
                    log.info("Successfully created unique constraint uk_voters_epic_election on _voters table");
                    
                } catch (Exception createError) {
                    String errorMessage = "Failed to create required unique constraint on (epic_number, election_id). " +
                                          "Please manually run: ALTER TABLE _voters ADD CONSTRAINT uk_voters_epic_election UNIQUE (epic_number, election_id); " +
                                          "Error: " + createError.getMessage();
                    log.error(errorMessage);
                    throw new IllegalStateException(errorMessage, createError);
                }
            } else {
                log.debug("Database constraint validation passed - found {} matching constraints", constraintCount);
            }
            
        } catch (IllegalStateException e) {
            throw e; // Re-throw constraint creation errors
        } catch (Exception e) {
            log.error("Database constraint validation failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Database constraint validation failed", e);
        }
    }

    public String analyzeBulkUpload(Long bulkUploadId) {
        try {
            BulkUploadEntity bulkUpload = bulkUploadRepo.findById(bulkUploadId)
                .orElseThrow(() -> new IllegalArgumentException("BulkUpload not found: " + bulkUploadId));
            
            StringBuilder analysis = new StringBuilder();
            analysis.append("Bulk Upload Analysis for ID: ").append(bulkUploadId).append("\n");
            analysis.append("Status: ").append(bulkUpload.getStatus()).append("\n");
            analysis.append("Total Records: ").append(bulkUpload.getTotalRecords()).append("\n");
            analysis.append("Success: ").append(bulkUpload.getTotalSuccessVoters()).append("\n");
            analysis.append("Failed: ").append(bulkUpload.getTotalFailedVoters()).append("\n");
            
            return analysis.toString();
        } catch (Exception e) {
            log.error("Error analyzing bulk upload {}: {}", bulkUploadId, e.getMessage());
            return "Error analyzing bulk upload: " + e.getMessage();
        }
    }

    public String analyzeConstraintConfiguration() {
        try {
            StringBuilder analysis = new StringBuilder();
            analysis.append("Database Constraint Analysis:\n");
            
            String sql = "SELECT conname, consrc FROM pg_constraint WHERE conrelid = '_voters'::regclass";
            List<Map<String, Object>> constraints = jdbcTemplate.queryForList(sql);
            
            analysis.append("Found ").append(constraints.size()).append(" constraints on _voters table:\n");
            for (Map<String, Object> constraint : constraints) {
                analysis.append("- ").append(constraint.get("conname")).append(": ").append(constraint.get("consrc")).append("\n");
            }
            
            return analysis.toString();
        } catch (Exception e) {
            log.error("Error analyzing constraints: {}", e.getMessage());
            return "Error analyzing constraints: " + e.getMessage();
        }
    }
    
    /**
     * Resolve voter relationships from CSV data - creates missing reference data automatically
     * This method is called during batch processing to set up relationships efficiently
     */
    private void resolveVoterRelationships(Row row, Map<String, Integer> headerMapping, 
                                          VoterEntity voter, Long accountId, Long electionId,
                                          Map<String, ReligionEntity> religionCache,
                                          Map<String, CasteEntity> casteCache,
                                          Map<String, SubCasteEntity> subCasteCache,
                                          Map<String, CasteCategoryEntity> casteCategoryCache,
                                          Map<String, Party> partyCache,
                                          Map<String, Language> languageCache,
                                          Map<String, BenefitSchemes> benefitSchemeCache,
                                          Map<String, Availability> availabilityCache,
                                          Map<String, FeedbackIssue> feedbackIssueCache,
                                          Map<String, VoterHistoryEntity> voterHistoryCache) {
        try {
            // Religion
            if (headerMapping.containsKey("religion_name")) {
                String religionName = getCellValue(row, headerMapping.get("religion_name"));
                if (religionName != null && !religionName.trim().isEmpty()) {
                    ReligionEntity religion = voterReferenceDataService.getOrCreateReligion(religionName, accountId, electionId, religionCache);
                    if (religion != null) voter.setReligion(religion);
                }
            }
            
            // Caste (requires religion)
            if (headerMapping.containsKey("caste_name")) {
                String casteName = getCellValue(row, headerMapping.get("caste_name"));
                if (casteName != null && !casteName.trim().isEmpty()) {
                    // Caste must have a religion - use voter's religion or skip
                    ReligionEntity religion = voter.getReligion();
                    if (religion != null) {
                        CasteEntity caste = voterReferenceDataService.getOrCreateCaste(casteName, religion, accountId, electionId, casteCache);
                        if (caste != null) voter.setCaste(caste);
                    } else {
                        log.warn("Cannot create caste {} without religion for voter {}", casteName, voter.getEpicNumber());
                    }
                }
            }
            
            // Sub Caste (requires caste and religion)
            if (headerMapping.containsKey("sub_caste_name")) {
                String subCasteName = getCellValue(row, headerMapping.get("sub_caste_name"));
                if (subCasteName != null && !subCasteName.trim().isEmpty()) {
                    // SubCaste must have both caste and religion
                    CasteEntity caste = voter.getCaste();
                    ReligionEntity religion = voter.getReligion();
                    if (caste != null && religion != null) {
                        SubCasteEntity subCaste = voterReferenceDataService.getOrCreateSubCaste(subCasteName, caste, religion, accountId, electionId, subCasteCache);
                        if (subCaste != null) voter.setSubCaste(subCaste);
                    } else {
                        log.warn("Cannot create sub-caste {} without caste and religion for voter {}", subCasteName, voter.getEpicNumber());
                    }
                }
            }
            
            // Caste Category
            if (headerMapping.containsKey("caste_category_name")) {
                String casteCategoryName = getCellValue(row, headerMapping.get("caste_category_name"));
                if (casteCategoryName != null && !casteCategoryName.trim().isEmpty()) {
                    CasteCategoryEntity casteCategory = voterReferenceDataService.getOrCreateCasteCategory(casteCategoryName, accountId, electionId, casteCategoryCache);
                    if (casteCategory != null) voter.setCasteCategory(casteCategory);
                }
            }
            
            // Party
            if (headerMapping.containsKey("party_name")) {
                String partyName = getCellValue(row, headerMapping.get("party_name"));
                if (partyName != null && !partyName.trim().isEmpty()) {
                    Party party = voterReferenceDataService.getOrCreateParty(partyName, accountId, electionId, partyCache);
                    if (party != null) voter.setParty(party);
                }
            }
            
            // Availability
            if (headerMapping.containsKey("availability_category_name")) {
                String availabilityCategoryName = getCellValue(row, headerMapping.get("availability_category_name"));
                if (availabilityCategoryName != null && !availabilityCategoryName.trim().isEmpty()) {
                    Availability availability = voterReferenceDataService.getOrCreateAvailabilityByCategoryName(availabilityCategoryName, accountId, electionId, availabilityCache);
                    if (availability != null) voter.setAvailability1(availability);
                }
            }
            
            // Languages (single language support for now)
            if (headerMapping.containsKey("languages")) {
                String languagesStr = getCellValue(row, headerMapping.get("languages"));
                if (languagesStr != null && !languagesStr.trim().isEmpty()) {
                    String[] languageNames = languagesStr.split(",");
                    Set<Language> languages = new HashSet<>();
                    for (String langName : languageNames) {
                        String key = normalize(langName);
                        Language language = languageCache.get(key);
                        if (language != null) {
                            languages.add(language);
                        }
                    }
                    if (!languages.isEmpty()) {
                        voter.setLanguages(languages);
                    }
                }
            }
            
            // Note: Benefit Schemes, Voter History, Feedback Issues require Many-to-Many setup
            // These will be added in a future enhancement as they need VoterBenefitScheme join entities
            // For now, we support the main reference fields that use ManyToOne relationships
            
        } catch (Exception e) {
            log.warn("Error resolving relationships for voter {}: {}", voter.getEpicNumber(), e.getMessage());
        }
    }
    
    /**
     * Resolve dynamic field relationships from CSV fields array
     * @return List of benefit scheme names for post-processing
     */
    private List<String> resolveVoterRelationshipsFromCsv(String[] fields, Map<String, Integer> headerMapping, VoterEntity voter,
                                                   Long accountId, Long electionId,
                                                   Map<String, ReligionEntity> religionCache,
                                                   Map<String, CasteEntity> casteCache,
                                                   Map<String, SubCasteEntity> subCasteCache,
                                                   Map<String, CasteCategoryEntity> casteCategoryCache,
                                                   Map<String, Party> partyCache,
                                                   Map<String, Language> languageCache,
                                                   Map<String, BenefitSchemes> benefitSchemeCache,
                                                   Map<String, Availability> availabilityCache,
                                                   Map<String, FeedbackIssue> feedbackIssueCache,
                                                   Map<String, VoterHistoryEntity> voterHistoryCache) {
        List<String> benefitSchemeNames = new ArrayList<>(); // Track benefit schemes for return
        try {
            // Religion
            if (headerMapping.containsKey("religion_name")) {
                Integer idx = headerMapping.get("religion_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String religionName = fields[idx].trim();
                    ReligionEntity religion = voterReferenceDataService.getOrCreateReligion(religionName, accountId, electionId, religionCache);
                    if (religion != null) voter.setReligion(religion);
                }
            }
            
            // Caste (requires religion)
            if (headerMapping.containsKey("caste_name")) {
                Integer idx = headerMapping.get("caste_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String casteName = fields[idx].trim();
                    // Caste must have a religion - use voter's religion or skip
                    ReligionEntity religion = voter.getReligion();
                    if (religion != null) {
                        CasteEntity caste = voterReferenceDataService.getOrCreateCaste(casteName, religion, accountId, electionId, casteCache);
                        if (caste != null) voter.setCaste(caste);
                    } else {
                        log.warn("Cannot create caste {} without religion for voter {}", casteName, voter.getEpicNumber());
                    }
                }
            }
            
            // SubCaste (requires caste and religion)
            if (headerMapping.containsKey("sub_caste_name")) {
                Integer idx = headerMapping.get("sub_caste_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String subCasteName = fields[idx].trim();
                    // SubCaste must have both caste and religion
                    CasteEntity caste = voter.getCaste();
                    ReligionEntity religion = voter.getReligion();
                    if (caste != null && religion != null) {
                        SubCasteEntity subCaste = voterReferenceDataService.getOrCreateSubCaste(subCasteName, caste, religion, accountId, electionId, subCasteCache);
                        if (subCaste != null) voter.setSubCaste(subCaste);
                    } else {
                        log.warn("Cannot create sub-caste {} without caste and religion for voter {}", subCasteName, voter.getEpicNumber());
                    }
                }
            }
            
            // Caste Category
            if (headerMapping.containsKey("caste_category_name")) {
                Integer idx = headerMapping.get("caste_category_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String casteCategoryName = fields[idx].trim();
                    CasteCategoryEntity casteCategory = voterReferenceDataService.getOrCreateCasteCategory(casteCategoryName, accountId, electionId, casteCategoryCache);
                    if (casteCategory != null) voter.setCasteCategory(casteCategory);
                }
            }
            
            // Party
            if (headerMapping.containsKey("party_name")) {
                Integer idx = headerMapping.get("party_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String partyName = fields[idx].trim();
                    Party party = voterReferenceDataService.getOrCreateParty(partyName, accountId, electionId, partyCache);
                    if (party != null) voter.setParty(party);
                }
            }
            
            // Availability
            if (headerMapping.containsKey("availability_category_name")) {
                Integer idx = headerMapping.get("availability_category_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String availabilityCategoryName = fields[idx].trim();
                    Availability availability = voterReferenceDataService.getOrCreateAvailabilityByCategoryName(availabilityCategoryName, accountId, electionId, availabilityCache);
                    if (availability != null) voter.setAvailability1(availability);
                }
            }
            
            // Languages (comma-separated)
            if (headerMapping.containsKey("languages")) {
                Integer idx = headerMapping.get("languages");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String languagesStr = fields[idx].trim();
                    log.debug("Processing languages for voter {}: raw value = '{}'", voter.getEpicNumber(), languagesStr);
                    String[] languageNames = languagesStr.split(",");
                    Set<Language> languages = new HashSet<>();
                    for (String langName : languageNames) {
                        Language language = flexibleCacheMatch(langName, languageCache);
                        if (language != null) {
                            languages.add(language);
                            log.debug("Matched language '{}' for voter {}", langName, voter.getEpicNumber());
                        } else {
                            log.warn("Language '{}' not found in cache for voter {}", langName, voter.getEpicNumber());
                        }
                    }
                    if (!languages.isEmpty()) {
                        voter.setLanguages(languages);
                        log.debug("Set {} languages for voter {}", languages.size(), voter.getEpicNumber());
                    } else {
                        log.warn("No languages matched for voter {} from: {}", voter.getEpicNumber(), languagesStr);
                    }
                }
            } else {
                log.debug("No 'languages' column found in CSV headers");
            }
            
            // Benefit Schemes (comma-separated names) - Will be processed after save
            if (headerMapping.containsKey("benefit_schemes_name")) {
                Integer idx = headerMapping.get("benefit_schemes_name");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String schemesStr = fields[idx].trim();
                    String[] schemeNames = schemesStr.split(",");
                    for (String schemeName : schemeNames) {
                        String trimmed = schemeName.trim();
                        if (!trimmed.isEmpty()) {
                            benefitSchemeNames.add(trimmed);
                        }
                    }
                    if (!benefitSchemeNames.isEmpty()) {
                        log.debug("Captured {} benefit schemes for voter {}: {}", 
                            benefitSchemeNames.size(), voter.getEpicNumber(), String.join(", ", benefitSchemeNames));
                    }
                }
            }
            
            // Feedback Issues (comma-separated names)
            if (headerMapping.containsKey("feedback_issue_names")) {
                Integer idx = headerMapping.get("feedback_issue_names");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String issuesStr = fields[idx].trim();
                    log.debug("Processing feedback issues for voter {}: raw value = '{}'", voter.getEpicNumber(), issuesStr);
                    String[] issueNames = issuesStr.split(",");
                    Set<FeedbackIssue> issues = new HashSet<>();
                    for (String issueName : issueNames) {
                        FeedbackIssue issue = flexibleCacheMatch(issueName, feedbackIssueCache);
                        if (issue != null) {
                            issues.add(issue);
                            log.debug("Matched feedback issue '{}' for voter {}", issueName, voter.getEpicNumber());
                        } else {
                            log.warn("Feedback issue '{}' not found in cache for voter {}", issueName, voter.getEpicNumber());
                        }
                    }
                    if (!issues.isEmpty()) {
                        voter.setFeedbackIssues(issues);
                        log.debug("Set {} feedback issues for voter {}", issues.size(), voter.getEpicNumber());
                    } else {
                        log.warn("No feedback issues matched for voter {} from: {}", voter.getEpicNumber(), issuesStr);
                    }
                }
            } else {
                log.debug("No 'feedback_issue_names' column found in CSV headers");
            }
            
            // Voter Histories (comma-separated names)
            if (headerMapping.containsKey("voter_history_names")) {
                Integer idx = headerMapping.get("voter_history_names");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String historiesStr = fields[idx].trim();
                    log.debug("Processing voter histories for voter {}: raw value = '{}'", voter.getEpicNumber(), historiesStr);
                    String[] historyNames = historiesStr.split(",");
                    Set<VoterHistoryEntity> histories = new HashSet<>();
                    for (String historyName : historyNames) {
                        VoterHistoryEntity history = flexibleCacheMatch(historyName, voterHistoryCache);
                        if (history != null) {
                            histories.add(history);
                            log.debug("Matched voter history '{}' for voter {}", historyName, voter.getEpicNumber());
                        } else {
                            log.warn("Voter history '{}' not found in cache for voter {}", historyName, voter.getEpicNumber());
                        }
                    }
                    if (!histories.isEmpty()) {
                        voter.setVoterHistories(histories);
                        log.debug("Set {} voter histories for voter {}", histories.size(), voter.getEpicNumber());
                    } else {
                        log.warn("No voter histories matched for voter {} from: {}", voter.getEpicNumber(), historiesStr);
                    }
                }
            } else {
                log.debug("No 'voter_history_names' column found in CSV headers");
            }
            
            // Dynamic Fields - Handle any custom fields
            Map<String, String> dynamicFields = new HashMap<>();
            for (Map.Entry<String, Integer> entry : headerMapping.entrySet()) {
                String header = entry.getKey();
                Integer idx = entry.getValue();
                
                // Skip known system fields
                if (isSystemField(header)) continue;
                
                // This is a dynamic field
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String value = fields[idx].trim();
                    dynamicFields.put(header, value);
                }
            }
            
            if (!dynamicFields.isEmpty()) {
                voter.setDynamicFields(dynamicFields);
            }
            
            // Star Number
            if (headerMapping.containsKey("star_number")) {
                Integer idx = headerMapping.get("star_number");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    String value = fields[idx].trim();
                    voter.setStarNumber(Boolean.parseBoolean(value) || "true".equalsIgnoreCase(value) || "1".equals(value));
                }
            }
            
            // Family Sequence Number
            if (headerMapping.containsKey("family_sequence_number")) {
                Integer idx = headerMapping.get("family_sequence_number");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    try {
                        voter.setFamilySequenceNumber(Integer.parseInt(fields[idx].trim()));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid family_sequence_number: {}", fields[idx]);
                    }
                }
            }
            
            // Friend ID
            if (headerMapping.containsKey("friend_id")) {
                Integer idx = headerMapping.get("friend_id");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    try {
                        voter.setFriendId(UUID.fromString(fields[idx].trim()));
                    } catch (Exception e) {
                        log.warn("Invalid friend_id UUID: {}", fields[idx]);
                    }
                }
            }
            
            // Friends Details (JSON string)
            if (headerMapping.containsKey("friends_details")) {
                Integer idx = headerMapping.get("friends_details");
                if (idx < fields.length && !fields[idx].trim().isEmpty()) {
                    try {
                        // Parse JSON string into List<FriendDetail>
                        String friendsJson = fields[idx].trim();
                        List<FriendDetail> friendsList = objectMapper.readValue(
                            friendsJson, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, FriendDetail.class)
                        );
                        voter.setFriendsDetails(friendsList);
                    } catch (Exception e) {
                        log.warn("Invalid friends_details JSON for voter {}: {}", voter.getEpicNumber(), e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            log.warn("Error resolving relationships for CSV voter {}: {}", voter.getEpicNumber(), e.getMessage());
        }
        
        return benefitSchemeNames;
    }
    
    /**
     * Check if a header is a system field (not a dynamic field)
     */
    private boolean isSystemField(String header) {
        String normalized = header.toLowerCase();
        return normalized.equals("epic_number") || normalized.equals("voter_id") ||
               normalized.equals("part_no") || normalized.equals("gender") ||
               normalized.equals("voter_fname_en") || normalized.equals("voter_lname_en") ||
               normalized.equals("age") || normalized.equals("mobile_no") ||
               normalized.equals("serial_no") || normalized.equals("house_no_en") ||
               normalized.equals("house_no_l1") || normalized.equals("house_no_l2") ||
               normalized.equals("voter_fname_l1") || normalized.equals("voter_fname_l2") ||
               normalized.equals("voter_lname_l1") || normalized.equals("voter_lname_l2") ||
               normalized.equals("rln_type") || normalized.equals("rln_fname_en") ||
               normalized.equals("rln_lname_en") || normalized.equals("rln_fname_l1") ||
               normalized.equals("rln_lname_l1") || normalized.equals("rln_fname_l2") ||
               normalized.equals("rln_lname_l2") || normalized.equals("section_no") ||
               normalized.equals("section_name_en") || normalized.equals("section_name_l1") ||
               normalized.equals("section_name_l2") || normalized.equals("full_address") ||
               normalized.equals("dob") || normalized.equals("whatsapp_no") ||
               normalized.equals("e_mail") || normalized.equals("email") ||
               normalized.equals("voter_lati") || normalized.equals("voter_longi") ||
               normalized.equals("state_code") || normalized.equals("state_name_en") ||
               normalized.equals("state_name_l1") || normalized.equals("state_name_l2") ||
               normalized.equals("district_code") || normalized.equals("district_name_en") ||
               normalized.equals("district_name_l1") || normalized.equals("district_name_l2") ||
               normalized.equals("pc_no") || normalized.equals("pc_name_en") ||
               normalized.equals("pc_name_l1") || normalized.equals("pc_name_l2") ||
               normalized.equals("ac_no") || normalized.equals("ac_name_en") ||
               normalized.equals("ac_name_l1") || normalized.equals("ac_name_l2") ||
               normalized.equals("urban_no") || normalized.equals("urban_name_en") ||
               normalized.equals("urban_name_l1") || normalized.equals("urban_ward_no") ||
               normalized.equals("rur_district_union_no") || normalized.equals("rur_district_union_name_en") ||
               normalized.equals("rur_district_union_name_l1") || normalized.equals("rur_district_union_name_l2") ||
               normalized.equals("rur_district_union_ward_no") || normalized.equals("pan_union_no") ||
               normalized.equals("pan_union_name_en") || normalized.equals("pan_union_name_l1") ||
               normalized.equals("pan_union_name_l2") || normalized.equals("pan_union_ward_no") ||
               normalized.equals("vill_pan_no") || normalized.equals("vill_pan_name_en") ||
               normalized.equals("vill_pan_name_l1") || normalized.equals("vill_pan_ward_no") ||
               normalized.equals("pincode") || normalized.equals("part_name_en") ||
               normalized.equals("part_name_l1") || normalized.equals("part_name_l2") ||
               normalized.equals("star_number") || normalized.equals("page_number") ||
               normalized.equals("part_lati") || normalized.equals("part_long") ||
               normalized.equals("pan_number") || normalized.equals("aadhaar_number") ||
               normalized.equals("remarks") || normalized.equals("photo_url") ||
               normalized.equals("aadhaar_verified") || normalized.equals("friend_count") ||
               normalized.equals("family_id") || normalized.equals("friend_id") ||
               normalized.equals("family_count") || normalized.equals("family_sequence_number") ||
               normalized.equals("family_display_part") || normalized.equals("is_family_head") ||
               normalized.equals("friends_details") || normalized.equals("family_slip_print_count") ||
               normalized.equals("party_registration_number") || normalized.equals("membership_number") ||
               normalized.equals("religion_name") || normalized.equals("caste_name") ||
               normalized.equals("sub_caste_name") || normalized.equals("caste_category_name") ||
               normalized.equals("party_name") || normalized.equals("party_short_name") ||
               normalized.equals("availability_description") || normalized.equals("availability_category_name") ||
               normalized.equals("languages") || normalized.equals("benefit_schemes_name") ||
               normalized.equals("benefit_schemes_by") || normalized.equals("scheme") ||
               normalized.equals("voter_history_names") || normalized.equals("feedback_issue_names");
    }
    
    /**
     * Create DynamicFieldEntity definitions for any custom fields found in CSV headers
     */
    private void createDynamicFieldDefinitions(Map<String, Integer> headerMapping, Long accountId, Long electionId) {
        try {
            // Get existing dynamic fields for this election
            List<DynamicFieldEntity> existingFields = dynamicFieldRepository
                .findByElectionIdAndAccountId(electionId, accountId);
            
            Map<String, DynamicFieldEntity> existingFieldMap = existingFields.stream()
                .collect(Collectors.toMap(
                    f -> normalize(f.getName()),
                    f -> f
                ));
            
            // Track fields to create
            List<DynamicFieldEntity> fieldsToCreate = new ArrayList<>();
            int nextOrderIndex = existingFields.stream()
                .mapToInt(DynamicFieldEntity::getOrderIndex)
                .max()
                .orElse(-1) + 1;
            
            // Check each header for new dynamic fields
            for (String header : headerMapping.keySet()) {
                if (isSystemField(header)) continue;
                
                String normalizedHeader = normalize(header);
                if (!existingFieldMap.containsKey(normalizedHeader)) {
                    // Create new dynamic field definition
                    DynamicFieldEntity newField = new DynamicFieldEntity();
                    newField.setName(header);
                    newField.setLabel(formatLabel(header));
                    newField.setType("text"); // Default to text type
                    newField.setRequired(false);
                    newField.setStatus(true); // Active by default
                    newField.setAccountId(accountId);
                    newField.setElectionId(electionId);
                    newField.setOrderIndex(nextOrderIndex++);
                    
                    fieldsToCreate.add(newField);
                    log.info("Discovered new dynamic field from CSV: {} for election {}", header, electionId);
                }
            }
            
            // Save all new fields in batch
            if (!fieldsToCreate.isEmpty()) {
                dynamicFieldRepository.saveAll(fieldsToCreate);
                log.info("Created {} new dynamic field definitions for election {}", 
                    fieldsToCreate.size(), electionId);
            }
            
        } catch (Exception e) {
            log.error("Error creating dynamic field definitions for election {}: {}", 
                electionId, e.getMessage(), e);
        }
    }
    
    /**
     * Create VoterBenefitScheme join entities after voters are saved
     */
    private void createBenefitSchemeJoinEntities(
            List<VoterWithRelationships> batchWrappers,
            Long accountId,
            Long electionId,
            Map<String, BenefitSchemes> benefitSchemeCache) {
        
        if (batchWrappers.isEmpty()) return;
        
        // Collect epic numbers that have benefit schemes
        Map<String, List<String>> epicToSchemes = new HashMap<>();
        for (VoterWithRelationships wrapper : batchWrappers) {
            if (!wrapper.benefitSchemeNames.isEmpty()) {
                epicToSchemes.put(wrapper.voter.getEpicNumber(), wrapper.benefitSchemeNames);
            }
        }
        
        if (epicToSchemes.isEmpty()) {
            return; // No benefit schemes to process
        }
        
        // Query voter IDs by epic numbers
        String epicList = epicToSchemes.keySet().stream()
            .map(epic -> "'" + epic.replace("'", "''") + "'")
            .collect(Collectors.joining(","));
        
        String query = "SELECT id, epic_number FROM voters WHERE epic_number IN (" + epicList + ") " +
                      "AND election_id = ? AND account_id = ?";
        
        Map<String, Long> epicToVoterId = new HashMap<>();
        try {
            jdbcTemplate.query(query, rs -> {
                epicToVoterId.put(rs.getString("epic_number"), rs.getLong("id"));
            }, electionId, accountId);
        } catch (Exception e) {
            log.error("Error querying voter IDs for benefit schemes: {}", e.getMessage());
            return;
        }
        
        // Create VoterBenefitScheme join entities
        List<VoterBenefitScheme> joinEntities = new ArrayList<>();
        
        for (Map.Entry<String, List<String>> entry : epicToSchemes.entrySet()) {
            String epicNumber = entry.getKey();
            Long voterId = epicToVoterId.get(epicNumber);
            
            if (voterId == null) {
                log.warn("Voter ID not found for epic {}, skipping benefit schemes", epicNumber);
                continue;
            }
            
            // Find voter entity for join
            VoterEntity voter = voterRepository.findById(voterId).orElse(null);
            if (voter == null) continue;
            
            for (String schemeName : entry.getValue()) {
                String key = normalize(schemeName);
                BenefitSchemes scheme = benefitSchemeCache.get(key);
                
                if (scheme != null) {
                    VoterBenefitScheme joinEntity = new VoterBenefitScheme();
                    joinEntity.setVoter(voter);
                    joinEntity.setBenefitScheme(scheme);
                    joinEntity.setSelected(true);
                    joinEntities.add(joinEntity);
                }
            }
        }
        
        // Batch save join entities
        if (!joinEntities.isEmpty()) {
            try {
                voterBenefitSchemeRepository.saveAll(joinEntities);
                log.info("Created {} benefit scheme associations for {} voters", 
                    joinEntities.size(), epicToSchemes.size());
            } catch (Exception e) {
                log.error("Error saving benefit scheme join entities: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * Format a field name into a readable label
     */
    private String formatLabel(String fieldName) {
        // Convert snake_case or camelCase to Title Case
        String[] words = fieldName.split("[_\\s]+");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (label.length() > 0) label.append(" ");
            if (word.length() > 0) {
                label.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    label.append(word.substring(1).toLowerCase());
                }
            }
        }
        return label.toString();
    }
    
    /**
     * Parse friends details from JSON string
     */
    // ============ HELPER METHODS FOR DYNAMIC FIELD SUPPORT ============
    
    /**
     * Wrapper class to store voter entity and temporary relationship data during parsing
     */
    private static class VoterWithRelationships {
        VoterEntity voter;
        List<String> benefitSchemeNames = new ArrayList<>();
        Map<String, String> relationshipData;
        
        VoterWithRelationships(VoterEntity voter) {
            this.voter = voter;
            this.relationshipData = new HashMap<>();
        }
    }
    
    /**
     * Normalize string for case-insensitive lookups
     */
    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
    
    /**
     * Get or create religion entity (like election merge does)
     */
    
    /**
     * Properly parse CSV line handling quoted fields, commas within quotes, and escape characters
     * This fixes the issue where addresses like "123, NETHAJI SALAI" break naive .split(",")
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                // Handle escaped quotes (two consecutive quotes)
                if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // End of field - sanitize before adding
                fields.add(sanitizeCsvField(currentField.toString()));
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add last field - sanitize before adding
        fields.add(sanitizeCsvField(currentField.toString()));
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Sanitize CSV field value by removing leading/trailing quotes and converting literal quote strings to empty strings
     */
    private String sanitizeCsvField(String field) {
        if (field == null) {
            return "";
        }
        
        String trimmed = field.trim();
        
        // Convert literal quote character to empty string
        if (trimmed.equals("\"") || trimmed.equals("\\\"")) {
            return "";
        }
        
        // Remove surrounding quotes if present
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        
        return trimmed;
    }
    
    /**
     * Flexible matching helper - tries to match with full name, then without parenthetical content
     */
    private <T> T flexibleCacheMatch(String name, Map<String, T> cache) {
        // Try exact match first
        String key = normalize(name);
        T item = cache.get(key);
        if (item != null) {
            return item;
        }
        
        // Try without parenthetical content: "Tamil (தமிழ்)" -> "Tamil"
        String withoutParentheses = name.replaceAll("\\s*\\([^)]*\\)\\s*", "").trim();
        if (!withoutParentheses.equals(name)) {
            String altKey = normalize(withoutParentheses);
            item = cache.get(altKey);
            if (item != null) {
                log.debug("Matched '{}' using base name '{}' (normalized: '{}')", name, withoutParentheses, altKey);
                return item;
            }
        }
        
        return null;
    }
}
