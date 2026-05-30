package com.thedal.thedal_app.campaign.service;

import com.thedal.thedal_app.campaign.dto.*;
import com.thedal.thedal_app.campaign.entity.CampaignEntity;
import com.thedal.thedal_app.campaign.repository.CampaignRepository;
import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;
import com.thedal.thedal_app.notification.SmsNotification;
import com.thedal.thedal_app.settings.electionsettings.*;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;
import com.thedal.thedal_app.voter.VoterSpecifications;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampaignService {
    // Database repository for persistent storage
    private final CampaignRepository campaignRepository;
    
    private final ReligionRepository religionRepository;
    private final CasteRepository casteRepository;
    private final SubCasteRepository subCasteRepository;
    private final CasteCategoryRepository casteCategoryRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PartManagerRepository partManagerRepository;
    private final SectionRepository sectionRepository;
    private final PartyRepository partyRepository;
    private final VoterRepo voterRepo;
    private final SmsNotification smsNotification;

    public CampaignService(
            CampaignRepository campaignRepository,
            ReligionRepository religionRepository,
            CasteRepository casteRepository,
            SubCasteRepository subCasteRepository,
            CasteCategoryRepository casteCategoryRepository,
            AvailabilityRepository availabilityRepository,
            PartManagerRepository partManagerRepository,
            SectionRepository sectionRepository,
            PartyRepository partyRepository,
            VoterRepo voterRepo,
            SmsNotification smsNotification  // Keep for content preparation, but SMS sending is mocked
    ) {
        this.campaignRepository = campaignRepository;
        this.religionRepository = religionRepository;
        this.casteRepository = casteRepository;
        this.subCasteRepository = subCasteRepository;
        this.casteCategoryRepository = casteCategoryRepository;
        this.availabilityRepository = availabilityRepository;
        this.partManagerRepository = partManagerRepository;
        this.sectionRepository = sectionRepository;
        this.partyRepository = partyRepository;
        this.voterRepo = voterRepo;
        this.smsNotification = smsNotification;  // Keep for content validation
    }

    // Helper methods for entity-DTO conversion
    private CampaignEntity toEntity(CampaignCreateRequest req, String id) {
        CampaignEntity entity = new CampaignEntity();
        entity.setId(id);
        entity.setChannel(req.getChannel());
        entity.setTitle(req.getTitle());
        entity.setSenderId(req.getSenderId());
        entity.setLanguage(req.getLanguage());
        
        // Handle content based on channel
        if ("sms".equalsIgnoreCase(req.getChannel())) {
            // For SMS: validate and clean content, ignore buttons/media
            String cleanContent = smsNotification.prepareSmsContent(req.getContentHtml());
            entity.setContentHtml(cleanContent);
            entity.setButtons(null); // SMS doesn't support buttons
            entity.setMedia(null);   // SMS doesn't support media
        } else {
            // For WhatsApp: keep original content
            entity.setContentHtml(req.getContentHtml());
            entity.setButtons(req.getButtons());
            entity.setMedia(req.getMedia());
        }
        
        entity.setTags(req.getTags());
        entity.setFilters(req.getFilters());
        entity.setStatus("draft");
        entity.setCreatedAt(OffsetDateTime.now());
        
        return entity;
    }

    private CampaignResponse toResponse(CampaignEntity entity) {
        CampaignResponse response = new CampaignResponse();
        response.setId(entity.getId());
        response.setChannel(entity.getChannel());
        response.setTitle(entity.getTitle());
        response.setSenderId(entity.getSenderId());
        response.setLanguage(entity.getLanguage());
        response.setContentHtml(entity.getContentHtml());
        response.setButtons(entity.getButtons());
        response.setMedia(entity.getMedia());
        response.setTags(entity.getTags());
        response.setFilters(entity.getFilters());
        response.setStatus(entity.getStatus());
        response.setRecipientsCount(entity.getRecipientsCount());
        response.setCreatedAt(entity.getCreatedAt());
        response.setScheduledAt(entity.getScheduledAt());
        
        return response;
    }

    public CampaignResponse create(CampaignCreateRequest req) {
        String id = UUID.randomUUID().toString();
        
        // Create entity and save to database
        CampaignEntity entity = toEntity(req, id);
        
        // Handle scheduling
        if (req.getSchedule() != null && req.getSchedule().getWhen() != null &&
                !"now".equalsIgnoreCase(req.getSchedule().getWhen())) {
            entity.setScheduledAt(OffsetDateTime.parse(req.getSchedule().getWhen()));
            entity.setStatus("scheduled");
        }
        
        CampaignEntity savedEntity = campaignRepository.save(entity);
        return toResponse(savedEntity);
    }

    public List<CampaignResponse> list(String channel, String status, String q) {
        try {
            // Try to use the native query method first
            List<CampaignEntity> entities = campaignRepository.findWithFilters(channel, status, q);
            return entities.stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Fallback to simple filtering if native query fails
            return listWithSimpleFiltering(channel, status, q);
        }
    }
    
    private List<CampaignResponse> listWithSimpleFiltering(String channel, String status, String q) {
        List<CampaignEntity> entities;
        
        // Get all campaigns first, then filter programmatically
        entities = campaignRepository.findAll();
        
        return entities.stream()
                .filter(entity -> channel == null || channel.equalsIgnoreCase(entity.getChannel()))
                .filter(entity -> status == null || status.equalsIgnoreCase(entity.getStatus()))
                .filter(entity -> q == null || q.isEmpty() || 
                        entity.getTitle() != null && entity.getTitle().toLowerCase().contains(q.toLowerCase()))
                .sorted((e1, e2) -> {
                    // Sort by created_at desc, handle nulls
                    if (e1.getCreatedAt() == null && e2.getCreatedAt() == null) return 0;
                    if (e1.getCreatedAt() == null) return 1;
                    if (e2.getCreatedAt() == null) return -1;
                    return e2.getCreatedAt().compareTo(e1.getCreatedAt());
                })
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CampaignResponse get(String id) {
        Optional<CampaignEntity> entity = campaignRepository.findById(id);
        return entity.map(this::toResponse).orElse(null);
    }

    public CampaignResponse update(String id, CampaignCreateRequest partial) {
        Optional<CampaignEntity> entityOpt = campaignRepository.findById(id);
        if (!entityOpt.isPresent()) return null;
        
        CampaignEntity entity = entityOpt.get();
        
        // Update fields if provided
        if (partial.getTitle() != null) entity.setTitle(partial.getTitle());
        if (partial.getContentHtml() != null) entity.setContentHtml(partial.getContentHtml());
        if (partial.getButtons() != null) entity.setButtons(partial.getButtons());
        if (partial.getMedia() != null) entity.setMedia(partial.getMedia());
        if (partial.getTags() != null) entity.setTags(partial.getTags());
        if (partial.getFilters() != null) entity.setFilters(partial.getFilters());
        
        CampaignEntity savedEntity = campaignRepository.save(entity);
        return toResponse(savedEntity);
    }

    public boolean delete(String id) {
        if (campaignRepository.existsById(id)) {
            campaignRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public CampaignResponse send(String id) {
        Optional<CampaignEntity> entityOpt = campaignRepository.findById(id);
        if (!entityOpt.isPresent()) return null;
        
        CampaignEntity entity = entityOpt.get();
        CampaignResponse response = toResponse(entity);
        
        // Set status to SENDING and save
        entity.setStatus("sending");
        campaignRepository.save(entity);
        response.setStatus("sending");
        
        if ("sms".equalsIgnoreCase(response.getChannel())) {
            return sendSmsCampaign(response, entity);
        } else if ("whatsapp".equalsIgnoreCase(response.getChannel())) {
            return sendWhatsAppCampaign(response, entity);
        } else {
            entity.setStatus("failed");
            campaignRepository.save(entity);
            response.setStatus("failed");
            return response;
        }
    }

    public EstimateResponse estimate(EstimateRequest req) {
        CampaignFilters f = req != null ? req.getFilters() : null;
        if (f == null || f.getElectionId() == null || f.getAccountId() == null) {
            return new EstimateResponse(0);
        }

        Specification<VoterEntity> spec = Specification
                .where(VoterSpecifications.hasElectionId(f.getElectionId()))
                .and(VoterSpecifications.hasAccountId(f.getAccountId()));

        // Sections take precedence over parts; sectionIds are in format "<part>-<section>"
        if (f.getSectionIds() != null && !f.getSectionIds().isEmpty()) {
            var pairs = f.getSectionIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(id -> id.split("-"))
                    .filter(arr -> arr.length == 2)
                    .map(arr -> new int[]{safeParseInt(arr[0]), safeParseInt(arr[1])})
                    .filter(p -> p[0] != Integer.MIN_VALUE && p[1] != Integer.MIN_VALUE)
                    .toList();
            if (!pairs.isEmpty()) {
                spec = spec.and((root, query, cb) -> {
                    var ors = new ArrayList<jakarta.persistence.criteria.Predicate>();
                    for (var p : pairs) {
                        ors.add(cb.and(
                                cb.equal(root.get("partNo"), p[0]),
                                cb.equal(root.get("sectionNo"), p[1])
                        ));
                    }
                    return cb.or(ors.toArray(new jakarta.persistence.criteria.Predicate[0]));
                });
            }
        } else if (f.getPartNos() != null && !f.getPartNos().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasPartNos(f.getPartNos()));
        }

        // Gender
        if (f.getGender() != null && !f.getGender().isBlank()) {
            spec = spec.and(VoterSpecifications.hasGender(f.getGender()));
        }

        // Age range parsing (e.g., "18-25" or "60+")
        Integer minAge = null;
        Integer maxAge = null;
        if (f.getAgeRange() != null && !f.getAgeRange().isBlank()) {
            String r = f.getAgeRange().trim();
            if (r.endsWith("+")) {
                minAge = safeParseInt(r.substring(0, r.length() - 1));
            } else if (r.contains("-")) {
                String[] parts = r.split("-");
                if (parts.length == 2) {
                    minAge = safeParseInt(parts[0]);
                    maxAge = safeParseInt(parts[1]);
                }
            }
        }
        if (minAge != null && minAge != Integer.MIN_VALUE) spec = spec.and(VoterSpecifications.hasMinAge(minAge));
        if (maxAge != null && maxAge != Integer.MIN_VALUE) spec = spec.and(VoterSpecifications.hasMaxAge(maxAge));

        // Multi-selects
        if (f.getReligionIds() != null && !f.getReligionIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasReligionIds(f.getReligionIds()));
        }
        if (f.getCasteIds() != null && !f.getCasteIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasCasteIds(f.getCasteIds()));
        }
        if (f.getSubCasteIds() != null && !f.getSubCasteIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasSubCasteIds(f.getSubCasteIds()));
        }
        if (f.getCasteCategoryIds() != null && !f.getCasteCategoryIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasCasteCategoryIds(f.getCasteCategoryIds()));
        }
        if (f.getAvailabilityIds() != null && !f.getAvailabilityIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasAvailabilityIds(f.getAvailabilityIds()));
        }
        if (f.getPartyIds() != null && !f.getPartyIds().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasPartyIds(f.getPartyIds()));
        }

        // Verification toggles
        if (f.getAadhaarVerified() != null) {
            spec = spec.and(VoterSpecifications.hasAadhaarVerified(f.getAadhaarVerified()));
        }
        if (f.getMembershipVerified() != null) {
            spec = spec.and(VoterSpecifications.hasMemberVerified(f.getMembershipVerified()));
        }

        // Poll status filter (voted/notVoted)
        if (f.getPollStatus() != null && !f.getPollStatus().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasPollStatus(f.getPollStatus()));
        }

        long count = voterRepo.count(spec);
        return new EstimateResponse(count);
    }

    public List<WhatsAppSender> listWhatsAppSenders() {
        return List.of(new WhatsAppSender("BJP-TN", "BJP Tamil Nadu"));
    }

    public List<SmsSender> listSmsSenders() {
        return List.of(
            new SmsSender("BJPTN", "BJP Tamil Nadu", "BJPTN"),
            new SmsSender("THEDAL", "Thedal Platform", "THEDAL")
        );
    }

    public FilterOptionsResponse getFilterOptions(Long electionId, Long accountId) {
    // Guard: if electionId/accountId missing, return empty options with static age/gender
    if (electionId == null || accountId == null) {
        return FilterOptionsResponse.builder()
            .parts(Collections.emptyList())
            .sectionsByPart(Collections.emptyMap())
            .districts(Collections.emptyList())
            .constituenciesByDistrict(Collections.emptyMap())
            .castes(Collections.emptyList())
            .subCastes(Collections.emptyList())
            .casteCategories(Collections.emptyList())
            .religions(Collections.emptyList())
            .parties(Collections.emptyList())
            .availabilities(Collections.emptyList())
            .ageRanges(defaultAgeRanges())
            .genders(defaultGenders())
            .tags(Collections.emptyList())
            .build();
    }

    // Parts (booths) and Sections
    // Prefer using SectionRepository to ensure numeric part/section ordering
    List<SectionEntity> sections = sectionRepository.findByElectionIdAndAccountId(electionId, accountId);
    // Unique part numbers
    List<Integer> parts = sections.stream()
        .map(SectionEntity::getPartNo)
        .filter(Objects::nonNull)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
    if (parts.isEmpty()) {
        // Fallback to PartManager if sections are not available
        parts = partManagerRepository.findByAccountIdAndElectionId(accountId, electionId).stream()
            .map(PartManager::getPartNo)
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> s.matches("\\d+"))
            .map(Integer::valueOf)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }

    // Sections grouped by part
    Map<Integer, List<FilterOptionsResponse.SimpleItem>> sectionsByPart = sections.stream()
        .filter(s -> s.getPartNo() != null && s.getSectionNo() != null)
        .sorted(Comparator.comparing(SectionEntity::getPartNo).thenComparing(SectionEntity::getSectionNo))
        .collect(Collectors.groupingBy(
            SectionEntity::getPartNo,
            LinkedHashMap::new,
            Collectors.mapping(s -> FilterOptionsResponse.SimpleItem.builder()
                    .id(s.getPartNo() + "-" + s.getSectionNo())
                    .name(s.getSectionNameEn() != null && !s.getSectionNameEn().isBlank()
                        ? s.getSectionNameEn()
                        : ("Section " + s.getSectionNo()))
                    .build(),
                Collectors.toList())));

    // Religions, Castes, SubCastes, Caste Categories, Availabilities, Parties
    List<FilterOptionsResponse.SimpleItem> religions = religionRepository
        .findByElectionIdAndAccountIdOrderByOrderIndex(electionId, accountId)
        .stream()
        .map(r -> simple(String.valueOf(r.getId()), r.getReligionName()))
        .collect(Collectors.toList());

    List<FilterOptionsResponse.SimpleItem> castes = casteRepository
        .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
        .stream()
        .map(c -> simple(String.valueOf(c.getId()), c.getCasteName()))
        .collect(Collectors.toList());

    List<FilterOptionsResponse.SimpleItem> subCastes = subCasteRepository
        .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
        .stream()
        .map(sc -> simple(String.valueOf(sc.getId()), sc.getSubCasteName()))
        .collect(Collectors.toList());

    List<FilterOptionsResponse.SimpleItem> casteCategories = casteCategoryRepository
        .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
        .stream()
        .map(cc -> simple(String.valueOf(cc.getId()), cc.getCasteCategoryName()))
        .collect(Collectors.toList());

    List<FilterOptionsResponse.SimpleItem> availabilities = availabilityRepository
        .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
        .stream()
        .map(a -> simple(String.valueOf(a.getId()), 
            a.getAvailabilityName() != null ? a.getAvailabilityName() : a.getDescription()))
        .collect(Collectors.toList());

    List<FilterOptionsResponse.SimpleItem> parties = partyRepository
        .findByAccountIdAndElectionIdOrderByOrderIndexAsc(accountId, electionId)
        .stream()
        .map(p -> simple(String.valueOf(p.getId()), p.getPartyName()))
        .collect(Collectors.toList());

    // Districts and constituencies are not modeled in current repos; return empty for now
    return FilterOptionsResponse.builder()
        .parts(parts)
        .sectionsByPart(sectionsByPart)
        .districts(Collections.emptyList())
        .constituenciesByDistrict(Collections.emptyMap())
        .castes(castes)
        .subCastes(subCastes)
        .casteCategories(casteCategories)
        .religions(religions)
        .parties(parties)
        .availabilities(availabilities)
        .ageRanges(defaultAgeRanges())
        .genders(defaultGenders())
        .tags(Collections.emptyList())
        .build();
    }

    private static FilterOptionsResponse.SimpleItem simple(String id, String name) {
    return FilterOptionsResponse.SimpleItem.builder().id(id).name(name).build();
    }

    private static List<FilterOptionsResponse.SimpleItem> defaultAgeRanges() {
    return List.of(
        FilterOptionsResponse.SimpleItem.builder().id("18-25").name("18-25 years").build(),
        FilterOptionsResponse.SimpleItem.builder().id("26-35").name("26-35 years").build(),
        FilterOptionsResponse.SimpleItem.builder().id("36-45").name("36-45 years").build(),
        FilterOptionsResponse.SimpleItem.builder().id("46-60").name("46-60 years").build(),
        FilterOptionsResponse.SimpleItem.builder().id("60+").name("60+ years").build()
    );
    }

    private static List<String> defaultGenders() {
    return List.of("male", "female", "other");
    }

    private static int safeParseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return Integer.MIN_VALUE;
        }
    }

    private CampaignResponse sendSmsCampaign(CampaignResponse campaign, CampaignEntity entity) {
        try {
            // Get recipients based on campaign filters
            List<String> mobileNumbers = getRecipientMobileNumbers(campaign);
            
            if (mobileNumbers.isEmpty()) {
                campaign.setStatus("failed");
                campaign.setRecipientsCount(0L);
                entity.setStatus("failed");
                entity.setRecipientsCount(0L);
                campaignRepository.save(entity);
                return campaign;
            }

            // Prepare SMS content (strip HTML, validate length)
            String smsContent = smsNotification.prepareSmsContent(campaign.getContentHtml());
            
            // Extract sender name from senderId (for SMS)
            String senderName = extractSmsFromSenderId(campaign.getSenderId());
            
            // TODO: DISABLE ACTUAL SMS SENDING FOR NOW - Just return success
            // smsNotification.sendBulkSms(mobileNumbers, smsContent, senderName)
            
            // Mock successful sending for now
            campaign.setStatus("sent");
            campaign.setRecipientsCount((long) mobileNumbers.size());
            
            // Update entity in database
            entity.setStatus("sent");
            entity.setRecipientsCount((long) mobileNumbers.size());
            campaignRepository.save(entity);
            
            return campaign;
            
            /* REAL SMS SENDING LOGIC (DISABLED FOR NOW):
            
            // Extract sender name from senderId (for SMS)
            String senderName = extractSmsFromSenderId(campaign.getSenderId());
            
            // Send bulk SMS asynchronously
            smsNotification.sendBulkSms(mobileNumbers, smsContent, senderName)
                .thenAccept(result -> {
                    // Update campaign status based on result
                    if (result.isSuccess()) {
                        campaign.setStatus("SENT");
                        campaign.setRecipientsCount((long) result.getSuccessCount());
                    } else {
                        campaign.setStatus("FAILED");
                        campaign.setRecipientsCount((long) result.getSuccessCount());
                    }
                })
                .exceptionally(throwable -> {
                    campaign.setStatus("FAILED");
                    return null;
                });

            // Set initial count and return (async processing will update later)
            campaign.setRecipientsCount((long) mobileNumbers.size());
            return campaign;
            
            */

        } catch (Exception e) {
            campaign.setStatus("failed");
            campaign.setRecipientsCount(0L);
            entity.setStatus("failed");
            entity.setRecipientsCount(0L);
            campaignRepository.save(entity);
            return campaign;
        }
    }

    private CampaignResponse sendWhatsAppCampaign(CampaignResponse campaign, CampaignEntity entity) {
        // Mock WhatsApp send: mark as sent and set recipientsCount to a fake estimate
        campaign.setStatus("sent");
        if (campaign.getRecipientsCount() == null) {
            campaign.setRecipientsCount(1800L);
        }
        
        // Update entity in database
        entity.setStatus("sent");
        if (entity.getRecipientsCount() == null) {
            entity.setRecipientsCount(1800L);
        }
        campaignRepository.save(entity);
        
        return campaign;
    }

    private List<String> getRecipientMobileNumbers(CampaignResponse campaign) {
        CampaignFilters f = campaign.getFilters();
        if (f == null || f.getElectionId() == null || f.getAccountId() == null) {
            return List.of(); // Return empty list if no valid filters
        }

        // Build specification from campaign filters
        Specification<VoterEntity> spec = Specification
                .where(VoterSpecifications.hasElectionId(f.getElectionId()))
                .and(VoterSpecifications.hasAccountId(f.getAccountId()));

        // Apply filters similar to estimate method
        if (f.getSectionIds() != null && !f.getSectionIds().isEmpty()) {
            var pairs = f.getSectionIds().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(id -> id.split("-"))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toList());

            if (!pairs.isEmpty()) {
                var partNos = pairs.stream().map(parts -> Integer.parseInt(parts[0])).collect(Collectors.toList());
                spec = spec.and(VoterSpecifications.hasPartNos(partNos));
                // Note: VoterSpecifications doesn't have section filtering, so we'll skip that for now
            }
        } else if (f.getPartNos() != null && !f.getPartNos().isEmpty()) {
            spec = spec.and(VoterSpecifications.hasPartNos(f.getPartNos()));
        }

        // Add other filters as needed...
        if (f.getGender() != null && !f.getGender().isBlank()) {
            spec = spec.and(VoterSpecifications.hasGender(f.getGender()));
        }

        // Get voters and extract mobile numbers
        List<VoterEntity> voters = voterRepo.findAll(spec);
        return voters.stream()
                .map(VoterEntity::getMobileNo)
                .filter(mobile -> mobile != null && !mobile.trim().isEmpty())
                .filter(mobile -> mobile.matches("\\d{10}")) // Basic validation for 10-digit numbers
                .collect(Collectors.toList());
    }

    private String extractSmsFromSenderId(String senderId) {
        if (senderId == null) return "THEDAL";
        
        // Extract SMS sender name from the campaign's senderId
        // This should match the SmsSender.senderName field
        SmsSender sender = listSmsSenders().stream()
            .filter(s -> s.getId().equals(senderId))
            .findFirst()
            .orElse(null);
            
        return sender != null ? sender.getSenderName() : "THEDAL";
    }

}
