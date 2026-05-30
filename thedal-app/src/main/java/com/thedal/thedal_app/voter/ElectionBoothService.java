package com.thedal.thedal_app.voter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.election.ElectionBooth;
import com.thedal.thedal_app.election.ElectionBoothRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.report.ReportService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ElectionBoothService {

	 @Autowired
	private ElectionBoothRepository electionBoothRepository;

	@Autowired
	private ReportService reportService;

	    @Autowired
	    private ElectionRepository electionRepository;
	    @Autowired
	    private ElectionBoothRepository boothRepository;

	    public ElectionBooth saveBooth(Long electionId, Integer boothNumber, Long accountId) {
	        // Check if the booth entry already exists for this election and booth
	        //Optional<ElectionBooth> existingBooth = electionBoothRepository.findByElectionIdAndBoothNumber(electionId, boothNumber);
	    	Optional<ElectionBooth> existingBooth = electionBoothRepository.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId);
	        
	        if (!existingBooth.isPresent()) {
	        	
	        	// Get the highest order index for booths under the same account
	            //Integer maxOrderIndex = boothRepository.findMaxOrderIndexByAccountId(accountId);
	        	Integer maxOrderIndex = boothRepository.findMaxOrderIndexByElectionId(electionId);
	        	int newOrderIndex = (maxOrderIndex != null) ? maxOrderIndex + 1 : 0;
	        	
	            ElectionBooth electionBooth = new ElectionBooth();
	            electionBooth.setElection(new ElectionEntity(electionId)); // assuming ElectionEntity constructor takes id
	            electionBooth.setBoothNumber(boothNumber);
	            electionBooth.setAccountId(accountId);
	            electionBooth.setOrderIndex(newOrderIndex);  
	            
				ElectionBooth booth = electionBoothRepository.save(electionBooth); // Save the new booth entry
	            
	            //FOR REPORTS
	            reportService.votersBasedOnBoothNumber(boothNumber,electionId,accountId); // Save the new booth entry
				return booth;
	        }
	        
	        return existingBooth.get(); // Return existing booth if already present
	    }	

	    public ElectionBooth updateElectionBooth(ElectionBooth electionBooth) {
	        return electionBoothRepository.save(electionBooth);
	    }
    
}
