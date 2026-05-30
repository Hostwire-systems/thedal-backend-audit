package com.thedal.thedal_app.voter;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;

import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.election.StaticFieldStatusService;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.Language;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VoterExcelDataRow {

	public static void populateDataRow(Row row, VoterEntity voter) {
        int colNum = 0;

        // Initial static fields (0-36)
        setCellValue(row, colNum++, voter.getPartNo());
        setCellValue(row, colNum++, voter.getSectionNo());
        setCellValue(row, colNum++, voter.getSerialNo());
        setCellValue(row, colNum++, voter.getHouseNoEn());
        setCellValue(row, colNum++, voter.getHouseNoL1());
        setCellValue(row, colNum++, voter.getHouseNoL2());
        setCellValue(row, colNum++, voter.getVoterFnameEn());
        setCellValue(row, colNum++, voter.getVoterLnameEn());
        setCellValue(row, colNum++, voter.getVoterFnameL1());
        setCellValue(row, colNum++, voter.getVoterLnameL1());
        setCellValue(row, colNum++, voter.getVoterFnameL2());
        setCellValue(row, colNum++, voter.getVoterLnameL2());
        setCellValue(row, colNum++, voter.getRlnFnameEn());
        setCellValue(row, colNum++, voter.getRlnLnameEn());
        setCellValue(row, colNum++, voter.getRlnFnameL1());
        setCellValue(row, colNum++, voter.getRlnLnameL1());
        setCellValue(row, colNum++, voter.getRlnFnameL2());
        setCellValue(row, colNum++, voter.getRlnLnameL2());
        setCellValue(row, colNum++, voter.getRlnType());
        setCellValue(row, colNum++, voter.getEpicNumber());
        setCellValue(row, colNum++, voter.getGender());
        setCellValue(row, colNum++, voter.getSectionNameEn());
        setCellValue(row, colNum++, voter.getSectionNameL1());
        setCellValue(row, colNum++, voter.getSectionNameL2());
        setCellValue(row, colNum++, voter.getFullAddress());
        setCellValue(row, colNum++, voter.getPartNameEn());
        setCellValue(row, colNum++, voter.getPartNameL1());
        setCellValue(row, colNum++, voter.getPartNameL2());
        setCellValue(row, colNum++, voter.getPincode());
        setCellValue(row, colNum++, voter.getPartLati());
        setCellValue(row, colNum++, voter.getPartLong());
        setCellValue(row, colNum++, voter.getAge());
        setCellValue(row, colNum++, voter.getDob());
        setCellValue(row, colNum++, voter.getMobileNo());
        setCellValue(row, colNum++, voter.getWhatsappNo());
        setCellValue(row, colNum++, voter.getEMail());
        setCellValue(row, colNum++, voter.getVoterLati());
        setCellValue(row, colNum++, voter.getVoterLongi());

        // Remaining static fields (37-63)
        setCellValue(row, colNum++, voter.getStateCode());
        setCellValue(row, colNum++, voter.getStateNameEn());
        setCellValue(row, colNum++, voter.getStateNameL1());
        setCellValue(row, colNum++, voter.getStateNameL2());
        setCellValue(row, colNum++, voter.getDistrictCode());
        setCellValue(row, colNum++, voter.getDistrictNameEn());
        setCellValue(row, colNum++, voter.getDistrictNameL1());
        setCellValue(row, colNum++, voter.getDistrictNameL2());
        setCellValue(row, colNum++, voter.getPcNo());
        setCellValue(row, colNum++, voter.getPcNameEn());
        setCellValue(row, colNum++, voter.getPcNameL1());
        setCellValue(row, colNum++, voter.getPcNameL2());
        setCellValue(row, colNum++, voter.getAcNo());
        setCellValue(row, colNum++, voter.getAcNameEn());
        setCellValue(row, colNum++, voter.getAcNameL1());
        setCellValue(row, colNum++, voter.getAcNameL2());
        setCellValue(row, colNum++, voter.getUrbanNo());
        setCellValue(row, colNum++, voter.getUrbanNameEn());
        setCellValue(row, colNum++, voter.getUrbanNameL1());
        setCellValue(row, colNum++, voter.getUrbanWardNo());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNo());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameEn());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameL1());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameL2());
        setCellValue(row, colNum++, voter.getRurDistrictUnionWardNo());
        setCellValue(row, colNum++, voter.getPanUnionNo());
        setCellValue(row, colNum++, voter.getPanUnionNameEn());
        setCellValue(row, colNum++, voter.getPanUnionNameL1());
        setCellValue(row, colNum++, voter.getPanUnionNameL2());
        setCellValue(row, colNum++, voter.getPanUnionWardNo());
        setCellValue(row, colNum++, voter.getVillPanNo());
        setCellValue(row, colNum++, voter.getVillPanNameEn());
        setCellValue(row, colNum++, voter.getVillPanNameL1());
        setCellValue(row, colNum++, voter.getVillPanWardNo());

        // Dynamic fields (64-73)
        setCellValue(row, colNum++, voter.getReligion() != null ? voter.getReligion().getReligionName() : "");
        setCellValue(row, colNum++, voter.getCaste() != null ? voter.getCaste().getCasteName() : "");
        setCellValue(row, colNum++, voter.getSubCaste() != null ? voter.getSubCaste().getSubCasteName() : "");
        setCellValue(row, colNum++, voter.getLanguages() != null ? 
            voter.getLanguages().stream().map(Language::getLanguageName).collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, voter.getVoterBenefitSchemes() != null ? 
                voter.getVoterBenefitSchemes().stream()
                    .map(vbs -> vbs.getBenefitScheme() != null ? vbs.getBenefitScheme().getSchemeName() : "")
                    .collect(Collectors.joining(", ")) : "");
            setCellValue(row, colNum++, voter.getVoterBenefitSchemes() != null ? 
                voter.getVoterBenefitSchemes().stream()
                    .map(vbs -> vbs.getBenefitScheme() != null && vbs.getBenefitScheme().getSchemeBy() != null ? 
                        vbs.getBenefitScheme().getSchemeBy().toString() : "")
                    .collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, voter.getScheme());
        setCellValue(row, colNum++, voter.getAvailability1() != null ? voter.getAvailability1().getDescription() : "");
        setCellValue(row, colNum++, voter.getAvailability1() != null ? voter.getAvailability1().getCategoryName() : "");
        setCellValue(row, colNum++, voter.getParty() != null ? voter.getParty().getPartyName() : "");
        setCellValue(row, colNum++, voter.getParty() != null ? voter.getParty().getPartyShortName() : "");
        setCellValue(row, colNum++, voter.getFamilyId() != null ? voter.getFamilyId().toString() : "");
        setCellValue(row, colNum++, voter.getFamilyCount() != null ? voter.getFamilyCount() : 1);
        
        setCellValue(row, colNum++, voter.getFeedbackIssues() != null ?
                voter.getFeedbackIssues().stream().map(FeedbackIssue::getIssueName).collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, voter.getVoterHistories() != null ?
                voter.getVoterHistories().stream().map(VoterHistoryEntity::getVoterHistoryName).collect(Collectors.joining(", ")) : "");
        
        setCellValue(row, colNum++, voter.getStarNumber());
        setCellValue(row, colNum++, voter.getAadhaarNumber());
        setCellValue(row, colNum++, voter.getPanNumber());
        setCellValue(row, colNum++, voter.getPartyRegistrationNumber());
        setCellValue(row, colNum++, voter.getPageNumber());
        setCellValue(row, colNum++, voter.getRemarks());
        setCellValue(row, colNum++, voter.getCasteCategory() != null ? voter.getCasteCategory().getCasteCategoryName() : null);
        setCellValue(row, colNum++, voter.getAadhaarVerified());
        setCellValue(row, colNum++, voter.getPhotoUrl());
        setCellValue(row, colNum++, voter.getFriendCount());
    }

    /**
     * Enhanced method to populate data row respecting static field status configuration
     */
    public static void populateDataRowWithFieldStatus(Row row, VoterEntity voter, Long accountId, Long electionId, StaticFieldStatusService staticFieldStatusService) {
        int colNum = 0;

        // Get enabled fields for this election
        List<String> enabledFields = staticFieldStatusService.getEnabledFieldNames(accountId, electionId);

        // Basic fields
        if (enabledFields.contains("partNo")) setCellValue(row, colNum++, voter.getPartNo());
        if (enabledFields.contains("sectionNo")) setCellValue(row, colNum++, voter.getSectionNo());
        if (enabledFields.contains("serialNo")) setCellValue(row, colNum++, voter.getSerialNo());
        if (enabledFields.contains("epicNumber")) setCellValue(row, colNum++, voter.getEpicNumber());
        if (enabledFields.contains("gender")) setCellValue(row, colNum++, voter.getGender());
        if (enabledFields.contains("age")) setCellValue(row, colNum++, voter.getAge());
        if (enabledFields.contains("dob")) setCellValue(row, colNum++, voter.getDob());

        // Name fields
        if (enabledFields.contains("voterFnameEn")) setCellValue(row, colNum++, voter.getVoterFnameEn());
        if (enabledFields.contains("voterLnameEn")) setCellValue(row, colNum++, voter.getVoterLnameEn());
        if (enabledFields.contains("voterFnameL1")) setCellValue(row, colNum++, voter.getVoterFnameL1());
        if (enabledFields.contains("voterLnameL1")) setCellValue(row, colNum++, voter.getVoterLnameL1());
        if (enabledFields.contains("voterFnameL2")) setCellValue(row, colNum++, voter.getVoterFnameL2());
        if (enabledFields.contains("voterLnameL2")) setCellValue(row, colNum++, voter.getVoterLnameL2());

        // Relation fields
        if (enabledFields.contains("rlnType")) setCellValue(row, colNum++, voter.getRlnType());
        if (enabledFields.contains("rlnFnameEn")) setCellValue(row, colNum++, voter.getRlnFnameEn());
        if (enabledFields.contains("rlnLnameEn")) setCellValue(row, colNum++, voter.getRlnLnameEn());
        if (enabledFields.contains("rlnFnameL1")) setCellValue(row, colNum++, voter.getRlnFnameL1());
        if (enabledFields.contains("rlnLnameL1")) setCellValue(row, colNum++, voter.getRlnLnameL1());
        if (enabledFields.contains("rlnFnameL2")) setCellValue(row, colNum++, voter.getRlnFnameL2());
        if (enabledFields.contains("rlnLnameL2")) setCellValue(row, colNum++, voter.getRlnLnameL2());

        // Address fields
        if (enabledFields.contains("houseNoEn")) setCellValue(row, colNum++, voter.getHouseNoEn());
        if (enabledFields.contains("houseNoL1")) setCellValue(row, colNum++, voter.getHouseNoL1());
        if (enabledFields.contains("houseNoL2")) setCellValue(row, colNum++, voter.getHouseNoL2());
        if (enabledFields.contains("fullAddress")) setCellValue(row, colNum++, voter.getFullAddress());
        if (enabledFields.contains("pincode")) setCellValue(row, colNum++, voter.getPincode());

        // Contact fields
        if (enabledFields.contains("mobileNo")) setCellValue(row, colNum++, voter.getMobileNo());
        if (enabledFields.contains("whatsappNo")) setCellValue(row, colNum++, voter.getWhatsappNo());
        if (enabledFields.contains("eMail")) setCellValue(row, colNum++, voter.getEMail());

        // Geographic fields
        if (enabledFields.contains("partLati")) setCellValue(row, colNum++, voter.getPartLati());
        if (enabledFields.contains("partLong")) setCellValue(row, colNum++, voter.getPartLong());
        if (enabledFields.contains("voterLati")) setCellValue(row, colNum++, voter.getVoterLati());
        if (enabledFields.contains("voterLongi")) setCellValue(row, colNum++, voter.getVoterLongi());

        // Section fields
        if (enabledFields.contains("sectionNameEn")) setCellValue(row, colNum++, voter.getSectionNameEn());
        if (enabledFields.contains("sectionNameL1")) setCellValue(row, colNum++, voter.getSectionNameL1());
        if (enabledFields.contains("sectionNameL2")) setCellValue(row, colNum++, voter.getSectionNameL2());

        // Part fields
        if (enabledFields.contains("partNameEn")) setCellValue(row, colNum++, voter.getPartNameEn());
        if (enabledFields.contains("partNameL1")) setCellValue(row, colNum++, voter.getPartNameL1());
        if (enabledFields.contains("partNameL2")) setCellValue(row, colNum++, voter.getPartNameL2());

        // State fields
        if (enabledFields.contains("stateCode")) setCellValue(row, colNum++, voter.getStateCode());
        if (enabledFields.contains("stateNameEn")) setCellValue(row, colNum++, voter.getStateNameEn());
        if (enabledFields.contains("stateNameL1")) setCellValue(row, colNum++, voter.getStateNameL1());
        if (enabledFields.contains("stateNameL2")) setCellValue(row, colNum++, voter.getStateNameL2());

        // District fields
        if (enabledFields.contains("districtCode")) setCellValue(row, colNum++, voter.getDistrictCode());
        if (enabledFields.contains("districtNameEn")) setCellValue(row, colNum++, voter.getDistrictNameEn());
        if (enabledFields.contains("districtNameL1")) setCellValue(row, colNum++, voter.getDistrictNameL1());
        if (enabledFields.contains("districtNameL2")) setCellValue(row, colNum++, voter.getDistrictNameL2());

        // PC fields
        if (enabledFields.contains("pcNo")) setCellValue(row, colNum++, voter.getPcNo());
        if (enabledFields.contains("pcNameEn")) setCellValue(row, colNum++, voter.getPcNameEn());
        if (enabledFields.contains("pcNameL1")) setCellValue(row, colNum++, voter.getPcNameL1());
        if (enabledFields.contains("pcNameL2")) setCellValue(row, colNum++, voter.getPcNameL2());

        // AC fields
        if (enabledFields.contains("acNo")) setCellValue(row, colNum++, voter.getAcNo());
        if (enabledFields.contains("acNameEn")) setCellValue(row, colNum++, voter.getAcNameEn());
        if (enabledFields.contains("acNameL1")) setCellValue(row, colNum++, voter.getAcNameL1());
        if (enabledFields.contains("acNameL2")) setCellValue(row, colNum++, voter.getAcNameL2());

        // Urban fields
        if (enabledFields.contains("urbanNo")) setCellValue(row, colNum++, voter.getUrbanNo());
        if (enabledFields.contains("urbanNameEn")) setCellValue(row, colNum++, voter.getUrbanNameEn());
        if (enabledFields.contains("urbanNameL1")) setCellValue(row, colNum++, voter.getUrbanNameL1());
        if (enabledFields.contains("urbanWardNo")) setCellValue(row, colNum++, voter.getUrbanWardNo());

        // Rural fields
        if (enabledFields.contains("rurDistrictUnionNo")) setCellValue(row, colNum++, voter.getRurDistrictUnionNo());
        if (enabledFields.contains("rurDistrictUnionNameEn")) setCellValue(row, colNum++, voter.getRurDistrictUnionNameEn());
        if (enabledFields.contains("rurDistrictUnionNameL1")) setCellValue(row, colNum++, voter.getRurDistrictUnionNameL1());
        if (enabledFields.contains("rurDistrictUnionNameL2")) setCellValue(row, colNum++, voter.getRurDistrictUnionNameL2());
        if (enabledFields.contains("rurDistrictUnionWardNo")) setCellValue(row, colNum++, voter.getRurDistrictUnionWardNo());

        // Panchayat fields
        if (enabledFields.contains("panUnionNo")) setCellValue(row, colNum++, voter.getPanUnionNo());
        if (enabledFields.contains("panUnionNameEn")) setCellValue(row, colNum++, voter.getPanUnionNameEn());
        if (enabledFields.contains("panUnionNameL1")) setCellValue(row, colNum++, voter.getPanUnionNameL1());
        if (enabledFields.contains("panUnionNameL2")) setCellValue(row, colNum++, voter.getPanUnionNameL2());
        if (enabledFields.contains("panUnionWardNo")) setCellValue(row, colNum++, voter.getPanUnionWardNo());

        // Village fields
        if (enabledFields.contains("villPanNo")) setCellValue(row, colNum++, voter.getVillPanNo());
        if (enabledFields.contains("villPanNameEn")) setCellValue(row, colNum++, voter.getVillPanNameEn());
        if (enabledFields.contains("villPanNameL1")) setCellValue(row, colNum++, voter.getVillPanNameL1());
        if (enabledFields.contains("villPanWardNo")) setCellValue(row, colNum++, voter.getVillPanWardNo());

        // Family fields
        if (enabledFields.contains("familyId")) setCellValue(row, colNum++, voter.getFamilyId() != null ? voter.getFamilyId().toString() : "");
        if (enabledFields.contains("familyCount")) setCellValue(row, colNum++, voter.getFamilyCount() != null ? voter.getFamilyCount() : 1);

        // Verification fields
        if (enabledFields.contains("mobileVerified")) setCellValue(row, colNum++, voter.getMobileVerified());
        if (enabledFields.contains("aadhaarVerified")) setCellValue(row, colNum++, voter.getAadhaarVerified());
        if (enabledFields.contains("memberVerified")) setCellValue(row, colNum++, voter.getMemberVerified());

        // Document fields
        if (enabledFields.contains("aadhaarNumber")) setCellValue(row, colNum++, voter.getAadhaarNumber());
        if (enabledFields.contains("panNumber")) setCellValue(row, colNum++, voter.getPanNumber());
        if (enabledFields.contains("partyRegistrationNumber")) setCellValue(row, colNum++, voter.getPartyRegistrationNumber());

        // Status fields
        if (enabledFields.contains("starNumber")) setCellValue(row, colNum++, voter.getStarNumber());
        if (enabledFields.contains("availability")) setCellValue(row, colNum++, voter.getAvailability());
        if (enabledFields.contains("partyAffiliation")) setCellValue(row, colNum++, voter.getPartyAffiliation());
        if (enabledFields.contains("scheme")) setCellValue(row, colNum++, voter.getScheme());

        // Additional fields
        if (enabledFields.contains("pageNumber")) setCellValue(row, colNum++, voter.getPageNumber());
        if (enabledFields.contains("remarks")) setCellValue(row, colNum++, voter.getRemarks());
        if (enabledFields.contains("photoUrl")) setCellValue(row, colNum++, voter.getPhotoUrl());
        if (enabledFields.contains("videoUrl")) setCellValue(row, colNum++, voter.getVideoUrl());

        // Always include dynamic and relational fields (they have their own management)
        setCellValue(row, colNum++, voter.getReligion() != null ? voter.getReligion().getReligionName() : "");
        setCellValue(row, colNum++, voter.getCaste() != null ? voter.getCaste().getCasteName() : "");
        setCellValue(row, colNum++, voter.getSubCaste() != null ? voter.getSubCaste().getSubCasteName() : "");
        setCellValue(row, colNum++, voter.getLanguages() != null ? 
            voter.getLanguages().stream().map(Language::getLanguageName).collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, voter.getAvailability1() != null ? voter.getAvailability1().getDescription() : "");
        setCellValue(row, colNum++, voter.getParty() != null ? voter.getParty().getPartyName() : "");
        setCellValue(row, colNum++, voter.getCasteCategory() != null ? voter.getCasteCategory().getCasteCategoryName() : "");
        setCellValue(row, colNum++, voter.getAadhaarVerified());
        setCellValue(row, colNum++, voter.getPhotoUrl());
        setCellValue(row, colNum++, voter.getFriendCount());
        setCellValue(row, colNum++, voter.getFeedbackIssues() != null ?
                voter.getFeedbackIssues().stream().map(FeedbackIssue::getIssueName).collect(Collectors.joining(", ")) : "");
        setCellValue(row, colNum++, voter.getVoterHistories() != null ?
                voter.getVoterHistories().stream().map(VoterHistoryEntity::getVoterHistoryName).collect(Collectors.joining(", ")) : "");
    }

    /**
	 * Populate Excel row from VoterExportProjection for optimized export
	 */
	public static void populateDataRowFromProjection(Row row, com.thedal.thedal_app.voter.dto.VoterExportProjection voter) {
        int colNum = 0;

        // Initial static fields (0-36)
        setCellValue(row, colNum++, voter.getPartNo());
        setCellValue(row, colNum++, voter.getSectionNo());
        setCellValue(row, colNum++, voter.getSerialNo());
        setCellValue(row, colNum++, voter.getHouseNoEn());
        setCellValue(row, colNum++, voter.getHouseNoL1());
        setCellValue(row, colNum++, voter.getHouseNoL2());
        setCellValue(row, colNum++, voter.getVoterFnameEn());
        setCellValue(row, colNum++, voter.getVoterLnameEn());
        setCellValue(row, colNum++, voter.getVoterFnameL1());
        setCellValue(row, colNum++, voter.getVoterLnameL1());
        setCellValue(row, colNum++, voter.getVoterFnameL2());
        setCellValue(row, colNum++, voter.getVoterLnameL2());
        setCellValue(row, colNum++, voter.getRlnFnameEn());
        setCellValue(row, colNum++, voter.getRlnLnameEn());
        setCellValue(row, colNum++, voter.getRlnFnameL1());
        setCellValue(row, colNum++, voter.getRlnLnameL1());
        setCellValue(row, colNum++, voter.getRlnFnameL2());
        setCellValue(row, colNum++, voter.getRlnLnameL2());
        setCellValue(row, colNum++, voter.getRlnType());
        setCellValue(row, colNum++, voter.getEpicNumber());
        setCellValue(row, colNum++, voter.getGender());
        setCellValue(row, colNum++, voter.getSectionNameEn());
        setCellValue(row, colNum++, voter.getSectionNameL1());
        setCellValue(row, colNum++, voter.getSectionNameL2());
        setCellValue(row, colNum++, voter.getFullAddress());
        setCellValue(row, colNum++, voter.getPartNameEn());
        setCellValue(row, colNum++, voter.getPartNameL1());
        setCellValue(row, colNum++, voter.getPartNameL2());
        setCellValue(row, colNum++, voter.getPincode());
        setCellValue(row, colNum++, voter.getPartLati());
        setCellValue(row, colNum++, voter.getPartLong());
        setCellValue(row, colNum++, voter.getAge());
        setCellValue(row, colNum++, voter.getDob());
        setCellValue(row, colNum++, voter.getMobileNo());
        setCellValue(row, colNum++, voter.getWhatsappNo());
        setCellValue(row, colNum++, voter.getEMail());
        setCellValue(row, colNum++, voter.getVoterLati());
        setCellValue(row, colNum++, voter.getVoterLongi());

        // State and location fields (37-64)
        setCellValue(row, colNum++, voter.getStateCode());
        setCellValue(row, colNum++, voter.getStateNameEn());
        setCellValue(row, colNum++, voter.getStateNameL1());
        setCellValue(row, colNum++, voter.getStateNameL2());
        setCellValue(row, colNum++, voter.getDistrictCode());
        setCellValue(row, colNum++, voter.getDistrictNameEn());
        setCellValue(row, colNum++, voter.getDistrictNameL1());
        setCellValue(row, colNum++, voter.getDistrictNameL2());
        setCellValue(row, colNum++, voter.getPcNo());
        setCellValue(row, colNum++, voter.getPcNameEn());
        setCellValue(row, colNum++, voter.getPcNameL1());
        setCellValue(row, colNum++, voter.getPcNameL2());
        setCellValue(row, colNum++, voter.getAcNo());
        setCellValue(row, colNum++, voter.getAcNameEn());
        setCellValue(row, colNum++, voter.getAcNameL1());
        setCellValue(row, colNum++, voter.getAcNameL2());
        setCellValue(row, colNum++, voter.getUrbanNo());
        setCellValue(row, colNum++, voter.getUrbanNameEn());
        setCellValue(row, colNum++, voter.getUrbanNameL1());
        setCellValue(row, colNum++, voter.getUrbanWardNo());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNo());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameEn());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameL1());
        setCellValue(row, colNum++, voter.getRurDistrictUnionNameL2());
        setCellValue(row, colNum++, voter.getRurDistrictUnionWardNo());
        setCellValue(row, colNum++, voter.getPanUnionNo());
        setCellValue(row, colNum++, voter.getPanUnionNameEn());
        setCellValue(row, colNum++, voter.getPanUnionNameL1());
        setCellValue(row, colNum++, voter.getPanUnionNameL2());
        setCellValue(row, colNum++, voter.getPanUnionWardNo());

        // Related entity fields (65-74)
        setCellValue(row, colNum++, voter.getReligionName());
        setCellValue(row, colNum++, voter.getCasteName());
        setCellValue(row, colNum++, voter.getSubCasteName());
        
        // Collection fields
        if (voter.getLanguages() != null) {
            setCellValue(row, colNum++, voter.getLanguages().stream()
                    .map(Language::getLanguageName)
                    .collect(Collectors.joining(", ")));
        } else {
            setCellValue(row, colNum++, "");
        }
        
        setCellValue(row, colNum++, voter.getScheme());
        setCellValue(row, colNum++, voter.getAvailabilityDescription());
        setCellValue(row, colNum++, voter.getAvailabilityCategoryName());
        setCellValue(row, colNum++, voter.getPartyName());
        setCellValue(row, colNum++, voter.getPartyShortName());
        setCellValue(row, colNum++, voter.getFamilyId());
        setCellValue(row, colNum++, voter.getFamilyCount());
        setCellValue(row, colNum++, voter.getStarNumber());
        setCellValue(row, colNum++, voter.getAadhaarNumber());
        setCellValue(row, colNum++, voter.getPanNumber());
        setCellValue(row, colNum++, voter.getPartyRegistrationNumber());
        setCellValue(row, colNum++, voter.getPageNumber());
        setCellValue(row, colNum++, voter.getRemarks());
        setCellValue(row, colNum++, voter.getCasteCategory());
        setCellValue(row, colNum++, voter.getAadhaarVerified());
        setCellValue(row, colNum++, voter.getPhotoUrl());
        setCellValue(row, colNum++, voter.getFriendCount());
    }

    private static void setCellValue(Row row, int colNum, Object value) {
        if (value != null) {
            if (value instanceof String) {
                row.createCell(colNum).setCellValue((String) value);
            } else if (value instanceof Integer) {
                row.createCell(colNum).setCellValue((Integer) value);
            } else if (value instanceof Long) {
                row.createCell(colNum).setCellValue((Long) value);
            } else if (value instanceof Double) {
                row.createCell(colNum).setCellValue((Double) value);
            } else if (value instanceof Boolean) {
                row.createCell(colNum).setCellValue((Boolean) value);
            } else if (value instanceof java.util.Date) {
                row.createCell(colNum).setCellValue((java.util.Date) value);
            } else {
                row.createCell(colNum).setCellValue(value.toString());
            }
        } else {
            row.createCell(colNum).setCellValue("");
        }
    }
    
    /**
     * Populate data row with only specified columns (selective export)
     * @param row The Excel row to populate
     * @param voter The voter entity containing data
     * @param columnFields List of field names to include (e.g., ["epicNumber", "voterFnameEn"])
     */
    public static void populateSelectiveDataRow(Row row, VoterEntity voter, List<String> columnFields) {
        if (columnFields == null || columnFields.isEmpty()) {
            // Fallback to all columns
            populateDataRow(row, voter);
            return;
        }
        
        int colNum = 0;
        for (String fieldName : columnFields) {
            Object value = VoterColumnMapper.getFieldValue(voter, fieldName);
            setCellValue(row, colNum++, value);
        }
    }
	
}