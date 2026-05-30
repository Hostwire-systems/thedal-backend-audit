package com.thedal.thedal_app.merge.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Duration;
import org.springframework.context.ApplicationContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.cpanel.VoterHistoryRepository;
import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.merge.MergeField;
import com.thedal.thedal_app.merge.MergeJobStatus;
import com.thedal.thedal_app.merge.dto.MergeDryRunResultDTO;
import com.thedal.thedal_app.merge.dto.MergeRequestDTO;
import com.thedal.thedal_app.merge.entity.MergeJobEntity;
import com.thedal.thedal_app.merge.repo.MergeJobRepository;
import com.thedal.thedal_app.settings.electionsettings.AvailabilityRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteCategoryRepository;
import com.thedal.thedal_app.settings.electionsettings.CasteRepository;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssueRepository;
import com.thedal.thedal_app.settings.electionsettings.LanguageRepository;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.settings.electionsettings.ReligionRepository;
import com.thedal.thedal_app.settings.electionsettings.SubCasteRepository;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemesRepository;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import com.thedal.thedal_app.voter.VoterBenefitScheme;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ElectionDataMergeService {

    @PersistenceContext
    private EntityManager entityManager;

    private final MergeJobRepository mergeJobRepository;
    private final VoterRepo voterRepo;
    private final ReligionRepository religionRepository;
    private final CasteRepository casteRepository;
    private final SubCasteRepository subCasteRepository;
    private final CasteCategoryRepository casteCategoryRepository;
    private final PartyRepository partyRepository;
    private final AvailabilityRepository availabilityRepository;
    private final LanguageRepository languageRepository;
    private final FeedbackIssueRepository feedbackIssueRepository;
    private final VoterHistoryRepository voterHistoryRepository;
    private final BenefitSchemesRepository benefitSchemesRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationContext applicationContext;

    // Constructor initializes ObjectMapper with Java 8 date/time support
    public ElectionDataMergeService(
        MergeJobRepository mergeJobRepository,
        VoterRepo voterRepo,
        ReligionRepository religionRepository,
        CasteRepository casteRepository,
        SubCasteRepository subCasteRepository,
        CasteCategoryRepository casteCategoryRepository,
        PartyRepository partyRepository,
        AvailabilityRepository availabilityRepository,
        LanguageRepository languageRepository,
        FeedbackIssueRepository feedbackIssueRepository,
        VoterHistoryRepository voterHistoryRepository,
        BenefitSchemesRepository benefitSchemesRepository,
        ApplicationContext applicationContext
    ) {
        this.mergeJobRepository = mergeJobRepository;
        this.voterRepo = voterRepo;
        this.religionRepository = religionRepository;
        this.casteRepository = casteRepository;
        this.subCasteRepository = subCasteRepository;
        this.casteCategoryRepository = casteCategoryRepository;
        this.partyRepository = partyRepository;
        this.availabilityRepository = availabilityRepository;
        this.languageRepository = languageRepository;
        this.feedbackIssueRepository = feedbackIssueRepository;
        this.voterHistoryRepository = voterHistoryRepository;
        this.benefitSchemesRepository = benefitSchemesRepository;
        this.applicationContext = applicationContext;
        
        // Configure ObjectMapper for Java 8 date/time types
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // Access the proxied self to ensure @Transactional/@Async are applied on internal calls
    private ElectionDataMergeService self() {
        return applicationContext.getBean(ElectionDataMergeService.class);
    }

    // Listing jobs for an election (paged)
    public Page<MergeJobEntity> listJobs(Long electionId, Pageable pageable) {
        return mergeJobRepository.findByTargetElectionIdOrderByCreatedAtDesc(electionId, pageable);
    }

    // List ALL active jobs for debugging
    public List<MergeJobEntity> listActiveJobs(Long electionId) {
        return mergeJobRepository.findByTargetElectionIdAndStatusIn(electionId, 
            EnumSet.of(MergeJobStatus.PENDING, MergeJobStatus.RUNNING));
    }

    // Force cancel a stuck job
    @Transactional
    public void cancelJob(UUID jobId, Long targetElectionId) {
        var job = mergeJobRepository.findByIdAndTargetElectionId(jobId, targetElectionId)
            .orElseThrow(() -> new ThedalException(ThedalError.MERGE_JOB_NOT_FOUND, 
                org.springframework.http.HttpStatus.NOT_FOUND, "Merge job not found"));
        
        if (job.getStatus() == MergeJobStatus.COMPLETED) {
            throw new ThedalException(ThedalError.INVALID_REQUEST, 
                org.springframework.http.HttpStatus.BAD_REQUEST, "Cannot cancel a completed job");
        }
        
        job.setStatus(MergeJobStatus.FAILED);
        job.setErrorMessage("Job cancelled by user");
        job.setFinishedAt(Instant.now());
        mergeJobRepository.save(job);
        
        log.info("Job {} cancelled by user", jobId);
    }

    // Enqueue dry-run job (async)
    @Transactional
    public UUID enqueueDryRun(Long accountId, Long userId, Long targetElectionId, MergeRequestDTO request) {
        if (request.getSourceElectionId().equals(targetElectionId)) {
            throw new ThedalException(ThedalError.INVALID_REQUEST, org.springframework.http.HttpStatus.BAD_REQUEST, "Source and target election cannot be the same");
        }
        if (request.getFields() == null || request.getFields().isEmpty()) {
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, org.springframework.http.HttpStatus.BAD_REQUEST, "No fields selected");
        }
        
        // Check if another merge job is already running
        if (mergeJobRepository.existsByTargetElectionIdAndStatusIn(targetElectionId, EnumSet.of(MergeJobStatus.PENDING, MergeJobStatus.RUNNING))) {
            List<MergeJobEntity> activeJobs = mergeJobRepository.findByTargetElectionIdAndStatusIn(targetElectionId, EnumSet.of(MergeJobStatus.PENDING, MergeJobStatus.RUNNING));
            if (!activeJobs.isEmpty()) {
                MergeJobEntity blockingJob = activeJobs.get(0);
                String jobInfo = String.format("Job ID: %s, Status: %s, Started: %s", 
                    blockingJob.getId(), 
                    blockingJob.getStatus(),
                    blockingJob.getStartedAt() != null ? blockingJob.getStartedAt() : blockingJob.getCreatedAt());
                throw new ThedalException(ThedalError.MERGE_JOB_IN_PROGRESS, org.springframework.http.HttpStatus.CONFLICT, 
                    "A merge operation is already in progress for this election. " + jobInfo + ". Please wait for it to complete before starting a dry-run.");
            }
        }

        MergeJobEntity job = new MergeJobEntity();
        job.setAccountId(accountId);
        job.setUserId(userId);
        job.setSourceElectionId(request.getSourceElectionId());
        job.setTargetElectionId(targetElectionId);
        job.setFields(request.getFields());
        job.setStatus(MergeJobStatus.PENDING);
        mergeJobRepository.save(job);
        log.info("Enqueued dry-run job {} for targetElection {}", job.getId(), targetElectionId);
        return job.getId();
    }

    // Job detail with fields loaded
    public Optional<MergeJobEntity> getJobDetail(UUID jobId, Long electionId) {
        return mergeJobRepository.findByIdAndTargetElectionId(jobId, electionId)
                .flatMap(j -> mergeJobRepository.findWithFieldsById(j.getId()));
    }

    // Force mark job failed if stuck in PENDING/RUNNING
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forceFailJob(UUID jobId, Long electionId, String reason) {
        MergeJobEntity job = mergeJobRepository.findByIdAndTargetElectionId(jobId, electionId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found"));
    if (job.getStatus() == MergeJobStatus.COMPLETED || job.getStatus() == MergeJobStatus.FAILED || job.getStatus() == MergeJobStatus.CANCELED) {
            return; // already finished
        }
        job.setStatus(MergeJobStatus.FAILED);
        job.setErrorMessage(reason != null ? reason : "Force failed by user");
        job.setFinishedAt(Instant.now());
        mergeJobRepository.save(job);
    }

    // Dry run skeleton (real implementation to be expanded as we wire repositories)
    @Transactional(readOnly = true)
    public MergeDryRunResultDTO dryRun(Long accountId, Long userId, Long targetElectionId, MergeRequestDTO request) {
        if (request.getSourceElectionId().equals(targetElectionId)) {
            throw new ThedalException(ThedalError.INVALID_REQUEST, org.springframework.http.HttpStatus.BAD_REQUEST, "Source and target election cannot be the same");
        }

        List<MergeField> requested = request.getFields() == null ? List.of() : request.getFields();
        if (requested.isEmpty()) {
            throw new ThedalException(ThedalError.MISSING_REQUIRED_FIELDS, org.springframework.http.HttpStatus.BAD_REQUEST, "No fields selected");
        }

        Map<String, Object> fieldStats = new HashMap<>();
        Map<String, Object> fieldAvailability = new HashMap<>();
        List<String> warnings = new ArrayList<>();

        // Preload target reference names once to avoid per-voter DB lookups
        Set<String> targetReligionNames = new HashSet<>();
        religionRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(r -> { if (r.getReligionName()!=null) targetReligionNames.add(r.getReligionName().trim().toLowerCase()); });
        Set<String> targetCasteNames = new HashSet<>();
        casteRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(c -> { if (c.getCasteName()!=null) targetCasteNames.add(c.getCasteName().trim().toLowerCase()); });
        Set<String> targetSubCasteNames = new HashSet<>();
        subCasteRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(sc -> { if (sc.getSubCasteName()!=null) targetSubCasteNames.add(sc.getSubCasteName().trim().toLowerCase()); });
        Set<String> targetCasteCategoryNames = new HashSet<>();
        casteCategoryRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(cc -> { if (cc.getCasteCategoryName()!=null) targetCasteCategoryNames.add(cc.getCasteCategoryName().trim().toLowerCase()); });
        Set<String> targetPartyNames = new HashSet<>();
        partyRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(p -> { if (p.getPartyName()!=null) targetPartyNames.add(p.getPartyName().trim().toLowerCase()); });
        Set<String> targetAvailabilityNames = new HashSet<>();
        availabilityRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(a -> { if (a.getAvailabilityName()!=null) targetAvailabilityNames.add(a.getAvailabilityName().trim().toLowerCase()); });
        Set<String> targetLanguageNames = new HashSet<>();
        languageRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(l -> { if (l.getLanguageName()!=null) targetLanguageNames.add(l.getLanguageName().trim().toLowerCase()); });
        Set<String> targetFeedbackIssueNames = new HashSet<>();
        feedbackIssueRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(fi -> { if (fi.getIssueName()!=null) targetFeedbackIssueNames.add(fi.getIssueName().trim().toLowerCase()); });
        Set<String> targetHistoryNames = new HashSet<>();
        voterHistoryRepository.findByAccountIdAndElectionId(accountId, targetElectionId)
            .forEach(h -> { if (h.getVoterHistoryName()!=null) targetHistoryNames.add(h.getVoterHistoryName().trim().toLowerCase()); });

        // 1. Collect source + target EPIC sets (with eager fetching to avoid LazyInitializationException)
        List<VoterEntity> sourceVoters = voterRepo.findByAccountIdAndElectionIdWithHistoriesAndFeedback(accountId, request.getSourceElectionId());
        List<VoterEntity> targetVoters = voterRepo.findByAccountIdAndElectionIdWithHistoriesAndFeedback(accountId, targetElectionId);
        Map<String, VoterEntity> targetByEpic = new HashMap<>();
        for (VoterEntity v : targetVoters) {
            targetByEpic.put(normalizeEpic(v.getEpicNumber()), v);
        }
        long missingInTarget = 0;
        long matched = 0;
        // Precompute diffs per field
        Map<MergeField, Long> updatesPerField = new HashMap<>();
        Set<String> missingReligionNames = new HashSet<>();
        Set<String> missingCasteNames = new HashSet<>();
        Set<String> missingSubCasteNames = new HashSet<>();
        Set<String> missingCasteCategoryNames = new HashSet<>();
        Set<String> missingPartyNames = new HashSet<>();
        Set<String> missingAvailabilityNames = new HashSet<>();
        Set<String> missingLanguageNames = new HashSet<>();
        Set<String> missingFeedbackIssueNames = new HashSet<>();
        Set<String> missingHistoryNames = new HashSet<>();
        long familyMappingSkipped = 0; // placeholder; actual requires family group check
        long friendsMappingSkipped = 0; // placeholder
        List<String> missingEpicSample = new ArrayList<>();

        for (VoterEntity sv : sourceVoters) {
            String epicKey = normalizeEpic(sv.getEpicNumber());
            VoterEntity tv = targetByEpic.get(epicKey);
            if (tv == null) {
                missingInTarget++;
                if (missingEpicSample.size() < 10) missingEpicSample.add(sv.getEpicNumber());
                continue;
            }
            matched++;
            // For now, we approximate 'would change' counts by simple inequality & non-null source
            for (MergeField field : requested) {
                switch (field) {
                    // Contact & identity
                    case MOBILE_NUMBER -> countIfDiff(sv.getMobileNo(), tv.getMobileNo(), updatesPerField, field);
                    case WHATSAPP_NUMBER -> countIfDiff(sv.getWhatsappNo(), tv.getWhatsappNo(), updatesPerField, field);
                    case EMAIL_ID -> countIfDiff(sv.getEMail(), tv.getEMail(), updatesPerField, field);
                    case AADHAAR_NUMBER -> countIfDiff(sv.getAadhaarNumber(), tv.getAadhaarNumber(), updatesPerField, field);
                    case PAN_NUMBER -> countIfDiff(sv.getPanNumber(), tv.getPanNumber(), updatesPerField, field);
                    case MEMBERSHIP_NUMBER -> countIfDiff(sv.getPartyRegistrationNumber(), tv.getPartyRegistrationNumber(), updatesPerField, field);

                    // Personal details
                    case DATE_OF_BIRTH -> countIfDiff(sv.getDob(), tv.getDob(), updatesPerField, field);
                    case AGE -> countIfDiff(sv.getAge(), tv.getAge(), updatesPerField, field);
                    case GENDER -> countIfDiff(sv.getGender(), tv.getGender(), updatesPerField, field);
                    case PHOTO_URL -> countIfDiff(sv.getPhotoUrl(), tv.getPhotoUrl(), updatesPerField, field);
                    case VIDEO_URL -> countIfDiff(sv.getVideoUrl(), tv.getVideoUrl(), updatesPerField, field);
                    case STAR_NUMBER -> countIfDiff(sv.getStarNumber(), tv.getStarNumber(), updatesPerField, field);

                    // Geo & address
                    case VOTER_LATITUDE -> countIfDiff(sv.getVoterLati(), tv.getVoterLati(), updatesPerField, field);
                    case VOTER_LONGITUDE -> countIfDiff(sv.getVoterLongi(), tv.getVoterLongi(), updatesPerField, field);
                    case PART_LATITUDE -> countIfDiff(sv.getPartLati(), tv.getPartLati(), updatesPerField, field);
                    case PART_LONGITUDE -> countIfDiff(sv.getPartLong(), tv.getPartLong(), updatesPerField, field);
                    case FULL_ADDRESS -> countIfDiff(sv.getFullAddress(), tv.getFullAddress(), updatesPerField, field);
                    case PINCODE -> countIfDiff(sv.getPincode(), tv.getPincode(), updatesPerField, field);

                    // Part/Section identifiers
                    case BOOTH_NUMBER -> countIfDiff(sv.getBoothNumber(), tv.getBoothNumber(), updatesPerField, field);
                    case PART_NUMBER -> countIfDiff(sv.getPartNo(), tv.getPartNo(), updatesPerField, field);
                    case SECTION_NUMBER -> countIfDiff(sv.getSectionNo(), tv.getSectionNo(), updatesPerField, field);
                    case SERIAL_NUMBER -> countIfDiff(sv.getSerialNo(), tv.getSerialNo(), updatesPerField, field);
                    case PAGE_NUMBER -> countIfDiff(sv.getPageNumber(), tv.getPageNumber(), updatesPerField, field);

                    // House & names
                    case HOUSE_NO_EN -> countIfDiff(sv.getHouseNoEn(), tv.getHouseNoEn(), updatesPerField, field);
                    case HOUSE_NO_L1 -> countIfDiff(sv.getHouseNoL1(), tv.getHouseNoL1(), updatesPerField, field);
                    case HOUSE_NO_L2 -> countIfDiff(sv.getHouseNoL2(), tv.getHouseNoL2(), updatesPerField, field);
                    case VOTER_FNAME_EN -> countIfDiff(sv.getVoterFnameEn(), tv.getVoterFnameEn(), updatesPerField, field);
                    case VOTER_LNAME_EN -> countIfDiff(sv.getVoterLnameEn(), tv.getVoterLnameEn(), updatesPerField, field);
                    case VOTER_FNAME_L1 -> countIfDiff(sv.getVoterFnameL1(), tv.getVoterFnameL1(), updatesPerField, field);
                    case VOTER_LNAME_L1 -> countIfDiff(sv.getVoterLnameL1(), tv.getVoterLnameL1(), updatesPerField, field);
                    case VOTER_FNAME_L2 -> countIfDiff(sv.getVoterFnameL2(), tv.getVoterFnameL2(), updatesPerField, field);
                    case VOTER_LNAME_L2 -> countIfDiff(sv.getVoterLnameL2(), tv.getVoterLnameL2(), updatesPerField, field);

                    // Relation details
                    case RLN_TYPE -> countIfDiff(sv.getRlnType(), tv.getRlnType(), updatesPerField, field);
                    case RLN_FNAME_EN -> countIfDiff(sv.getRlnFnameEn(), tv.getRlnFnameEn(), updatesPerField, field);
                    case RLN_LNAME_EN -> countIfDiff(sv.getRlnLnameEn(), tv.getRlnLnameEn(), updatesPerField, field);
                    case RLN_FNAME_L1 -> countIfDiff(sv.getRlnFnameL1(), tv.getRlnFnameL1(), updatesPerField, field);
                    case RLN_LNAME_L1 -> countIfDiff(sv.getRlnLnameL1(), tv.getRlnLnameL1(), updatesPerField, field);
                    case RLN_FNAME_L2 -> countIfDiff(sv.getRlnFnameL2(), tv.getRlnFnameL2(), updatesPerField, field);
                    case RLN_LNAME_L2 -> countIfDiff(sv.getRlnLnameL2(), tv.getRlnLnameL2(), updatesPerField, field);

                    // Section/Part display names
                    case SECTION_NAME_EN -> countIfDiff(sv.getSectionNameEn(), tv.getSectionNameEn(), updatesPerField, field);
                    case SECTION_NAME_L1 -> countIfDiff(sv.getSectionNameL1(), tv.getSectionNameL1(), updatesPerField, field);
                    case SECTION_NAME_L2 -> countIfDiff(sv.getSectionNameL2(), tv.getSectionNameL2(), updatesPerField, field);
                    case PART_NAME_EN -> countIfDiff(sv.getPartNameEn(), tv.getPartNameEn(), updatesPerField, field);
                    case PART_NAME_L1 -> countIfDiff(sv.getPartNameL1(), tv.getPartNameL1(), updatesPerField, field);
                    case PART_NAME_L2 -> countIfDiff(sv.getPartNameL2(), tv.getPartNameL2(), updatesPerField, field);

                    // Key identifiers
                    case EPIC_NUMBER -> countIfDiff(sv.getEpicNumber(), tv.getEpicNumber(), updatesPerField, field);

                    // Notes
                    case REMARKS -> countIfDiff(sv.getRemarks(), tv.getRemarks(), updatesPerField, field);

                    // Religious & caste
                    case RELIGION -> {
                        if (sv.getReligion() != null) {
                            if (tv.getReligion() == null || !equalsIgnoreCase(sv.getReligion().getReligionName(), tv.getReligion().getReligionName())) {
                                updatesPerField.merge(field, 1L, Long::sum);
                            }
                        }
                        if (sv.getReligion() != null && sv.getReligion().getReligionName()!=null) {
                            String key = sv.getReligion().getReligionName().trim().toLowerCase();
                            if (!targetReligionNames.contains(key)) missingReligionNames.add(sv.getReligion().getReligionName());
                        }
                    }
                    case CASTE -> {
                        if (sv.getCaste() != null && (tv.getCaste() == null || !equalsIgnoreCase(sv.getCaste().getCasteName(), tv.getCaste().getCasteName()))) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        if (sv.getCaste() != null && sv.getCaste().getCasteName()!=null) {
                            String key = sv.getCaste().getCasteName().trim().toLowerCase();
                            if (!targetCasteNames.contains(key)) missingCasteNames.add(sv.getCaste().getCasteName());
                        }
                    }
                    case SUB_CASTE -> {
                        if (sv.getSubCaste() != null && (tv.getSubCaste() == null || !equalsIgnoreCase(sv.getSubCaste().getSubCasteName(), tv.getSubCaste().getSubCasteName()))) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        if (sv.getSubCaste() != null && sv.getSubCaste().getSubCasteName()!=null) {
                            String key = sv.getSubCaste().getSubCasteName().trim().toLowerCase();
                            if (!targetSubCasteNames.contains(key)) missingSubCasteNames.add(sv.getSubCaste().getSubCasteName());
                        }
                    }
                    case CASTE_CATEGORY -> {
                        if (sv.getCasteCategory() != null && (tv.getCasteCategory() == null || !equalsIgnoreCase(sv.getCasteCategory().getCasteCategoryName(), tv.getCasteCategory().getCasteCategoryName()))) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        if (sv.getCasteCategory() != null && sv.getCasteCategory().getCasteCategoryName()!=null) {
                            String key = sv.getCasteCategory().getCasteCategoryName().trim().toLowerCase();
                            if (!targetCasteCategoryNames.contains(key)) missingCasteCategoryNames.add(sv.getCasteCategory().getCasteCategoryName());
                        }
                    }

                    // Political
                    case PARTY -> {
                        if (sv.getParty() != null && (tv.getParty() == null || !equalsIgnoreCase(sv.getParty().getPartyName(), tv.getParty().getPartyName()))) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        if (sv.getParty() != null && sv.getParty().getPartyName()!=null) {
                            String key = sv.getParty().getPartyName().trim().toLowerCase();
                            if (!targetPartyNames.contains(key)) missingPartyNames.add(sv.getParty().getPartyName());
                        }
                    }
                    case PARTY_AFFILIATION -> countIfDiff(sv.getPartyAffiliation(), tv.getPartyAffiliation(), updatesPerField, field);
                    case VOTER_CATEGORY -> {
                        if (sv.getAvailability1() != null && (tv.getAvailability1() == null || !equalsIgnoreCase(sv.getAvailability1().getAvailabilityName(), tv.getAvailability1().getAvailabilityName()))) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        if (sv.getAvailability1() != null && sv.getAvailability1().getAvailabilityName()!=null) {
                            String key = sv.getAvailability1().getAvailabilityName().trim().toLowerCase();
                            if (!targetAvailabilityNames.contains(key)) missingAvailabilityNames.add(sv.getAvailability1().getAvailabilityName());
                        }
                    }

                    // Collections
                    case LANGUAGE -> {
                        if (!sv.getLanguages().isEmpty()) {
                            if (!languageSetsEqual(sv.getLanguages(), tv.getLanguages())) {
                                updatesPerField.merge(field, 1L, Long::sum);
                            }
                            sv.getLanguages().forEach(l -> {
                                if (l.getLanguageName()!=null) {
                                    String key = l.getLanguageName().trim().toLowerCase();
                                    if (!targetLanguageNames.contains(key)) missingLanguageNames.add(l.getLanguageName());
                                }
                            });
                        }
                    }
                    case BENEFIT_SCHEMES -> {
                        if (sv.getVoterBenefitSchemes() != null && !sv.getVoterBenefitSchemes().isEmpty()) {
                            if (!listsEqualSchemeIds(sv.getVoterBenefitSchemes(), tv.getVoterBenefitSchemes())) {
                                updatesPerField.merge(field, 1L, Long::sum);
                            }
                        }
                    }
                    case FEEDBACK -> {
                        if (!sv.getFeedbackIssues().isEmpty() && !feedbackSetsEqual(sv.getFeedbackIssues(), tv.getFeedbackIssues())) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        sv.getFeedbackIssues().forEach(fi -> {
                            if (fi.getIssueName()!=null) {
                                String key = fi.getIssueName().trim().toLowerCase();
                                if (!targetFeedbackIssueNames.contains(key)) missingFeedbackIssueNames.add(fi.getIssueName());
                            }
                        });
                    }
                    case VOTER_HISTORY -> {
                        if (!sv.getVoterHistories().isEmpty() && !historySetsEqual(sv.getVoterHistories(), tv.getVoterHistories())) {
                            updatesPerField.merge(field, 1L, Long::sum);
                        }
                        sv.getVoterHistories().forEach(h -> {
                            if (h.getVoterHistoryName()!=null) {
                                String key = h.getVoterHistoryName().trim().toLowerCase();
                                if (!targetHistoryNames.contains(key)) missingHistoryNames.add(h.getVoterHistoryName());
                            }
                        });
                    }

                    // Relationships
                    case FAMILY_MAPPING -> {
                        boolean familyChange = false;
                        if (sv.getFamilyId() != null && (tv.getFamilyId() == null || !sv.getFamilyId().equals(tv.getFamilyId()))) familyChange = true;
                        if (sv.getFamilyCount() != null && !sv.getFamilyCount().equals(tv.getFamilyCount())) familyChange = true;
                        if (sv.getFamilySequenceNumber() != null && !Objects.equals(sv.getFamilySequenceNumber(), tv.getFamilySequenceNumber())) familyChange = true;
                        if (sv.getFamilyDisplayPart() != null && !Objects.equals(sv.getFamilyDisplayPart(), tv.getFamilyDisplayPart())) familyChange = true;
                        if (sv.getIsFamilyHead() != null && !Objects.equals(sv.getIsFamilyHead(), tv.getIsFamilyHead())) familyChange = true;
                        if (familyChange) updatesPerField.merge(field, 1L, Long::sum);
                    }
                    case FRIENDS_MAPPING -> {
                        boolean friendsChange = false;
                        if (sv.getFriendId() != null && (tv.getFriendId() == null || !sv.getFriendId().equals(tv.getFriendId()))) friendsChange = true;
                        if (sv.getFriendCount() != null && !sv.getFriendCount().equals(tv.getFriendCount())) friendsChange = true;
                        if (sv.getFriendsDetails() != null && !Objects.equals(sv.getFriendsDetails(), tv.getFriendsDetails())) friendsChange = true;
                        if (friendsChange) updatesPerField.merge(field, 1L, Long::sum);
                    }
                    default -> {}
                }
            }
        }

        // Build fieldStats map
        updatesPerField.forEach((f,c) -> fieldStats.put(f.name(), Map.of("willUpdate", c)));
        if (!missingReligionNames.isEmpty()) fieldAvailability.put("RELIGION", Map.of("status","PARTIAL","missingNames", missingReligionNames));
        if (!missingCasteNames.isEmpty()) fieldAvailability.put("CASTE", Map.of("status","PARTIAL","missingNames", missingCasteNames));
        if (!missingSubCasteNames.isEmpty()) fieldAvailability.put("SUB_CASTE", Map.of("status","PARTIAL","missingNames", missingSubCasteNames));
        if (!missingCasteCategoryNames.isEmpty()) fieldAvailability.put("CASTE_CATEGORY", Map.of("status","PARTIAL","missingNames", missingCasteCategoryNames));
        if (!missingPartyNames.isEmpty()) fieldAvailability.put("PARTY", Map.of("status","PARTIAL","missingNames", missingPartyNames));
        if (!missingAvailabilityNames.isEmpty()) fieldAvailability.put("VOTER_CATEGORY", Map.of("status","PARTIAL","missingNames", missingAvailabilityNames));
        if (!missingLanguageNames.isEmpty()) fieldAvailability.put("LANGUAGE", Map.of("status","PARTIAL","missingNames", missingLanguageNames));
        if (!missingFeedbackIssueNames.isEmpty()) fieldAvailability.put("FEEDBACK", Map.of("status","PARTIAL","missingNames", missingFeedbackIssueNames));
        if (!missingHistoryNames.isEmpty()) fieldAvailability.put("VOTER_HISTORY", Map.of("status","PARTIAL","missingNames", missingHistoryNames));

        long votersAffected = updatesPerField.values().stream().mapToLong(Long::longValue).max().orElse(0L); // rough upper bound
        boolean hasMergeable = !requested.isEmpty(); // All fields are now mergeable

        return MergeDryRunResultDTO.builder()
                .dryRun(true)
                .sourceElectionId(request.getSourceElectionId())
                .targetElectionId(targetElectionId)
                .selectedFields(requested)
                .votersMatched(matched)
                .votersAffected(votersAffected)
                .missingEpicInTargetCount(missingInTarget)
                .missingEpicSample(missingEpicSample)
                .fieldStats(fieldStats)
                .fieldAvailability(fieldAvailability)
                .warnings(warnings)
                .canProceed(hasMergeable)
                .estimatedRuntimeSeconds(0)
                .generatedAt(Instant.now())
                .build();
    }

    @Transactional
    public UUID enqueueMerge(Long accountId, Long userId, Long targetElectionId, MergeRequestDTO request) {
        if (mergeJobRepository.existsByTargetElectionIdAndStatusIn(targetElectionId, EnumSet.of(MergeJobStatus.PENDING, MergeJobStatus.RUNNING))) {
            List<MergeJobEntity> activeJobs = mergeJobRepository.findByTargetElectionIdAndStatusIn(targetElectionId, EnumSet.of(MergeJobStatus.PENDING, MergeJobStatus.RUNNING));
            if (!activeJobs.isEmpty()) {
                MergeJobEntity blockingJob = activeJobs.get(0);
                String jobInfo = String.format("Job ID: %s, Status: %s, Started: %s", 
                    blockingJob.getId(), 
                    blockingJob.getStatus(),
                    blockingJob.getStartedAt() != null ? blockingJob.getStartedAt() : blockingJob.getCreatedAt());
                throw new ThedalException(ThedalError.MERGE_JOB_IN_PROGRESS, org.springframework.http.HttpStatus.CONFLICT, 
                    "A merge operation is already in progress for this election. " + jobInfo + ". Please wait for it to complete or cancel the job before starting a new merge.");
            }
        }
        MergeJobEntity job = new MergeJobEntity();
        job.setAccountId(accountId);
        job.setUserId(userId);
        job.setSourceElectionId(request.getSourceElectionId());
        job.setTargetElectionId(targetElectionId);
        job.setFields(request.getFields());
        job.setStatus(MergeJobStatus.PENDING);
        mergeJobRepository.save(job);
        log.info("Enqueued merge job {} for targetElection {}", job.getId(), targetElectionId);
        return job.getId();
    }

    // Call this after enqueueMerge returns, so the transaction has committed
    public void startAsyncProcessing(UUID jobId) {
        // Ensure we call through the Spring proxy so that @Async is honored
    self().runJobAsync(jobId);
    }

    // Call this after enqueueDryRun returns, so the transaction has committed
    public void startAsyncDryRun(UUID jobId) {
        self().runDryRunAsync(jobId);
    }

    // Force read from primary to avoid replica lag
    @Transactional
    public MergeJobEntity getJob(UUID jobId) {
        return mergeJobRepository.findWithFieldsById(jobId).orElseThrow(() ->
            new ThedalException(ThedalError.MERGE_JOB_NOT_FOUND, org.springframework.http.HttpStatus.NOT_FOUND, "Merge job not found. It may have been cancelled or deleted."));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markJobRunning(MergeJobEntity job) {
        job.setStatus(MergeJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        mergeJobRepository.save(job);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeJobSuccess(MergeJobEntity job, Map<String,Object> finalStats) {
        // Re-fetch to detach from outer transaction context
        MergeJobEntity freshJob = mergeJobRepository.findById(job.getId())
            .orElseThrow(() -> new ThedalException(ThedalError.MERGE_JOB_NOT_FOUND, 
                org.springframework.http.HttpStatus.NOT_FOUND, "Job not found"));
        freshJob.setStatus(MergeJobStatus.COMPLETED);
        freshJob.setFinishedAt(Instant.now());
        try { 
            String jsonStats = objectMapper.writeValueAsString(finalStats);
            freshJob.setResultStatsJson(jsonStats);
            log.info("Job {} completed with stats: {}", freshJob.getId(), jsonStats);
        } catch (JsonProcessingException e) { 
            log.error("Failed to serialize result stats for job {}", freshJob.getId(), e);
        }
        MergeJobEntity saved = mergeJobRepository.saveAndFlush(freshJob);
        entityManager.flush();
        entityManager.clear();
        log.info("Job {} status persisted: COMPLETED (DB status: {})", saved.getId(), saved.getStatus());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finalizeJobFailure(MergeJobEntity job, String error) {
        // Re-fetch to detach from outer transaction context
        MergeJobEntity freshJob = mergeJobRepository.findById(job.getId())
            .orElseThrow(() -> new ThedalException(ThedalError.MERGE_JOB_NOT_FOUND, 
                org.springframework.http.HttpStatus.NOT_FOUND, "Job not found"));
        freshJob.setStatus(MergeJobStatus.FAILED);
        freshJob.setFinishedAt(Instant.now());
        // Truncate error message to fit in VARCHAR(512) column
        String truncatedError = error != null && error.length() > 500 
            ? error.substring(0, 497) + "..." 
            : error;
        freshJob.setErrorMessage(truncatedError);
        mergeJobRepository.saveAndFlush(freshJob);
        entityManager.flush();
        entityManager.clear();
        log.info("Job {} status persisted: FAILED", freshJob.getId());
    }

    // Utility helpers ----------------------------------------------------
    private static String normalizeEpic(String epic) { return epic == null ? null : epic.trim().toUpperCase(); }
    private static boolean equalsIgnoreCase(String a, String b) { return a == null ? b == null : b != null && a.trim().equalsIgnoreCase(b.trim()); }

    private boolean religionNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return religionRepository.existsByReligionNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean casteNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return casteRepository.existsByCasteNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean subCasteNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return subCasteRepository.existsBySubCasteNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean casteCategoryNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return casteCategoryRepository.existsByCasteCategoryNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean partyNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return partyRepository.existsByPartyNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean availabilityNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return availabilityRepository.existsByAvailabilityNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean languageNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return languageRepository.existsByLanguageNameAndElectionIdAndAccountId(name, electionId, accountId);
    }
    private boolean feedbackIssueNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return feedbackIssueRepository.existsByIssueNameAndAccountIdAndElectionId(name, accountId, electionId);
    }
    private boolean historyNameExistsInTarget(String name, Long electionId, Long accountId) {
        if (name == null) return false; return voterHistoryRepository.findByVoterHistoryNameAndAccountIdAndElectionId(name, accountId, electionId).isPresent();
    }

    private void countIfDiff(Object src, Object dest, Map<MergeField, Long> counter, MergeField field) {
        if (src != null && (dest == null || !src.equals(dest))) counter.merge(field, 1L, Long::sum);
    }

    private boolean locationDiffers(VoterEntity s, VoterEntity t) {
        if (t == null) return true;
        return diff(s.getFullAddress(), t.getFullAddress()) || diff(s.getPincode(), t.getPincode()) || diff(s.getPartLati(), t.getPartLati()) || diff(s.getPartLong(), t.getPartLong()) || diff(s.getVoterLati(), t.getVoterLati()) || diff(s.getVoterLongi(), t.getVoterLongi());
    }
    private boolean diff(Object a, Object b) { return a != null && !a.equals(b); }

    private boolean languageSetsEqual(Set<?> a, Set<?> b) { if (a==null||b==null) return a==b; return a.size()==b.size(); }
    private boolean feedbackSetsEqual(Set<?> a, Set<?> b) { if (a==null||b==null) return a==b; return a.size()==b.size(); }
    private boolean historySetsEqual(Set<VoterHistoryEntity> a, Set<VoterHistoryEntity> b) { if (a==null||b==null) return a==b; return a.size()==b.size(); }

    // Async entrypoint
    @Async
    public void runJobAsync(UUID jobId) {
        Map<String, Object> finalStats = null;
        boolean success = false;
        String errorMsg = null;
        
        try {
            finalStats = self().executeJobAndReturnStats(jobId);
            success = true;
        } catch (Exception ex) {
            log.error("Async merge job {} crashed", jobId, ex);
            errorMsg = ex.getMessage();
        }
        
        // Finalize OUTSIDE the executeJob transaction to ensure it commits
        MergeJobEntity job = getJob(jobId);
        if (success && finalStats != null) {
            log.info("Finalizing job {} as SUCCESS", jobId);
            self().finalizeJobSuccess(job, finalStats);
            log.info("Job {} finalized successfully", jobId);
        } else {
            log.info("Finalizing job {} as FAILED: {}", jobId, errorMsg);
            self().finalizeJobFailure(job, errorMsg != null ? errorMsg : "Unknown error");
            log.info("Job {} finalized as failed", jobId);
        }
    }

    // Async entrypoint for dry-run
    @Async
    public void runDryRunAsync(UUID jobId) {
        Map<String, Object> finalStats = null;
        boolean success = false;
        String errorMsg = null;
        
        try {
            finalStats = self().executeDryRunJobAndReturnStats(jobId);
            success = true;
        } catch (Exception ex) {
            log.error("Async dry-run job {} crashed", jobId, ex);
            errorMsg = ex.getMessage();
        }
        
        // Finalize OUTSIDE the executeDryRunJob transaction
        MergeJobEntity job = getJob(jobId);
        if (success && finalStats != null) {
            log.info("Finalizing dry-run job {} as SUCCESS", jobId);
            self().finalizeJobSuccess(job, finalStats);
            log.info("Dry-run job {} finalized successfully", jobId);
        } else {
            log.info("Finalizing dry-run job {} as FAILED: {}", jobId, errorMsg);
            self().finalizeJobFailure(job, errorMsg != null ? errorMsg : "Unknown error");
            log.info("Dry-run job {} finalized as failed", jobId);
        }
    }

    // Core execution (transactional per batch via REQUIRES_NEW chunks not used yet; single Tx for now)
    // Returns stats map on success, throws on failure
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> executeJobAndReturnStats(UUID jobId) {
        MergeJobEntity job = getJob(jobId);
    if (job.getStatus() != MergeJobStatus.PENDING) return null;
    // Call through proxy so REQUIRES_NEW applies even within this class
    self().markJobRunning(job);
        Map<String,Object> finalStats = new HashMap<>();
        try {
            // Load voters for source & target (with eager loading of histories and feedback for merge operations)
            List<VoterEntity> sourceVoters = voterRepo.findByAccountIdAndElectionIdWithHistoriesAndFeedback(job.getAccountId(), job.getSourceElectionId());
            List<VoterEntity> targetVoters = voterRepo.findByAccountIdAndElectionIdWithHistoriesAndFeedback(job.getAccountId(), job.getTargetElectionId());
            Map<String, VoterEntity> targetByEpic = new HashMap<>();
            for (VoterEntity tv : targetVoters) {
                targetByEpic.put(normalizeEpic(tv.getEpicNumber()), tv);
            }

            // Pre-fetch target reference data into name->entity maps (lowercased keys)
            Map<String, com.thedal.thedal_app.settings.electionsettings.ReligionEntity> religionsByName = new HashMap<>();
            religionRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(r -> religionsByName.put(r.getReligionName()==null?null:r.getReligionName().trim().toLowerCase(), r));
            Map<String, com.thedal.thedal_app.settings.electionsettings.CasteEntity> castesByName = new HashMap<>();
            casteRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(c -> castesByName.put(c.getCasteName()==null?null:c.getCasteName().trim().toLowerCase(), c));
            Map<String, com.thedal.thedal_app.settings.electionsettings.SubCasteEntity> subCastesByName = new HashMap<>();
            subCasteRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(sc -> subCastesByName.put(sc.getSubCasteName()==null?null:sc.getSubCasteName().trim().toLowerCase(), sc));
            Map<String, com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity> casteCategoriesByName = new HashMap<>();
            casteCategoryRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(cc -> casteCategoriesByName.put(cc.getCasteCategoryName()==null?null:cc.getCasteCategoryName().trim().toLowerCase(), cc));
            Map<String, com.thedal.thedal_app.settings.electionsettings.Party> partiesByName = new HashMap<>();
            partyRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(p -> partiesByName.put(p.getPartyName()==null?null:p.getPartyName().trim().toLowerCase(), p));
            Map<String, com.thedal.thedal_app.settings.electionsettings.Availability> availabilityByName = new HashMap<>();
            availabilityRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(a -> availabilityByName.put(a.getAvailabilityName()==null?null:a.getAvailabilityName().trim().toLowerCase(), a));
            Map<String, com.thedal.thedal_app.settings.electionsettings.Language> languagesByName = new HashMap<>();
            languageRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(l -> languagesByName.put(l.getLanguageName()==null?null:l.getLanguageName().trim().toLowerCase(), l));
            Map<String, com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> feedbackIssuesByName = new HashMap<>();
            feedbackIssueRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(fi -> feedbackIssuesByName.put(fi.getIssueName()==null?null:fi.getIssueName().trim().toLowerCase(), fi));
            Map<String, VoterHistoryEntity> historiesByName = new HashMap<>();
            voterHistoryRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(h -> historiesByName.put(h.getVoterHistoryName()==null?null:h.getVoterHistoryName().trim().toLowerCase(), h));
            // Map: benefit scheme name -> BenefitSchemes
            Map<String, com.thedal.thedal_app.settings.electionsettings.BenefitSchemes> benefitSchemesByName = new HashMap<>();
            benefitSchemesRepository.findByAccountIdAndElectionId(job.getAccountId(), job.getTargetElectionId())
                .forEach(bs -> benefitSchemesByName.put(bs.getSchemeName()==null?null:bs.getSchemeName().trim().toLowerCase(), bs));

            long totalMatched = 0L;
            long updatedCount = 0L;
            Map<String,Long> fieldUpdateCounts = new HashMap<>();
            List<String> skippedMissingEpic = new ArrayList<>();
            // Audit enhancements
            Map<String, Set<String>> missingRefNames = new HashMap<>(); // type -> set of names
            List<String> updatedEpicSample = new ArrayList<>();
            final int UPDATED_EPIC_SAMPLE_LIMIT = 25;

            java.util.function.BiConsumer<String,String> recordMissing = (type, name) -> {
                if (name == null) return; // guard
                missingRefNames.computeIfAbsent(type, k -> new HashSet<>()).add(name);
            };
            job.setTotalVoters((long) sourceVoters.size());
            mergeJobRepository.saveAndFlush(job);
            log.info("Merge job {} starting: {} source voters, {} target voters, {} fields to merge", 
                job.getId(), sourceVoters.size(), targetVoters.size(), job.getFields().size());

            final int BATCH_SIZE = 1000; // Increased from 500
            final int LOG_INTERVAL = 500; // Log every 500 voters
            List<VoterEntity> batch = new ArrayList<>(BATCH_SIZE);
            int processedCount = 0;

            for (VoterEntity sv : sourceVoters) {
                String epic = normalizeEpic(sv.getEpicNumber());
                VoterEntity tv = targetByEpic.get(epic);
                if (tv == null) {
                    skippedMissingEpic.add(sv.getEpicNumber());
                    continue; // no creation
                }
                totalMatched++;
                boolean voterChanged = false;
                for (MergeField field : job.getFields()) {
                    switch (field) {
                        case MOBILE_NUMBER -> { if (copyScalar(sv.getMobileNo(), tv.getMobileNo(), v -> tv.setMobileNo((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case WHATSAPP_NUMBER -> { if (copyScalar(sv.getWhatsappNo(), tv.getWhatsappNo(), v -> tv.setWhatsappNo((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case DATE_OF_BIRTH -> { if (copyScalar(sv.getDob(), tv.getDob(), v -> tv.setDob((java.time.LocalDate)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case EMAIL_ID -> { if (copyScalar(sv.getEMail(), tv.getEMail(), v -> tv.setEMail((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case AADHAAR_NUMBER -> { if (copyScalar(sv.getAadhaarNumber(), tv.getAadhaarNumber(), v -> tv.setAadhaarNumber((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case PAN_NUMBER -> { if (copyScalar(sv.getPanNumber(), tv.getPanNumber(), v -> tv.setPanNumber((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case REMARKS -> { if (copyScalar(sv.getRemarks(), tv.getRemarks(), v -> tv.setRemarks((String)v))) voterChanged = inc(fieldUpdateCounts, field) || voterChanged; }
                        case RELIGION -> { 
                            if (sv.getReligion()!=null && sv.getReligion().getReligionName()!=null) { 
                                var key = sv.getReligion().getReligionName().trim().toLowerCase(); 
                                var targetRel = religionsByName.get(key); 
                                if (targetRel == null) {
                                    try {
                                        // Create missing religion in target election
                                        var newReligion = new com.thedal.thedal_app.settings.electionsettings.ReligionEntity();
                                        newReligion.setReligionName(sv.getReligion().getReligionName());
                                        newReligion.setAccountId(job.getAccountId());
                                        newReligion.setElectionId(job.getTargetElectionId());
                                        targetRel = religionRepository.save(newReligion);
                                        religionsByName.put(key, targetRel);
                                        log.debug("Created missing religion '{}' in target election {}", sv.getReligion().getReligionName(), job.getTargetElectionId());
                                    } catch (Exception e) {
                                        // If creation fails, try to find if it was created by another thread
                                        targetRel = religionRepository.findByReligionNameAndAccountIdAndElectionId(
                                            sv.getReligion().getReligionName(), job.getAccountId(), job.getTargetElectionId())
                                            .orElse(null);
                                        if (targetRel != null) {
                                            religionsByName.put(key, targetRel);
                                            log.debug("Found existing religion '{}' after failed creation in target election {}", sv.getReligion().getReligionName(), job.getTargetElectionId());
                                        } else {
                                            log.error("Failed to create or find religion '{}' in target election {}", sv.getReligion().getReligionName(), job.getTargetElectionId(), e);
                                        }
                                    }
                                }
                                if (targetRel!=null && (tv.getReligion()==null || !equalsIgnoreCase(tv.getReligion().getReligionName(), targetRel.getReligionName()))) { 
                                    tv.setReligion(targetRel); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case CASTE -> { 
                            if (sv.getCaste()!=null && sv.getCaste().getCasteName()!=null) { 
                                var key = sv.getCaste().getCasteName().trim().toLowerCase(); 
                                var targetCaste = castesByName.get(key); 
                                if (targetCaste == null) {
                                    // Ensure religion exists (use voter religion name if available)
                                    com.thedal.thedal_app.settings.electionsettings.ReligionEntity targetRel = null;
                                    String srcReligionName = sv.getReligion()!=null ? sv.getReligion().getReligionName() : null;
                                    if (srcReligionName != null) {
                                        var rKey = srcReligionName.trim().toLowerCase();
                                        targetRel = religionsByName.get(rKey);
                                        if (targetRel == null) {
                                            var newReligion = new com.thedal.thedal_app.settings.electionsettings.ReligionEntity();
                                            newReligion.setReligionName(srcReligionName);
                                            newReligion.setAccountId(job.getAccountId());
                                            newReligion.setElectionId(job.getTargetElectionId());
                                            targetRel = religionRepository.save(newReligion);
                                            religionsByName.put(rKey, targetRel);
                                            log.debug("Created missing religion '{}' in target election {} while creating caste", srcReligionName, job.getTargetElectionId());
                                        }
                                    }
                                    if (targetRel == null) {
                                        // Cannot create caste without religion (FK not-null). Record and skip.
                                        recordMissing.accept("CASTE", sv.getCaste().getCasteName());
                                        log.warn("Skipping creation of caste '{}' due to missing religion for target election {}", sv.getCaste().getCasteName(), job.getTargetElectionId());
                                    } else {
                                        // Create missing caste in target election with required religion
                                        var newCaste = new com.thedal.thedal_app.settings.electionsettings.CasteEntity();
                                        newCaste.setCasteName(sv.getCaste().getCasteName());
                                        newCaste.setAccountId(job.getAccountId());
                                        newCaste.setElectionId(job.getTargetElectionId());
                                        newCaste.setReligion(targetRel);
                                        targetCaste = casteRepository.save(newCaste);
                                        castesByName.put(key, targetCaste);
                                        log.debug("Created missing caste '{}' in target election {}", sv.getCaste().getCasteName(), job.getTargetElectionId());
                                    }
                                }
                                if (targetCaste!=null && (tv.getCaste()==null || !equalsIgnoreCase(tv.getCaste().getCasteName(), targetCaste.getCasteName()))) { 
                                    tv.setCaste(targetCaste); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case SUB_CASTE -> { 
                            if (sv.getSubCaste()!=null && sv.getSubCaste().getSubCasteName()!=null) { 
                                var key = sv.getSubCaste().getSubCasteName().trim().toLowerCase(); 
                                var targetSub = subCastesByName.get(key); 
                                if (targetSub == null) {
                                    // Ensure related caste exists (use voter's caste), and religion exists
                                    com.thedal.thedal_app.settings.electionsettings.CasteEntity targetCasteRef = null;
                                    com.thedal.thedal_app.settings.electionsettings.ReligionEntity targetRelForSub = null;
                                    if (sv.getCaste()!=null && sv.getCaste().getCasteName()!=null) {
                                        var casteKey = sv.getCaste().getCasteName().trim().toLowerCase();
                                        targetCasteRef = castesByName.get(casteKey);
                                        if (targetCasteRef == null) {
                                            // Ensure religion for caste
                                            String srcReligionName = sv.getReligion()!=null ? sv.getReligion().getReligionName() : null;
                                            if (srcReligionName != null) {
                                                var rKey = srcReligionName.trim().toLowerCase();
                                                targetRelForSub = religionsByName.get(rKey);
                                                if (targetRelForSub == null) {
                                                    var newReligion = new com.thedal.thedal_app.settings.electionsettings.ReligionEntity();
                                                    newReligion.setReligionName(srcReligionName);
                                                    newReligion.setAccountId(job.getAccountId());
                                                    newReligion.setElectionId(job.getTargetElectionId());
                                                    targetRelForSub = religionRepository.save(newReligion);
                                                    religionsByName.put(rKey, targetRelForSub);
                                                    log.debug("Created missing religion '{}' in target election {} while creating sub-caste", srcReligionName, job.getTargetElectionId());
                                                }
                                            }
                                            var newCaste = new com.thedal.thedal_app.settings.electionsettings.CasteEntity();
                                            newCaste.setCasteName(sv.getCaste().getCasteName());
                                            newCaste.setAccountId(job.getAccountId());
                                            newCaste.setElectionId(job.getTargetElectionId());
                                            if (targetRelForSub != null) newCaste.setReligion(targetRelForSub);
                                            targetCasteRef = casteRepository.save(newCaste);
                                            castesByName.put(casteKey, targetCasteRef);
                                            log.debug("Created missing caste '{}' in target election {} while creating sub-caste", sv.getCaste().getCasteName(), job.getTargetElectionId());
                                        }
                                    }
                                    // Create missing sub-caste in target election and link caste + religion
                                    if (targetCasteRef == null) {
                                        recordMissing.accept("SUB_CASTE", sv.getSubCaste().getSubCasteName());
                                        log.warn("Skipping creation of sub-caste '{}' due to missing caste for target election {}", sv.getSubCaste().getSubCasteName(), job.getTargetElectionId());
                                    } else {
                                        var newSubCaste = new com.thedal.thedal_app.settings.electionsettings.SubCasteEntity();
                                        newSubCaste.setSubCasteName(sv.getSubCaste().getSubCasteName());
                                        newSubCaste.setAccountId(job.getAccountId());
                                        newSubCaste.setElectionId(job.getTargetElectionId());
                                        newSubCaste.setCaste(targetCasteRef);
                                        // Set religion for sub-caste as well (use voter's religion if available; else use caste's religion)
                                        com.thedal.thedal_app.settings.electionsettings.ReligionEntity rel = null;
                                        if (sv.getReligion()!=null && sv.getReligion().getReligionName()!=null) {
                                            var rKey = sv.getReligion().getReligionName().trim().toLowerCase();
                                            rel = religionsByName.get(rKey);
                                            if (rel == null) {
                                                var newReligion = new com.thedal.thedal_app.settings.electionsettings.ReligionEntity();
                                                newReligion.setReligionName(sv.getReligion().getReligionName());
                                                newReligion.setAccountId(job.getAccountId());
                                                newReligion.setElectionId(job.getTargetElectionId());
                                                rel = religionRepository.save(newReligion);
                                                religionsByName.put(rKey, rel);
                                            }
                                        }
                                        if (rel == null && targetCasteRef.getReligion()!=null) {
                                            rel = targetCasteRef.getReligion();
                                        }
                                        if (rel == null) {
                                            recordMissing.accept("SUB_CASTE", sv.getSubCaste().getSubCasteName());
                                            log.warn("Skipping creation of sub-caste '{}' due to missing religion for target election {}", sv.getSubCaste().getSubCasteName(), job.getTargetElectionId());
                                        } else {
                                            newSubCaste.setReligion(rel);
                                            targetSub = subCasteRepository.save(newSubCaste);
                                            subCastesByName.put(key, targetSub);
                                            log.debug("Created missing sub-caste '{}' in target election {}", sv.getSubCaste().getSubCasteName(), job.getTargetElectionId());
                                        }
                                    }
                                }
                                if (targetSub!=null && (tv.getSubCaste()==null || !equalsIgnoreCase(tv.getSubCaste().getSubCasteName(), targetSub.getSubCasteName()))) { 
                                    tv.setSubCaste(targetSub); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case CASTE_CATEGORY -> { 
                            if (sv.getCasteCategory()!=null && sv.getCasteCategory().getCasteCategoryName()!=null) { 
                                var key = sv.getCasteCategory().getCasteCategoryName().trim().toLowerCase(); 
                                var targetCat = casteCategoriesByName.get(key); 
                                if (targetCat == null) {
                                    // Create missing caste category in target election
                                    var newCasteCategory = new com.thedal.thedal_app.settings.electionsettings.CasteCategoryEntity();
                                    newCasteCategory.setCasteCategoryName(sv.getCasteCategory().getCasteCategoryName());
                                    newCasteCategory.setAccountId(job.getAccountId());
                                    newCasteCategory.setElectionId(job.getTargetElectionId());
                                    targetCat = casteCategoryRepository.save(newCasteCategory);
                                    casteCategoriesByName.put(key, targetCat);
                                    log.debug("Created missing caste category '{}' in target election {}", sv.getCasteCategory().getCasteCategoryName(), job.getTargetElectionId());
                                }
                                if (targetCat!=null && (tv.getCasteCategory()==null || !equalsIgnoreCase(tv.getCasteCategory().getCasteCategoryName(), targetCat.getCasteCategoryName()))) { 
                                    tv.setCasteCategory(targetCat); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case PARTY -> { 
                            if (sv.getParty()!=null && sv.getParty().getPartyName()!=null) { 
                                var key = sv.getParty().getPartyName().trim().toLowerCase(); 
                                var targetParty = partiesByName.get(key); 
                                if (targetParty == null) {
                                    // Create missing party in target election
                                    var newParty = new com.thedal.thedal_app.settings.electionsettings.Party();
                                    newParty.setPartyName(sv.getParty().getPartyName());
                                    newParty.setAccountId(job.getAccountId());
                                    newParty.setElectionId(job.getTargetElectionId());
                                    if (sv.getParty().getPartyImage() != null) {
                                        newParty.setPartyImage(sv.getParty().getPartyImage());
                                    }
                                    targetParty = partyRepository.save(newParty);
                                    partiesByName.put(key, targetParty);
                                    log.debug("Created missing party '{}' in target election {}", sv.getParty().getPartyName(), job.getTargetElectionId());
                                }
                                if (targetParty!=null && (tv.getParty()==null || !equalsIgnoreCase(tv.getParty().getPartyName(), targetParty.getPartyName()))) { 
                                    tv.setParty(targetParty); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case VOTER_CATEGORY -> { 
                            if (sv.getAvailability1()!=null && sv.getAvailability1().getAvailabilityName()!=null) { 
                                var key = sv.getAvailability1().getAvailabilityName().trim().toLowerCase(); 
                                var targetAvail = availabilityByName.get(key); 
                                if (targetAvail == null) {
                                    // Create missing availability in target election - copy all required fields
                                    var sourceAvail = sv.getAvailability1();
                                    // Skip if required fields are null in source (data integrity issue)
                                    if (sourceAvail.getDescription() != null && sourceAvail.getCategoryName() != null) {
                                        var newAvailability = new com.thedal.thedal_app.settings.electionsettings.Availability();
                                        newAvailability.setAvailabilityName(sourceAvail.getAvailabilityName());
                                        newAvailability.setDescription(sourceAvail.getDescription());
                                        newAvailability.setCategoryName(sourceAvail.getCategoryName());
                                        newAvailability.setAvailabilityImage(sourceAvail.getAvailabilityImage());
                                        newAvailability.setOrderIndex(sourceAvail.getOrderIndex());
                                        newAvailability.setAccountId(job.getAccountId());
                                        newAvailability.setElectionId(job.getTargetElectionId());
                                        targetAvail = availabilityRepository.save(newAvailability);
                                        availabilityByName.put(key, targetAvail);
                                        log.debug("Created missing availability '{}' in target election {}", sourceAvail.getAvailabilityName(), job.getTargetElectionId());
                                    } else {
                                        log.warn("Skipping availability '{}' with null required fields (description={}, categoryName={}) for voter {}", 
                                            sourceAvail.getAvailabilityName(), sourceAvail.getDescription(), sourceAvail.getCategoryName(), sv.getEpicNumber());
                                        recordMissing.accept("Availability", sourceAvail.getAvailabilityName() + " (incomplete data)");
                                    }
                                }
                                if (targetAvail!=null && (tv.getAvailability1()==null || !equalsIgnoreCase(tv.getAvailability1().getAvailabilityName(), targetAvail.getAvailabilityName()))) { 
                                    tv.setAvailability1(targetAvail); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case LANGUAGE -> { 
                            if (!sv.getLanguages().isEmpty()) { 
                                // replace with intersection of resolvable languages
                                Set<com.thedal.thedal_app.settings.electionsettings.Language> newSet = new HashSet<>();
                                for (var lang : sv.getLanguages()) {
                                    if (lang.getLanguageName()!=null) {
                                        var key = lang.getLanguageName().trim().toLowerCase();
                                        var targetLang = languagesByName.get(key);
                                        if (targetLang == null) {
                                            // Create missing language in target election
                                            var newLanguage = new com.thedal.thedal_app.settings.electionsettings.Language();
                                            newLanguage.setLanguageName(lang.getLanguageName());
                                            newLanguage.setAccountId(job.getAccountId());
                                            newLanguage.setElectionId(job.getTargetElectionId());
                                            targetLang = languageRepository.save(newLanguage);
                                            languagesByName.put(key, targetLang);
                                            log.debug("Created missing language '{}' in target election {}", lang.getLanguageName(), job.getTargetElectionId());
                                        }
                                        if (targetLang != null) newSet.add(targetLang);
                                    }
                                }
                                if (!newSet.isEmpty() && !setsEqualIds(newSet, tv.getLanguages())) { 
                                    // Replace the entire collection to avoid lazy loading issues with existing PersistentSet
                                    tv.setLanguages(new HashSet<>(newSet)); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                }
                            } 
                        }
                        case BENEFIT_SCHEMES -> { 
                            if (sv.getVoterBenefitSchemes() != null && !sv.getVoterBenefitSchemes().isEmpty()) { 
                                List<VoterBenefitScheme> newList = new ArrayList<>(); 
                                for (var vbs : sv.getVoterBenefitSchemes()) { 
                                    if (vbs.getBenefitScheme() != null && vbs.getBenefitScheme().getSchemeName() != null) { 
                                        var key = vbs.getBenefitScheme().getSchemeName().trim().toLowerCase(); 
                                        var targetScheme = benefitSchemesByName.get(key); 
                                        if (targetScheme == null) {
                                            // Create missing benefit scheme in target election
                                            var newBenefitScheme = new com.thedal.thedal_app.settings.electionsettings.BenefitSchemes();
                                            newBenefitScheme.setSchemeName(vbs.getBenefitScheme().getSchemeName());
                                            newBenefitScheme.setAccountId(job.getAccountId());
                                            newBenefitScheme.setElectionId(job.getTargetElectionId());
                                            if (vbs.getBenefitScheme().getSchemeBy() != null) {
                                                newBenefitScheme.setSchemeBy(vbs.getBenefitScheme().getSchemeBy());
                                            }
                                            targetScheme = benefitSchemesRepository.save(newBenefitScheme);
                                            benefitSchemesByName.put(key, targetScheme);
                                            log.debug("Created missing benefit scheme '{}' in target election {}", vbs.getBenefitScheme().getSchemeName(), job.getTargetElectionId());
                                        }
                                        if (targetScheme != null) {
                                            var newVoterBenefitScheme = new VoterBenefitScheme();
                                            newVoterBenefitScheme.setVoter(tv);
                                            newVoterBenefitScheme.setBenefitScheme(targetScheme);
                                            newVoterBenefitScheme.setSelected(vbs.getSelected());
                                            newList.add(newVoterBenefitScheme);
                                        }
                                    } 
                                } 
                                if (!newList.isEmpty() && !listsEqualSchemeIds(newList, tv.getVoterBenefitSchemes())) { 
                                    // Replace the entire collection to avoid any potential lazy loading issues
                                    tv.setVoterBenefitSchemes(new ArrayList<>(newList)); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case FEEDBACK -> { 
                            if (!sv.getFeedbackIssues().isEmpty()) { 
                                List<com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> newList = new ArrayList<>(); 
                                for (var fi : sv.getFeedbackIssues()) { 
                                    if (fi.getIssueName()!=null) { 
                                        var key = fi.getIssueName().trim().toLowerCase(); 
                                        var targetFi = feedbackIssuesByName.get(key); 
                                        if (targetFi == null) {
                                            // Create missing feedback issue in target election
                                            var newFeedbackIssue = new com.thedal.thedal_app.settings.electionsettings.FeedbackIssue();
                                            newFeedbackIssue.setIssueName(fi.getIssueName());
                                            newFeedbackIssue.setAccountId(job.getAccountId());
                                            newFeedbackIssue.setElectionId(job.getTargetElectionId());
                                            targetFi = feedbackIssueRepository.save(newFeedbackIssue);
                                            feedbackIssuesByName.put(key, targetFi);
                                            log.debug("Created missing feedback issue '{}' in target election {}", fi.getIssueName(), job.getTargetElectionId());
                                        }
                                        if (targetFi != null) newList.add(targetFi);
                                    } 
                                } 
                                if (!newList.isEmpty() && !feedbackIssuesListEqualIds(newList, tv.getFeedbackIssues())) { 
                                    // Replace the entire collection to avoid lazy loading issues with existing PersistentSet
                                    tv.setFeedbackIssues(new HashSet<>(newList)); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case VOTER_HISTORY -> { 
                            if (!sv.getVoterHistories().isEmpty()) { 
                                Set<VoterHistoryEntity> newSet = new HashSet<>(); 
                                for (var h : sv.getVoterHistories()) { 
                                    if (h.getVoterHistoryName()!=null) { 
                                        var key = h.getVoterHistoryName().trim().toLowerCase(); 
                                        var targetHist = historiesByName.get(key); 
                                        if (targetHist == null) {
                                            // Create missing voter history in target election
                                            var newVoterHistory = new VoterHistoryEntity();
                                            newVoterHistory.setVoterHistoryName(h.getVoterHistoryName());
                                            // Ensure non-null to satisfy NOT NULL constraint in DB schema
                                            newVoterHistory.setVoterHistoryImage("");
                                            newVoterHistory.setAccountId(job.getAccountId());
                                            newVoterHistory.setElectionId(job.getTargetElectionId());
                                            targetHist = voterHistoryRepository.save(newVoterHistory);
                                            historiesByName.put(key, targetHist);
                                            log.debug("Created missing voter history '{}' in target election {}", h.getVoterHistoryName(), job.getTargetElectionId());
                                        }
                                        if (targetHist != null) newSet.add(targetHist);
                                    } 
                                } 
                                if (!newSet.isEmpty() && !setsEqualIds(newSet, tv.getVoterHistories())) { 
                                    // Replace the entire collection to avoid lazy loading issues with existing PersistentSet
                                    tv.setVoterHistories(new HashSet<>(newSet)); 
                                    voterChanged = inc(fieldUpdateCounts, field) || voterChanged; 
                                } 
                            } 
                        }
                        case FAMILY_MAPPING -> {
                            // Merge family data from source to target
                            boolean familyChanged = false;
                            if (sv.getFamilyId() != null && (tv.getFamilyId() == null || !sv.getFamilyId().equals(tv.getFamilyId()))) {
                                tv.setFamilyId(sv.getFamilyId());
                                familyChanged = true;
                            }
                            if (sv.getFamilyCount() != null && !sv.getFamilyCount().equals(tv.getFamilyCount())) {
                                tv.setFamilyCount(sv.getFamilyCount());
                                familyChanged = true;
                            }
                            if (sv.getFamilySequenceNumber() != null && !Objects.equals(sv.getFamilySequenceNumber(), tv.getFamilySequenceNumber())) {
                                tv.setFamilySequenceNumber(sv.getFamilySequenceNumber());
                                familyChanged = true;
                            }
                            if (sv.getFamilyDisplayPart() != null && !Objects.equals(sv.getFamilyDisplayPart(), tv.getFamilyDisplayPart())) {
                                tv.setFamilyDisplayPart(sv.getFamilyDisplayPart());
                                familyChanged = true;
                            }
                            if (sv.getIsFamilyHead() != null && !Objects.equals(sv.getIsFamilyHead(), tv.getIsFamilyHead())) {
                                tv.setIsFamilyHead(sv.getIsFamilyHead());
                                familyChanged = true;
                            }
                            if (familyChanged) voterChanged = inc(fieldUpdateCounts, field) || voterChanged;
                        }
                        case FRIENDS_MAPPING -> {
                            // Merge friends data from source to target
                            boolean friendsChanged = false;
                            if (sv.getFriendId() != null && (tv.getFriendId() == null || !sv.getFriendId().equals(tv.getFriendId()))) {
                                tv.setFriendId(sv.getFriendId());
                                friendsChanged = true;
                            }
                            if (sv.getFriendCount() != null && !sv.getFriendCount().equals(tv.getFriendCount())) {
                                tv.setFriendCount(sv.getFriendCount());
                                friendsChanged = true;
                            }
                            if (sv.getFriendsDetails() != null && !Objects.equals(sv.getFriendsDetails(), tv.getFriendsDetails())) {
                                tv.setFriendsDetails(sv.getFriendsDetails());
                                friendsChanged = true;
                            }
                            if (friendsChanged) voterChanged = inc(fieldUpdateCounts, field) || voterChanged;
                        }
                        case MEMBERSHIP_NUMBER -> {
                            // Treat partyRegistrationNumber as membership number and merge it
                            if (sv.getPartyRegistrationNumber() != null && (tv.getPartyRegistrationNumber() == null || !sv.getPartyRegistrationNumber().equals(tv.getPartyRegistrationNumber()))) {
                                tv.setPartyRegistrationNumber(sv.getPartyRegistrationNumber());
                                voterChanged = inc(fieldUpdateCounts, field) || voterChanged;
                            }
                        }
                    }
                }
                if (voterChanged) { updatedCount++; if (updatedEpicSample.size()<UPDATED_EPIC_SAMPLE_LIMIT) updatedEpicSample.add(sv.getEpicNumber()); batch.add(tv); }
                
                // Update processed count (only save to DB periodically)
                processedCount++;
                
                // Log progress less frequently
                if (processedCount % LOG_INTERVAL == 0) {
                    log.info("Merge job {}: Processed {}/{} voters ({} matched, {} updated so far)", 
                        job.getId(), processedCount, sourceVoters.size(), totalMatched, updatedCount);
                }
                
                // Save batch and clear persistence context
                if (batch.size() >= BATCH_SIZE) { 
                    voterRepo.saveAll(batch);
                    entityManager.flush();
                    entityManager.clear();
                    batch.clear();
                    
                    // Update job status after batch save
                    job.setProcessedVoters((long) processedCount);
                    mergeJobRepository.saveAndFlush(job);
                    
                    log.info("Merge job {}: Saved batch, {} voters updated, {}/{} processed", 
                        job.getId(), updatedCount, processedCount, sourceVoters.size());
                }
            }
            
            // Save final batch
            if (!batch.isEmpty()) { 
                voterRepo.saveAll(batch);
                entityManager.flush();
                entityManager.clear();
                batch.clear();
                log.info("Merge job {}: Saved final batch, {} total voters updated", job.getId(), updatedCount);
            }
            
            // Final status update
            job.setProcessedVoters((long) processedCount);
            mergeJobRepository.saveAndFlush(job);
            log.info("Merge job {}: Completed processing {} voters ({} matched, {} updated)", 
                job.getId(), processedCount, totalMatched, updatedCount);
            finalStats.put("totalSourceVoters", sourceVoters.size());
            finalStats.put("matchedVoters", totalMatched);
            finalStats.put("updatedVoters", updatedCount);
            finalStats.put("fieldUpdateCounts", fieldUpdateCounts);
            if (!skippedMissingEpic.isEmpty()) finalStats.put("missingEpicInTargetSample", skippedMissingEpic.subList(0, Math.min(25, skippedMissingEpic.size())));
            finalStats.put("missingEpicInTargetCount", skippedMissingEpic.size());
            // Build missing reference summaries
            if (!missingRefNames.isEmpty()) {
                Map<String, Integer> missingCounts = new HashMap<>();
                Map<String, List<String>> missingSamples = new HashMap<>();
                missingRefNames.forEach((type,set) -> {
                    missingCounts.put(type, set.size());
                    missingSamples.put(type, set.stream().limit(15).toList());
                });
                finalStats.put("missingReferenceCounts", missingCounts);
                finalStats.put("missingReferenceSamples", missingSamples);
                finalStats.put("missingReferenceTypes", missingRefNames.keySet());
            }
            finalStats.put("updatedEpicSample", updatedEpicSample);
            finalStats.put("unmodifiedMatchedVoters", totalMatched - updatedCount);
            if (job.getStartedAt()!=null) {
                finalStats.put("durationSeconds", Duration.between(job.getStartedAt(), Instant.now()).toSeconds());
            }
            
            return finalStats;
        } catch (Exception ex) {
            log.error("Merge job {} failed during execution", jobId, ex);
            throw ex; // Re-throw to be caught in runJobAsync
        }
    }

    // Core execution for dry-run (no DB mutations) - returns stats on success, throws on failure
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> executeDryRunJobAndReturnStats(UUID jobId) {
        MergeJobEntity job = getJob(jobId);
        if (job.getStatus() != MergeJobStatus.PENDING) return null;
        self().markJobRunning(job);
        
        try {
            MergeRequestDTO req = new MergeRequestDTO();
            req.setSourceElectionId(job.getSourceElectionId());
            req.setFields(job.getFields());
            req.setDryRun(true);

            var result = dryRun(job.getAccountId(), job.getUserId(), job.getTargetElectionId(), req);
            Map<String, Object> stats = Map.of("dryRun", result);
            log.info("Dry-run job {} completed execution", jobId);
            return stats;
        } catch (Exception ex) {
            log.error("Dry-run job {} failed during execution", jobId, ex);
            throw ex; // Re-throw to be caught in runDryRunAsync
        }
    }

    // Helper: copy scalar with overwrite rule (non-null source) & detect change
    private boolean copyScalar(Object src, Object dest, java.util.function.Consumer<Object> setter) {
        if (src == null) return false; if (dest == null || !src.equals(dest)) { setter.accept(src); return true; } return false;
    }
    private boolean copyLocation(VoterEntity s, VoterEntity t) {
        boolean changed = false;
        changed |= copyScalar(s.getFullAddress(), t.getFullAddress(), v -> t.setFullAddress((String)v));
        changed |= copyScalar(s.getPincode(), t.getPincode(), v -> t.setPincode((String)v));
        changed |= copyScalar(s.getPartLati(), t.getPartLati(), v -> t.setPartLati((Double)v));
        changed |= copyScalar(s.getPartLong(), t.getPartLong(), v -> t.setPartLong((Double)v));
        changed |= copyScalar(s.getVoterLati(), t.getVoterLati(), v -> t.setVoterLati((Double)v));
        changed |= copyScalar(s.getVoterLongi(), t.getVoterLongi(), v -> t.setVoterLongi((Double)v));
        return changed;
    }
    private boolean setsEqualIds(Set<?> a, Set<?> b) { if (a.size()!=b.size()) return false; // shallow size quick check
        return a.containsAll(b) && b.containsAll(a); }
    
    private boolean listsEqualSchemeIds(List<VoterBenefitScheme> a, List<VoterBenefitScheme> b) { 
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        Set<Long> aIds = a.stream().map(vbs -> vbs.getBenefitScheme() != null ? vbs.getBenefitScheme().getId() : null).collect(java.util.stream.Collectors.toSet());
        Set<Long> bIds = b.stream().map(vbs -> vbs.getBenefitScheme() != null ? vbs.getBenefitScheme().getId() : null).collect(java.util.stream.Collectors.toSet());
        return aIds.equals(bIds);
    }
    
    // Special method for FeedbackIssue comparison by IDs to avoid lazy loading issues
    private boolean feedbackIssuesSetsEqualIds(Set<com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> a, Set<com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        Set<Long> aIds = a.stream().map(fi -> fi.getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> bIds = b.stream().map(fi -> fi.getId()).collect(java.util.stream.Collectors.toSet());
        return aIds.equals(bIds);
    }
    
    // Compare List of FeedbackIssue with Set of FeedbackIssue by IDs to avoid lazy loading issues
    private boolean feedbackIssuesListEqualIds(List<com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> a, Set<com.thedal.thedal_app.settings.electionsettings.FeedbackIssue> b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        Set<Long> aIds = a.stream().map(fi -> fi.getId()).collect(java.util.stream.Collectors.toSet());
        Set<Long> bIds = b.stream().map(fi -> fi.getId()).collect(java.util.stream.Collectors.toSet());
        return aIds.equals(bIds);
    }
    
    private boolean inc(Map<String,Long> map, MergeField f) { 
        map.merge(f.name(),1L,Long::sum); 
        return true; // Return true to indicate that the voter was changed
    }
}
