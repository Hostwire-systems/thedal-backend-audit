package com.thedal.thedal_app.voter;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionEntity;
import com.thedal.thedal_app.settings.electionsettings.SectionRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SectionVoterService {
	
	@Autowired
    private SectionRepository sectionRepository;

    public SectionEntity saveSection(Long electionId, Integer partNo, Integer sectionNo, String sectionNameEn, String sectionNameL1, Long accountId) {
        // Check if the section entry already exists for this election, partNo, sectionNo, and accountId
        Optional<SectionEntity> existingSection = sectionRepository.findByElectionIdAndPartNoAndSectionNoAndAccountId(
            electionId, partNo, sectionNo, accountId);

        if (!existingSection.isPresent()) {
            SectionEntity section = new SectionEntity();
            section.setElection(new ElectionEntity(electionId)); 
            section.setPartNo(partNo);
            section.setSectionNo(sectionNo);
            section.setSectionNameEn(sectionNameEn);
            section.setSectionNameL1(sectionNameL1);
            section.setAccountId(accountId);

            return sectionRepository.save(section); 
        }

        return existingSection.get(); 
    }

}
