package com.thedal.thedal_app.voter.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;

import com.thedal.thedal_app.cpanel.dtos.VoterHistoryEntity;
import com.thedal.thedal_app.settings.electionsettings.BenefitSchemes;
import com.thedal.thedal_app.settings.electionsettings.FeedbackIssue;
import com.thedal.thedal_app.settings.electionsettings.Language;

public interface VoterExportProjection {
    // Core fields
    Integer getPartNo();
    Integer getSectionNo();
    Long getSerialNo();
    String getHouseNoEn();
    String getHouseNoL1();
    String getHouseNoL2();
    String getVoterFnameEn();
    String getVoterLnameEn();
    String getVoterFnameL1();
    String getVoterLnameL1();
    String getVoterFnameL2();
    String getVoterLnameL2();
    String getRlnFnameEn();
    String getRlnLnameEn();
    String getRlnFnameL1();
    String getRlnLnameL1();
    String getRlnFnameL2();
    String getRlnLnameL2();
    String getRlnType();
    String getEpicNumber();
    String getGender();
    String getSectionNameEn();
    String getSectionNameL1();
    String getSectionNameL2();
    String getFullAddress();
    String getPartNameEn();
    String getPartNameL1();
    String getPartNameL2();
    String getPincode();
    Double getPartLati();
    Double getPartLong();
    Integer getAge();
    LocalDate getDob();
    String getMobileNo();
    String getWhatsappNo();
    String getEMail();
    Double getVoterLati();
    Double getVoterLongi();
    // State and location fields
    String getStateCode();
    String getStateNameEn();
    String getStateNameL1();
    String getStateNameL2();
    String getDistrictCode();
    String getDistrictNameEn();
    String getDistrictNameL1();
    String getDistrictNameL2();
    String getPcNo();
    String getPcNameEn();
    String getPcNameL1();
    String getPcNameL2();
    String getAcNo();
    String getAcNameEn();
    String getAcNameL1();
    String getAcNameL2();
    String getUrbanNo();
    String getUrbanNameEn();
    String getUrbanNameL1();
    Integer getUrbanWardNo();
    String getRurDistrictUnionNo();
    String getRurDistrictUnionNameEn();
    String getRurDistrictUnionNameL1();
    String getRurDistrictUnionNameL2();
    String getRurDistrictUnionWardNo();
    String getPanUnionNo();
    String getPanUnionNameEn();
    String getPanUnionNameL1();
    String getPanUnionNameL2();
    String getPanUnionWardNo();
    String getVillPanNo();
    String getVillPanNameEn();
    String getVillPanNameL1();
    String getVillPanWardNo();
    // Related entity fields
    String getReligionName();
    String getCasteName();
    String getSubCasteName();
    Set<Language> getLanguages();
    List<BenefitSchemes> getBenefitSchemes();
    String getScheme();
    String getAvailabilityDescription();
    String getAvailabilityCategoryName();
    String getPartyName();
    String getPartyShortName();
    UUID getFamilyId();
    Integer getFamilyCount();
    Set<FeedbackIssue> getFeedbackIssues();
    Set<VoterHistoryEntity> getVoterHistories();
    Boolean getStarNumber();
    String getAadhaarNumber();
    String getPanNumber();
    String getPartyRegistrationNumber();
    Integer getPageNumber();
    String getRemarks();
    
    @Value("#{target.casteCategory?.casteCategoryName}")
    String getCasteCategory();
    
    Boolean getAadhaarVerified();
    String getPhotoUrl();
    Integer getFriendCount();
}