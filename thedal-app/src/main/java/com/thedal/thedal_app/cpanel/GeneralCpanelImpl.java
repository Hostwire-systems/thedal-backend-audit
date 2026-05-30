package com.thedal.thedal_app.cpanel;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.cpanel.dtos.GeneralCpanelRequest;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.response.ThedalResponse;
import com.thedal.thedal_app.response.ThedalSuccess;
import com.thedal.thedal_app.role.RolePermission;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GeneralCpanelImpl implements GeneralCpanelService{
    private static final String TERMS_AND_CONDITIONS = "terms_of_service";
    private static final String PRIVACY_POLICY = "privacy_policy";
    private static final String FAQ = "faq";
    private static final String ABOUT = "about";

    private final GeneralCpanelRepository generalCpanelRepository;
    private final RequestDetailsService requestDetails;
    
    @Autowired
    public GeneralCpanelImpl(GeneralCpanelRepository generalCpanelRepository,
    		RequestDetailsService requestDetails) {
        this.generalCpanelRepository = generalCpanelRepository;
        this.requestDetails=requestDetails;
    }
    
    @Override
    @Transactional
    public ThedalResponse<Void> saveTnC(GeneralCpanelRequest request) {
    	log.info("Attempting to save profile for request: {}", request);
        requestDetails.checkUserRolePermission(RolePermission.SETTINGS_MANAGEMENT);

        if (request.getFaq() != null) {
            Optional<GeneralCpanelEntity> faq = generalCpanelRepository.findByCpanelName(FAQ);
            if (faq.isPresent()) {
                faq.get().setCpanelValue(request.getFaq());
                generalCpanelRepository.save(faq.get());
            } else {
                GeneralCpanelEntity newFaq = new GeneralCpanelEntity();
                newFaq.setCpanelName(FAQ);
                newFaq.setCpanelValue(request.getFaq());
                generalCpanelRepository.save(newFaq);
            }
        }
        if (request.getPrivacyPolicy() != null) {
            Optional<GeneralCpanelEntity> privacyPolicy = generalCpanelRepository.findByCpanelName(PRIVACY_POLICY);
            if (privacyPolicy.isPresent()) {
                privacyPolicy.get().setCpanelValue(request.getPrivacyPolicy());
                generalCpanelRepository.save(privacyPolicy.get());
            } else {
                GeneralCpanelEntity newPrivacyPolicy = new GeneralCpanelEntity();
                newPrivacyPolicy.setCpanelName(PRIVACY_POLICY);
                newPrivacyPolicy.setCpanelValue(request.getPrivacyPolicy());
                generalCpanelRepository.save(newPrivacyPolicy);
            }
        }

        if (request.getTermsAndConditions() != null) {
            Optional<GeneralCpanelEntity> termsAndConditions = generalCpanelRepository.findByCpanelName(TERMS_AND_CONDITIONS);
            if (termsAndConditions.isPresent()) {
                termsAndConditions.get().setCpanelValue(request.getTermsAndConditions());
                generalCpanelRepository.save(termsAndConditions.get());
            } else {
                GeneralCpanelEntity newTermsAndConditions = new GeneralCpanelEntity();
                newTermsAndConditions.setCpanelName(TERMS_AND_CONDITIONS);
                newTermsAndConditions.setCpanelValue(request.getTermsAndConditions());
                generalCpanelRepository.save(newTermsAndConditions);
            }
        }
        
     // New logic for About
        if (request.getAbout() != null) {
            Optional<GeneralCpanelEntity> about = generalCpanelRepository.findByCpanelName(ABOUT);
            if (about.isPresent()) {
                about.get().setCpanelValue(request.getAbout());
                generalCpanelRepository.save(about.get());
            } else {
                GeneralCpanelEntity newAbout = new GeneralCpanelEntity();
                newAbout.setCpanelName(ABOUT);
                newAbout.setCpanelValue(request.getAbout());
                generalCpanelRepository.save(newAbout);
            }
        }
        
        log.info("cpanel details updated successfully");
        return new ThedalResponse<>(ThedalSuccess.CPANEL_DETAILS_UPDATED);
    }

    @Override
    public ThedalResponse<String> getTnC() {
        log.info("Retrieving Tnc for current account.");
        
        Optional<GeneralCpanelEntity> tnc = generalCpanelRepository.findByCpanelName(TERMS_AND_CONDITIONS);
        //return the terms and conditions
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, tnc.get().getCpanelValue());
    }

    @Override
    public ThedalResponse<String> getPrivacyPolicy() {
        log.info("Retrieving Privacy Policy for current account.");
        
        Optional<GeneralCpanelEntity> privacyPolicy = generalCpanelRepository.findByCpanelName(PRIVACY_POLICY);
        //return the privacy policy
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, privacyPolicy.get().getCpanelValue());
    }

    @Override
    public ThedalResponse<String> getFaq() {
        log.info("Retrieving FAQ for current account.");
        
        Optional<GeneralCpanelEntity> faq = generalCpanelRepository.findByCpanelName(FAQ);
        //return the faq
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, faq.get().getCpanelValue());
    }

    @Override
    public ThedalResponse<String> getAbout() {
    log.info("Retrieving About content for current account.");
    
    Optional<GeneralCpanelEntity> about = generalCpanelRepository.findByCpanelName(ABOUT);
    
    
        return new ThedalResponse<>(ThedalSuccess.SUCCESS, about.get().getCpanelValue());
   
    }

    



    
}

