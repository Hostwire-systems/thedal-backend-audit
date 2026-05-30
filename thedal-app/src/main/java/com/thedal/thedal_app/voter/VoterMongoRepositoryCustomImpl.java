package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class VoterMongoRepositoryCustomImpl implements VoterMongoRepositoryCustom {
    @Autowired
    private MongoTemplate mongoTemplate;

    
    public void saveVoterMongoWithNullFields(VoterMongo voterMongo) {
        // Check if voter already exists in MongoDB to prevent duplicates
        Document existingDoc = mongoTemplate.findOne(
            Query.query(Criteria.where("accountId").is(voterMongo.getAccountId())
                       .and("electionId").is(voterMongo.getElectionId())
                       .and("epicNumber").is(voterMongo.getEpicNumber())), 
            Document.class, "_voters");
        
        Document doc = new Document();
        // Use existing _id if document exists, otherwise use the provided id
        doc.put("_id", existingDoc != null ? existingDoc.get("_id") : voterMongo.getId());
        doc.put("voterId", voterMongo.getVoterId());
        doc.put("religionId", voterMongo.getReligionId()); // Add religionId
        doc.put("casteId", voterMongo.getCasteId()); // Add casteId
        doc.put("subCasteId", voterMongo.getSubCasteId()); // Add subCasteId
        doc.put("casteCategoryId", voterMongo.getCasteCategoryId()); // Add casteCategoryId
        doc.put("availabilityId", voterMongo.getAvailabilityId()); // Add availabilityId
        doc.put("partyId", voterMongo.getPartyId()); // Add partyId
        doc.put("partManagerId", voterMongo.getPartManagerId()); 
        doc.put("familyId", voterMongo.getFamilyId()); // Explicitly include familyId
        doc.put("familyCount", voterMongo.getFamilyCount());
        doc.put("friendId", voterMongo.getFriendId());
        doc.put("friendCount", voterMongo.getFriendCount());
        doc.put("friendsDetails", voterMongo.getRawFriendsDetails());
        doc.put("photoUrl", voterMongo.getPhotoUrl());
        doc.put("accountId", voterMongo.getAccountId());
        doc.put("electionId", voterMongo.getElectionId());
        doc.put("boothNumber", voterMongo.getBoothNumber());
        doc.put("hasVoted", voterMongo.getHasVoted());
        doc.put("votedTimestamp", voterMongo.getVotedTimestamp());
        doc.put("createdTime", voterMongo.getCreatedTime());
        doc.put("modifiedTime", voterMongo.getModifiedTime());
        doc.put("partNo", voterMongo.getPartNo());
        doc.put("sectionNo", voterMongo.getSectionNo());
        doc.put("serialNo", voterMongo.getSerialNo());
        doc.put("houseNoEn", voterMongo.getHouseNoEn());
        doc.put("houseNoL1", voterMongo.getHouseNoL1());
        doc.put("houseNoL2", voterMongo.getHouseNoL2());
        doc.put("voterFnameEn", voterMongo.getVoterFnameEn());
        doc.put("voterLnameEn", voterMongo.getVoterLnameEn());
        doc.put("voterFnameL1", voterMongo.getVoterFnameL1());
        doc.put("voterFnameL2", voterMongo.getVoterFnameL2());
        doc.put("voterLnameL1", voterMongo.getVoterLnameL1());
        doc.put("voterLnameL2", voterMongo.getVoterLnameL2());
        doc.put("rlnType", voterMongo.getRlnType());
        doc.put("rlnFnameEn", voterMongo.getRlnFnameEn());
        doc.put("rlnLnameEn", voterMongo.getRlnLnameEn());
        doc.put("rlnFnameL1", voterMongo.getRlnFnameL1());
        doc.put("rlnFnameL2", voterMongo.getRlnFnameL2());
        doc.put("rlnLnameL1", voterMongo.getRlnLnameL1());
        doc.put("rlnLnameL2", voterMongo.getRlnLnameL2());
        doc.put("epicNumber", voterMongo.getEpicNumber());
        doc.put("gender", voterMongo.getGender());
        doc.put("sectionNameEn", voterMongo.getSectionNameEn());
        doc.put("sectionNameL1", voterMongo.getSectionNameL1());
        doc.put("sectionNameL2", voterMongo.getSectionNameL2());
        doc.put("fullAddress", voterMongo.getFullAddress());
        doc.put("partNameEn", voterMongo.getPartNameEn());
        doc.put("partNameL1", voterMongo.getPartNameL1());
        doc.put("partNameL2", voterMongo.getPartNameL2());
        doc.put("pincode", voterMongo.getPincode());
        doc.put("partLati", voterMongo.getPartLati());
        doc.put("partLong", voterMongo.getPartLong());
        doc.put("age", voterMongo.getAge());
        doc.put("dob", voterMongo.getDob());
        doc.put("mobileNo", voterMongo.getMobileNo());
        doc.put("whatsappNo", voterMongo.getWhatsappNo());
        doc.put("eMail", voterMongo.getEMail());
        doc.put("voterLati", voterMongo.getVoterLati());
        doc.put("voterLongi", voterMongo.getVoterLongi());
        doc.put("stateCode", voterMongo.getStateCode());
        doc.put("stateNameEn", voterMongo.getStateNameEn());
        doc.put("stateNameL1", voterMongo.getStateNameL1());
        doc.put("stateNameL2", voterMongo.getStateNameL2());
        doc.put("districtCode", voterMongo.getDistrictCode());
        doc.put("districtNameEn", voterMongo.getDistrictNameEn());
        doc.put("districtNameL1", voterMongo.getDistrictNameL1());
        doc.put("districtNameL2", voterMongo.getDistrictNameL2());
        doc.put("pcNo", voterMongo.getPcNo());
        doc.put("pcNameEn", voterMongo.getPcNameEn());
        doc.put("pcNameL1", voterMongo.getPcNameL1());
        doc.put("pcNameL2", voterMongo.getPcNameL2());
        doc.put("acNo", voterMongo.getAcNo());
        doc.put("acNameEn", voterMongo.getAcNameEn());
        doc.put("acNameL1", voterMongo.getAcNameL1());
        doc.put("acNameL2", voterMongo.getAcNameL2());
        doc.put("urbanNo", voterMongo.getUrbanNo());
        doc.put("urbanNameEn", voterMongo.getUrbanNameEn());
        doc.put("urbanNameL1", voterMongo.getUrbanNameL1());
        doc.put("urbanWardNo", voterMongo.getUrbanWardNo());
        doc.put("rurDistrictUnionNo", voterMongo.getRurDistrictUnionNo());
        doc.put("rurDistrictUnionNameEn", voterMongo.getRurDistrictUnionNameEn());
        doc.put("rurDistrictUnionNameL1", voterMongo.getRurDistrictUnionNameL1());
        doc.put("rurDistrictUnionNameL2", voterMongo.getRurDistrictUnionNameL2());
        doc.put("rurDistrictUnionWardNo", voterMongo.getRurDistrictUnionWardNo());
        doc.put("panUnionNo", voterMongo.getPanUnionNo());
        doc.put("panUnionNameEn", voterMongo.getPanUnionNameEn());
        doc.put("panUnionNameL1", voterMongo.getPanUnionNameL1());
        doc.put("panUnionNameL2", voterMongo.getPanUnionNameL2());
        doc.put("panUnionWardNo", voterMongo.getPanUnionWardNo());
        doc.put("villPanNo", voterMongo.getVillPanNo());
        doc.put("villPanNameEn", voterMongo.getVillPanNameEn());
        doc.put("villPanNameL1", voterMongo.getVillPanNameL1());
        doc.put("villPanWardNo", voterMongo.getVillPanWardNo());
        doc.put("availability", voterMongo.getAvailability());
        doc.put("partyAffiliation", voterMongo.getPartyAffiliation());
        doc.put("starNumber", voterMongo.getStarNumber());
        doc.put("aadhaarNumber", voterMongo.getAadhaarNumber());
        doc.put("panNumber", voterMongo.getPanNumber());
        doc.put("partyRegistrationNumber", voterMongo.getPartyRegistrationNumber());
        doc.put("dynamicFields", voterMongo.getDynamicFields());
        doc.put("languageIds", voterMongo.getLanguageIds());
       // doc.put("benefitSchemeIds", voterMongo.getBenefitSchemeIds());
     // Map voterBenefitSchemes to a list of documents
        doc.put("voterBenefitSchemes", voterMongo.getVoterBenefitSchemes() != null ? 
                voterMongo.getVoterBenefitSchemes().stream().map(vbs -> {
                    Document vbsDoc = new Document();
                    vbsDoc.put("id", vbs.getId());
                    vbsDoc.put("benefitSchemeId", vbs.getBenefitSchemeId());
                    vbsDoc.put("selected", vbs.getSelected());
                    return vbsDoc;
                }).collect(Collectors.toList()) : null);
        doc.put("scheme", voterMongo.getScheme());
        doc.put("pageNumber", voterMongo.getPageNumber());
        doc.put("remarks", voterMongo.getRemarks());
        doc.put("videoUrl", voterMongo.getVideoUrl());
        doc.put("otp", voterMongo.getOtp());
        doc.put("otpCreatedAt", voterMongo.getOtpCreatedAt());
        doc.put("mobileVerified", voterMongo.getMobileVerified());
        doc.put("aadhaarVerified", voterMongo.getAadhaarVerified());
        doc.put("memberVerified", voterMongo.getMemberVerified());
        doc.put("feedbackIssueIds", voterMongo.getFeedbackIssueIds());
        doc.put("voterHistoryIds", voterMongo.getVoterHistoryIds());
        doc.put("createdByUserId", voterMongo.getCreatedByUserId());
        doc.put("_class", voterMongo.getClass().getName());

        // Use upsert to prevent duplicates - replaces existing document with same _id
        mongoTemplate.save(doc, "_voters");
        log.debug("Upserted VoterMongo with _id: {}, familyId: {}, epicNumber: {}", 
                voterMongo.getId(), voterMongo.getFamilyId(), voterMongo.getEpicNumber());
    }

    /**
     * Optimized bulk upsert method for family mapping migration to prevent duplicates
     */
    public void bulkUpsertVoterMongoWithDeduplication(List<VoterMongo> voterMongoList) {
        if (voterMongoList == null || voterMongoList.isEmpty()) {
            return;
        }

        for (VoterMongo voterMongo : voterMongoList) {
            try {
                // Check if voter already exists to prevent duplicates
                Document existingDoc = mongoTemplate.findOne(
                    Query.query(Criteria.where("accountId").is(voterMongo.getAccountId())
                               .and("electionId").is(voterMongo.getElectionId())
                               .and("epicNumber").is(voterMongo.getEpicNumber())), 
                    Document.class, "_voters");

                if (existingDoc != null) {
                    // Update existing document with new family mapping data
                    Document updateDoc = new Document();
                    updateDoc.put("familyId", voterMongo.getFamilyId());
                    updateDoc.put("familyCount", voterMongo.getFamilyCount());
                    updateDoc.put("friendId", voterMongo.getFriendId());
                    updateDoc.put("friendCount", voterMongo.getFriendCount());
                    updateDoc.put("friendsDetails", voterMongo.getRawFriendsDetails());
                    updateDoc.put("modifiedTime", voterMongo.getModifiedTime());
                 // Update voterBenefitSchemes
                    updateDoc.put("voterBenefitSchemes", voterMongo.getVoterBenefitSchemes() != null ? 
                            voterMongo.getVoterBenefitSchemes().stream().map(vbs -> {
                                Document vbsDoc = new Document();
                                vbsDoc.put("id", vbs.getId());
                                vbsDoc.put("benefitSchemeId", vbs.getBenefitSchemeId());
                                vbsDoc.put("selected", vbs.getSelected());
                                return vbsDoc;
                            }).collect(Collectors.toList()) : null);
                    
                    mongoTemplate.updateFirst(
                        Query.query(Criteria.where("_id").is(existingDoc.get("_id"))), 
                        Update.fromDocument(updateDoc), "_voters");
                    
                    log.debug("Updated existing voter in MongoDB: epicNumber={}, familyId={}", 
                             voterMongo.getEpicNumber(), voterMongo.getFamilyId());
                } else {
                    // Create new document
                    saveVoterMongoWithNullFields(voterMongo);
                    log.debug("Created new voter in MongoDB: epicNumber={}, familyId={}", 
                             voterMongo.getEpicNumber(), voterMongo.getFamilyId());
                }
            } catch (Exception e) {
                log.error("Failed to upsert voter: epicNumber={}, familyId={}, error={}", 
                         voterMongo.getEpicNumber(), voterMongo.getFamilyId(), e.getMessage());
            }
        }
    }
}