package com.thedal.thedal_app.voter;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.PartManager;
import com.thedal.thedal_app.election.PartManagerRepository;

@Service
public class PartManagerService {
	
	@Autowired
    private PartManagerRepository partManagerRepository;

    public PartManager savePartManager(Long electionId, String partNo, String partNameEn, String partNameL1, 
                                       Double partLat, Double partLong, String pincode, Long accountId) {
        Optional<PartManager> existingPartManager = partManagerRepository
            .findByPartNoAndElectionIdAndAccountId(partNo, electionId, accountId);

        if (!existingPartManager.isPresent()) {
            PartManager partManager = new PartManager();
            partManager.setElectionId(electionId);
            partManager.setPartNo(partNo);
            partManager.setPartNameEnglish(partNameEn);
            partManager.setPartNameL1(partNameL1);
            //partManager.setSchoolName(schoolName);
            partManager.setSchoolName(partNameEn);
            partManager.setPartLat(partLat);
            partManager.setPartLong(partLong);
            partManager.setPincode(pincode);
            partManager.setSchoolLat(partLat);
            partManager.setSchoolLong(partLong);
            partManager.setAccountId(accountId);
            return partManagerRepository.save(partManager);
        }
        return existingPartManager.get();
    }

}
