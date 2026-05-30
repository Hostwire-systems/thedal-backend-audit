package com.thedal.thedal_app.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thedal.thedal_app.election.ElectionRepository;
import com.thedal.thedal_app.election.ElectionEntity;
import com.thedal.thedal_app.general.RequestDetailsService;
import com.thedal.thedal_app.report.cadre.ElectionDashboardDTO;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportEntity;
import com.thedal.thedal_app.report.cadre.VolunteerVsVoterReportRepository;
import com.thedal.thedal_app.report.dto.BoothWiseTimingVotersCountResponseDTO;
import com.thedal.thedal_app.report.dto.CadreElectionOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.CadreOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.CadrePerformanceDto;
import com.thedal.thedal_app.report.dto.CadreReportDTO;
import com.thedal.thedal_app.report.dto.ElectionDashboardOverviewResponseDTO;
import com.thedal.thedal_app.report.dto.ElectionOverviewDTO;
import com.thedal.thedal_app.report.dto.PollDayReportDTO;
import com.thedal.thedal_app.report.dto.PollingAgeWiseResponse;
import com.thedal.thedal_app.report.dto.PollingBasedOnAgeResponseDTO;
import com.thedal.thedal_app.report.dto.PollingPartyWiseResponse;
import com.thedal.thedal_app.report.dto.PartyVoterCount;
import com.thedal.thedal_app.report.dto.PartyWiseVotersResponse;
import com.thedal.thedal_app.report.dto.BoothStrengthOverviewDTO;
import com.thedal.thedal_app.report.dto.VoterVoteDetailsRequest;
import com.thedal.thedal_app.report.dto.VotersHaveContactsResponseDTO;
import com.thedal.thedal_app.report.dto.VotersHavingContactsDTO;
import com.thedal.thedal_app.report.election.ElectionDashboardOverviewEntity;
import com.thedal.thedal_app.report.election.ElectionDashboardOverviewRepository;
import com.thedal.thedal_app.report.election.ElectionMobileNumberEntity;
import com.thedal.thedal_app.report.election.ElectionMobileNumberRepository;
import com.thedal.thedal_app.report.election.ElectionPincodeEntity;
import com.thedal.thedal_app.report.election.ElectionPincodeRepository;
import com.thedal.thedal_app.report.election.VoterPartyReportEntity;
import com.thedal.thedal_app.report.election.VoterPartyReportRepository;
import com.thedal.thedal_app.report.election.VotersByBoothHavingContactsEntity;
import com.thedal.thedal_app.report.election.VotersByBoothHavingContactsRepository;
import com.thedal.thedal_app.report.pollday.BoothWiseTimingVotersCount;
import com.thedal.thedal_app.settings.electionsettings.Party;
import com.thedal.thedal_app.settings.electionsettings.PartyRepository;
import com.thedal.thedal_app.report.pollday.BoothWiseTimingVotersCountRedisRepo;
import com.thedal.thedal_app.report.pollday.BoothWiseTimingVotersCountRepository;
import com.thedal.thedal_app.report.pollday.PollingAgePercentageRepository;
import com.thedal.thedal_app.report.pollday.PollingAgeWiseRedis;
import com.thedal.thedal_app.report.pollday.PollingAgeWiseRedisRepo;
import com.thedal.thedal_app.report.pollday.VoteCountType;
import com.thedal.thedal_app.thedal_exception.ThedalError;
import com.thedal.thedal_app.thedal_exception.ThedalException;
import com.thedal.thedal_app.user.UserEntity;
import com.thedal.thedal_app.user.UserRepo;
import com.thedal.thedal_app.volunteer.VolunteerDailyActivityRepository;
import com.thedal.thedal_app.volunteer.VolunteerRepository;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ReportService {

	@Autowired
	private ElectionRepository electionRepository;
	
	@Autowired
	private VoterRepo voterRepo;
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private PollingAgePercentageRepository pollingAgePercentageRepository;
	
	@Autowired
	private VolunteerRepository volunteerRepository;
	
	@Autowired
	private VolunteerDailyActivityRepository volunteerDailyActivityRepository;
	
	@Autowired
	private VolunteerVsVoterReportRepository volunteerVsVoterReportRepository;
	
	@Autowired
	private ElectionPincodeRepository electionPincodeRepository;
	
	@Autowired
	private ElectionDashboardOverviewRepository electionDashboardOverviewRepository;
	
	@Autowired
	private ElectionMobileNumberRepository electionMobileNumberRepository;
	
//	@Autowired
//	private PollingBasedOnAgeRepository pollingBasedOnAgeRepository;
	
	@Autowired
	private VotersByBoothHavingContactsRepository votersByBoothHavingContactsRepository;
	
    @Autowired
    private RequestDetailsService requestDetails;
    
    @Autowired
    private BoothWiseTimingVotersCountRedisRepo boothWiseTimingVotersCountRedisRepo;
    
    @Autowired
    private PollingAgeWiseRedisRepo pollingAgeWiseRedisRepo;
    
    @Autowired
    private VoterPartyReportRepository voterPartyReportRepository;
    @Autowired
    private PartyRepository partyRepository;
    @Autowired
    private PollingAgeWiseService pollingAgeWiseService;
    @Autowired
    private CadreReportRepository cadreReportRepository;
    @Autowired
    private PollDayReportRepository pollDayReportRepository;
    @Autowired
    private ElectionDashboardRepository electionDashboardRepository;
    @Autowired
    private PollingAgeWiseRepository pollingAgeWiseRepository;
    @Autowired
    private BoothWiseTimingVotersCountRepository boothWiseTimingVotersCountRepo;
	
	// 
//	@Scheduled(cron = "0 0 1 * * ?") // Run daily at 01:00
//	private void calculatePollingAgePercentage() {
//		log.info("inside calculatePollingAgePercentage: TOTAL_VOTERS");
//		
//		LocalDate today = LocalDate.now();
//		
//		List<ElectionEntity> completedElections = electionRepository.findByIsCompleteAndCompletedDate(true, today);
//		
//		for(ElectionEntity election : completedElections) {
//			Long electionId = election.getId();
//			
//			// Fetch voters for this election
//            List<VoterEntity> voters = voterRepository.findByElectionId(electionId);
//            long totalPolled = voters.stream().filter(VoterEntity::getHasVoted).count();
//			long totalVoters = voters.size();
//			long totalNotVoted = totalVoters - totalPolled;
//           
//			// Polled voters age groups
//            AgeGroupStats polledStats = calculateAgeGroupStats(voters.stream().filter(VoterEntity::getHasVoted).toList(), today, totalVoters);
//            double overallPolledPercentage = calculatePercentage(totalPolled, totalVoters);
//
//            savePollingAgePercentage(electionId, VoteCountTye.TOTAL_VOTERS, polledStats, overallPolledPercentage);
//
//            // Not voted voters age groups
//            AgeGroupStats notVotedStats = calculateAgeGroupStats(voters.stream().filter(v -> !v.getHasVoted()).toList(), today, totalVoters);
//            double overallNotVotedPercentage = calculatePercentage(totalNotVoted, totalVoters);
//
//            savePollingAgePercentage(electionId, VoteCountTye.NOT_VOTED_VOTERS, notVotedStats, overallNotVotedPercentage);
//
//       
//		}
//		
//	}
//	
//	private AgeGroupStats calculateAgeGroupStats(List<VoterEntity> voters, LocalDate today, long totalVoters) {
//		 long ageGroup18To21 = 0, ageGroup22To25 = 0, ageGroup26To35 = 0, ageGroup36To45 = 0, ageGroup46To59 = 0;
//         
//			for(VoterEntity voterEntity:voters) {
//				int age = Period.between(voterEntity.getDateOfBirth(),today);
//				
//				if(age>=18 && age <= 21) ageGroup18To21++;
//				else if(age >= 22 && age <= 25) ageGroup22To25++;
//				else if(age >= 26 && age <= 35) ageGroup26To35++;
//				else if(age >= 36 && age <= 45) ageGroup36To45++;
//				else if(age >= 46 && age <= 59) ageGroup46To59++;
//			}
//		    return new AgeGroupStats(
//		            calculatePercentage(ageGroup18To21, totalVoters),
//		            calculatePercentage(ageGroup22To25, totalVoters),
//		            calculatePercentage(ageGroup26To35, totalVoters),
//		            calculatePercentage(ageGroup36To45, totalVoters),
//		            calculatePercentage(ageGroup46To59, totalVoters)
//		        );
//		
//	}
	
//	private void savePollingAgePercentage(Long electionId, VoteCountTye voteCountType, AgeGroupStats stats, double overallPercentage) {
//		 PollingAgePercentage pollingAgePercentage = new PollingAgePercentage();
//	        pollingAgePercentage.setElectionId(electionId);
//	        pollingAgePercentage.setVoteCountType(VoteCountTye.TOTAL_VOTERS);
//	        pollingAgePercentage.setAgeGroup18To21(stats.getAgeGroup18To21());
//	        pollingAgePercentage.setAgeGroup22To25(stats.getAgeGroup22To25());
//	        pollingAgePercentage.setAgeGroup26To35(stats.getAgeGroup26To35());
//	        pollingAgePercentage.setAgeGroup36To45(stats.getAgeGroup36To45());
//	        pollingAgePercentage.setAgeGroup46To59(stats.getAgeGroup46To59());
//	        pollingAgePercentage.setOverallPolledPercentage(overallPercentage);
//	        pollingAgePercentage.setCurrentTimestamp(LocalDateTime.now());
//			
//	        pollingAgePercentageRepository.save(pollingAgePercentage);
//	}
//
//	private double calculatePercentage(long groupCount, long total) {
//		
//		return total > 0 ? (groupCount * 100)/total : 0.0;
//	}

	public CadreOverviewResponseDTO getCadreOverviewForElection(Long electionId) {
		log.info("inside getCadreOverviewForElection:election id:{}",electionId);
		int volunteerCount = volunteerRepository.countByElectionEntity_Id(electionId);
		int countCheckedInVolunteersFilterFirst = volunteerDailyActivityRepository.countCheckedInVolunteersFilterFirst(electionId);
		//CadreElectionOverviewResponseDTO cadreElectionOverviewResponseDTO=volunteerVsVoterReportRepository.getTotalUpdatedSummaryByElectionId(electionId);
		CadreElectionOverviewResponseDTO cadreElectionOverviewResponseDTO = volunteerVsVoterReportRepository.getTotalUpdatedSummaryByElectionId(electionId);
		int activeCadreCount = volunteerRepository.countActiveByElectionEntity_Id(electionId);
	    int inactiveCadreCount = volunteerRepository.countInactiveByElectionEntity_Id(electionId);
	    	    
	    int maleCadreCount = volunteerRepository.countMaleByElectionEntity_Id(electionId);
        int femaleCadreCount = volunteerRepository.countFemaleByElectionEntity_Id(electionId);
        int otherCadreCount = volunteerRepository.countOtherByElectionEntity_Id(electionId);
		log.info("end of getCadreOverviewForElection:election id:{}",electionId);
		
		return new CadreOverviewResponseDTO(volunteerCount,
				countCheckedInVolunteersFilterFirst,
				(volunteerCount-countCheckedInVolunteersFilterFirst),
				 cadreElectionOverviewResponseDTO.getTotalWhatsappNumberUpdated(),
			     cadreElectionOverviewResponseDTO.getTotalRolesUpdated(),
			     cadreElectionOverviewResponseDTO.getTotalBoothsUpdated(),
			     cadreElectionOverviewResponseDTO.getTotalAddressUpdated(),
				activeCadreCount,
				inactiveCadreCount,
				maleCadreCount,
	            femaleCadreCount,
	            otherCadreCount
				);
		
	}
	
	
	public List<CadrePerformanceDto> getTopPerformanceCadre(Long electionId) {
	    log.info("inside getTopPerformanceCadre:election id:{}", electionId);
	    List<CadrePerformanceDto> top10ByElectionIdOrderByTotalVoterCreated = volunteerVsVoterReportRepository
	            .findTop10ByElectionIdOrderByTotalVoterCreated(electionId);

	    log.info("end of getTopPerformanceCadre:election id:{}", electionId);

	    return top10ByElectionIdOrderByTotalVoterCreated.stream()
	            .filter(report -> report.getUserId() != null) // Skip records with null userId
	            .map(report -> {
	                UserEntity userEntity = userRepo.findById(report.getUserId()).orElse(null);
	                String userName = (userEntity != null) ? userEntity.getFirstName() : "Unknown User (ID: " + report.getUserId() + ")";
	                return new CadrePerformanceDto(report.getUserId(), userName, report.getTotalVoterCreated());
	            })
	            .collect(Collectors.toList());
	}

	public List<CadrePerformanceDto> getLeastPerformanceCadre(Long electionId) {
	    Pageable pageable = PageRequest.of(0, 10);
	    log.info("inside getLeastPerformanceCadre:election id:{}", electionId);
	    List<CadrePerformanceDto> least10ByElectionIdOrderByTotalVoterCreated = volunteerVsVoterReportRepository
	            .findLeast10ByElectionIdOrderByTotalVoterCreated(electionId, pageable);

	    log.info("end of getLeastPerformanceCadre:election id:{}", electionId);

	    return least10ByElectionIdOrderByTotalVoterCreated.stream()
	            .filter(report -> report.getUserId() != null) // Skip records with null userId
	            .map(report -> {
	                UserEntity userEntity = userRepo.findById(report.getUserId()).orElse(null);
	                String userName = (userEntity != null) ? userEntity.getFirstName() : "Unknown User (ID: " + report.getUserId() + ")";
	                return new CadrePerformanceDto(report.getUserId(), userName, report.getTotalVoterCreated());
	            })
	            .collect(Collectors.toList());
	}
	
	
	@Transactional
	public void saveOrUpdateVolunteerVsVoterReport(Long electionId, Long userId, Long accountId,
			boolean isMobileNoUpdated, boolean isReligionUpdated, boolean isCasteUpdated, boolean isDobUpdated, 
			boolean isPartyUpdated, boolean isLanguageUpdated, boolean isNewVoter) {
		log.info("inside saveOrUpdateVolunteerVsVoterReport:election id:{}, userId:{}, accountId:{}", electionId, userId, accountId);
//		VolunteerVsVoterReportEntity reportEntity = volunteerVsVoterReportRepository
//	            .findByElectionIdAndVolunteerId(electionId, volunteerId)
//	            .orElseGet(() -> createNewReport(electionId, volunteerId));
		VolunteerVsVoterReportEntity reportEntity = volunteerVsVoterReportRepository
	            .findByElectionIdAndUserId(electionId, userId)
	            .orElseGet(() -> createNewReport(electionId, userId, accountId));
	    
	    if (isMobileNoUpdated) {
	        reportEntity.setTotalMobileNumberUpdated(reportEntity.getTotalMobileNumberUpdated() + 1);
	    }
	    if (isReligionUpdated) {
	        reportEntity.setTotalReligionUpdated(reportEntity.getTotalReligionUpdated() + 1);
	    }
	    if (isCasteUpdated) {
	        reportEntity.setTotalCasteUpdated(reportEntity.getTotalCasteUpdated() + 1);
	    }
	    if (isDobUpdated) {
	        reportEntity.setTotalDobUpdated(reportEntity.getTotalDobUpdated() + 1);
	    }
	    if (isPartyUpdated) {
	        reportEntity.setTotalPartyUpdated(reportEntity.getTotalPartyUpdated() + 1);
	    }
	    if (isLanguageUpdated) {
	        reportEntity.setTotalLanguageUpdated(reportEntity.getTotalLanguageUpdated() + 1);
	    }

	    // Update total voters created or updated
	    if (isNewVoter) {
	        reportEntity.setTotalVoterCreated(reportEntity.getTotalVoterCreated() + 1);
	    } else {
	        reportEntity.setTotalVoterUpdated(reportEntity.getTotalVoterUpdated() + 1);
	    }

	    reportEntity.setCurrentTimeStamp(LocalDateTime.now());
	    log.info("end of saveOrUpdateVolunteerVsVoterReport:election id:{},volunteer id:{}", electionId,userId);
	    volunteerVsVoterReportRepository.save(reportEntity);
	}
	
	private VolunteerVsVoterReportEntity createNewReport(Long electionId, Long userId, Long accountId) {
	    VolunteerVsVoterReportEntity newReport = new VolunteerVsVoterReportEntity();
	    newReport.setElectionId(electionId);
	    //newReport.setVolunteerId(volunteerId);
	    newReport.setUserId(userId);
	    newReport.setAccountId(accountId);
	    newReport.setTotalMobileNumberUpdated(0L);
	    newReport.setTotalReligionUpdated(0L);
	    newReport.setTotalCasteUpdated(0L);
	    newReport.setTotalDobUpdated(0L);
	    newReport.setTotalPartyUpdated(0L);
	    newReport.setTotalLanguageUpdated(0L);
	    newReport.setTotalVoterCreated(0L);
	    newReport.setTotalVoterUpdated(0L);
	    newReport.setCurrentTimeStamp(LocalDateTime.now());
	    return newReport;
	}
	
	@Transactional
	public void saveElectionOverview(Long electionId,Long accountId, List<ElectionOverviewDTO> electionOverviewDTOList) {
		log.info("inside saveElectionOverview:election id:{}", electionId);
		
        ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
        		.orElseGet(()->{
        		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
        		    newOverview.setElectionId(electionId);
        		    newOverview.setAccountId(accountId);
        		    newOverview.setAgeGroup18To21(0);
        		    newOverview.setAgeGroup18To30(0);
        		    newOverview.setAgeGroup30To40(0);
        		    newOverview.setAgeGroup40To50(0);
        		    newOverview.setAgeGroup50To60(0);
        		    newOverview.setAgeGroup60To70(0);
	                newOverview.setAbove70(0);
	                newOverview.setAgeGroup60To80(0);
	                newOverview.setAbove80(0);
	                newOverview.setTotalVoters(0);
	                newOverview.setTotalBooth(0);
	                newOverview.setNoOfPincode(0);
	                newOverview.setMale(0);
	                newOverview.setFemale(0);
	                newOverview.setTransgender(0);
        		    return newOverview;
        		});
        
        List<ElectionDashboardOverviewEntity> electionDashboardOverviewList = new ArrayList<>();
        for(ElectionOverviewDTO dto : electionOverviewDTOList) {
        	
        	String pincode = dto.getPincode();
        	String mobileNumber = dto.getMobileNumber();
        	String gender = dto.getGender();
        	boolean isNewVoter = dto.isNewVoter();
        
        if (pincode != null) {
        	log.info("inside saveElectionOverview:election id:{},pincode:{}", electionId,pincode);
            boolean exists = electionPincodeRepository.existsByElectionIdAndPincodeAndAccountId(electionId, pincode, accountId);
            if (!exists) {
                ElectionPincodeEntity entity = new ElectionPincodeEntity();
                entity.setElectionId(electionId);
                entity.setPincode(pincode);
                entity.setAccountId(accountId);
                electionPincodeRepository.save(entity);
                
//                ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
//                		.orElseGet(()->{
//                		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
//                		    newOverview.setElectionId(electionId);
//                		    newOverview.setAccountId(accountId);
//                		    
//                		    return newOverview;
//                		});
                electionDashboardOverviewEntity.setNoOfPincode(electionDashboardOverviewEntity.getNoOfPincode() + 1);
//                electionDashboardOverviewRepository.save(electionDashboardOverviewEntity);
                
                }
        } //END: of pincode 
        
        
        if(mobileNumber != null) {
        	log.info("inside saveElectionOverview:election id:{},mobile:{}", electionId,mobileNumber);
        	boolean exists = electionMobileNumberRepository.existsByElectionIdAndMobileAndAccountId(electionId, mobileNumber, accountId);
            if (!exists) {
            	ElectionMobileNumberEntity entity = new ElectionMobileNumberEntity();
                 entity.setElectionId(electionId);
                 entity.setMobile(mobileNumber);
                 entity.setAccountId(accountId);
                 electionMobileNumberRepository.save(entity);
                 
//                 ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
//                 		.orElseGet(()->{
//                 		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
//                 		    newOverview.setElectionId(electionId);
//                 		    newOverview.setAccountId(accountId);
//                 		    
//                 		    return newOverview;
//                 		});
                 electionDashboardOverviewEntity.setNoOfMobileNumber(electionDashboardOverviewEntity.getNoOfMobileNumber() + 1);
//                 electionDashboardOverviewRepository.save(electionDashboardOverviewEntity);
            	
            }
        } //END: of mobile 
        
        if(gender != null) {
        	log.info("inside saveElectionOverview:election id:{},gender:{}", electionId,gender);
//            ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
//             		.orElseGet(()->{
//             		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
//             		    newOverview.setElectionId(electionId);
//             		    newOverview.setAccountId(accountId);
//             		    
//             		    return newOverview;
//             		});
        	switch(gender.toUpperCase()) {
        	case "Male":
        		electionDashboardOverviewEntity.setMale(electionDashboardOverviewEntity.getMale() + 1);
        		break;
        	case "Female":
        		electionDashboardOverviewEntity.setFemale(electionDashboardOverviewEntity.getFemale() + 1);
        		break;
        	case "Other":
        		electionDashboardOverviewEntity.setTransgender(electionDashboardOverviewEntity.getTransgender() + 1);
        		break;
        	}
//        	 electionDashboardOverviewRepository.save(electionDashboardOverviewEntity);
        	
            }
        
        
        
        if(isNewVoter) {
        	log.info("inside saveElectionOverview:election id:{},isNewVoter:{}", electionId,isNewVoter);
//            ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
//             		.orElseGet(()->{
//             		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
//             		    newOverview.setElectionId(electionId);
//             		    newOverview.setAccountId(accountId);
//             		    
//             		    return newOverview;
//             		});
            electionDashboardOverviewEntity.setTotalVoters(electionDashboardOverviewEntity.getTotalVoters() + 1);
//            electionDashboardOverviewRepository.save(electionDashboardOverviewEntity);
        }//END: of isNewVoter
        
        electionDashboardOverviewList.add(electionDashboardOverviewEntity);
        log.info("saveElectionOverview:election id:{}", electionId);
        }
        
        
		if (!electionDashboardOverviewList.isEmpty()) {
			electionDashboardOverviewRepository.saveAll(electionDashboardOverviewList);
			log.info("electionDashboardOverviewList is not empty .end of saveElectionOverview:election id:{}",
					electionId);
		} else {
			log.info("electionDashboardOverviewList is empty.end of saveElectionOverview:election id:{}", electionId);
		}
    }
	
	public void votersBasedOnBoothNumber(Integer boothNumber,Long electionId,Long accountId) {
		log.info("inside votersBasedOnBoothNumber:election id:{},boothList:{}", electionId,boothNumber);
		if(boothNumber != null) {
			 ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
		        		.orElseGet(()->{
		        		 ElectionDashboardOverviewEntity newOverview = new ElectionDashboardOverviewEntity();
		        		    newOverview.setElectionId(electionId);
		        		    newOverview.setAccountId(accountId);
		        		    newOverview.setAgeGroup18To21(0);
		        		    newOverview.setAgeGroup18To30(0);
		        		    newOverview.setAgeGroup30To40(0);
		        		    newOverview.setAgeGroup40To50(0);
		        		    newOverview.setAgeGroup50To60(0);
		        		    newOverview.setAgeGroup60To70(0);
			                newOverview.setAbove70(0);
			                newOverview.setAgeGroup60To80(0);
			                newOverview.setAbove80(0);
			                newOverview.setTotalVoters(0);
			                newOverview.setTotalBooth(0);
			                newOverview.setNoOfPincode(0);
			                newOverview.setMale(0);
			                newOverview.setFemale(0);
			                newOverview.setTransgender(0);
		        		    return newOverview;
		        		});
			 electionDashboardOverviewEntity.setTotalBooth(electionDashboardOverviewEntity.getTotalBooth() + 1);
			 electionDashboardOverviewRepository.save(electionDashboardOverviewEntity);		
		}
		
	}
	
	
	public void votersBasedOnAge(List<Integer> ageList,Long electionId,Long accountId) {
		log.info("inside votersBasedOnAge:election id:{},ageList:{}", electionId,ageList);
		if(ageList != null && !ageList.isEmpty()) {
			List<ElectionDashboardOverviewEntity> pollingBasedOnAgeList = new ArrayList<>();
			
			
			for(Integer age:ageList) {
			log.info("inside votersBasedOnAge:age is not null:election id:{},age:{}", electionId,age);
			ElectionDashboardOverviewEntity pollingData = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId)
			            .orElseGet(() -> {
			            	ElectionDashboardOverviewEntity newPollingData = new ElectionDashboardOverviewEntity();
			            	newPollingData.setElectionId(electionId);
			            	newPollingData.setAccountId(accountId);
			            	newPollingData.setAgeGroup18To21(0);
		        		    newPollingData.setAgeGroup18To30(0);
		        		    newPollingData.setAgeGroup30To40(0);
		        		    newPollingData.setAgeGroup40To50(0);
		        		    newPollingData.setAgeGroup50To60(0);
		        		    newPollingData.setAgeGroup60To70(0);
		        		    newPollingData.setAbove70(0);
		        		    newPollingData.setAgeGroup60To80(0);
		        		    newPollingData.setAbove80(0);
		        		    newPollingData.setTotalVoters(0);
		        		    newPollingData.setTotalBooth(0);
		        		    newPollingData.setNoOfPincode(0);
		        		    newPollingData.setMale(0);
		        		    newPollingData.setFemale(0);
		        		    newPollingData.setTransgender(0);
			                return newPollingData;
			            });

			    if (age >= 18 && age <= 21) {
			        pollingData.setAgeGroup18To21(pollingData.getAgeGroup18To21() + 1);
			    } else if (age >= 18 && age <= 30) {
			        pollingData.setAgeGroup18To30(pollingData.getAgeGroup18To30() + 1);
			    } else if (age > 30 && age <= 40) {
			        pollingData.setAgeGroup30To40(pollingData.getAgeGroup30To40() + 1);
			    } else if (age > 40 && age <= 50) {
			        pollingData.setAgeGroup40To50(pollingData.getAgeGroup40To50() + 1);
			    } else if (age > 50 && age <= 60) {
			        pollingData.setAgeGroup50To60(pollingData.getAgeGroup50To60() + 1);
			    } else if (age > 60 && age <= 70) {
			        pollingData.setAgeGroup60To70(pollingData.getAgeGroup60To70() + 1);
			    } else if (age > 70 && age <= 80) {
			        pollingData.setAgeGroup60To80(pollingData.getAgeGroup60To80() + 1);
			    } else if (age > 80) {
			        pollingData.setAbove80(pollingData.getAbove80() + 1);
			    }
			    pollingData.setTotalVoters(pollingData.getTotalVoters() + 1);			    pollingBasedOnAgeList.add(pollingData);
			    log.info("end of votersBasedOnAge:election id:{},age:{}", electionId,age);
			    
			}
			
			electionDashboardOverviewRepository.saveAll(pollingBasedOnAgeList);

		}
		
	}
	
	
	public void votersHavingContacts(List<VotersHavingContactsDTO> votersHavingContactsDTO,Long electionId,Long accountId) {
		log.info("inside votersHavingContacts:election id:{},dto:{}", electionId,votersHavingContactsDTO);

		
		   if (votersHavingContactsDTO == null || 
			        electionId == null || accountId == null) {
			        return; 
			    }
		   
		   List<VotersByBoothHavingContactsEntity> votersByBoothHavingContactsList = new ArrayList<>();
		   
		   for(VotersHavingContactsDTO dto:votersHavingContactsDTO) {
			   
			   Integer boothNumber = dto.getBoothNumber();
		        String mobileNumber = dto.getMobileNumber();
		        
		        if (boothNumber == null ||
		                mobileNumber == null || mobileNumber.isEmpty()) {
		                log.warn("Invalid data in DTO: boothNumber or mobileNumber is null/empty");
		                continue; // Skip invalid entries
		            }
		        
		        
		   VotersByBoothHavingContactsEntity entity = votersByBoothHavingContactsRepository
		            .findByElectionIdAndAccountIdAndBoothNumberAndMobileNumber(electionId, accountId, boothNumber,mobileNumber)
		            .orElseGet(() -> {
		               
		                VotersByBoothHavingContactsEntity newEntity = new VotersByBoothHavingContactsEntity();
		                newEntity.setElectionId(electionId);
		                newEntity.setAccountId(accountId);
		                newEntity.setBoothNumber(boothNumber);
		                newEntity.setVoterCount(0); 
		                newEntity.setMobileNumber(mobileNumber);
		                return newEntity;
		            });
		   
		    entity.setVoterCount(entity.getVoterCount() + 1);
		    
		    votersByBoothHavingContactsList.add(entity);
		    
		    log.info("end of votersHavingContacts:election id:{},boothNumber:{}", electionId,boothNumber);
		    
		    
		   }
		   
		   if(! votersByBoothHavingContactsList.isEmpty()) {
			   votersByBoothHavingContactsRepository.saveAll(votersByBoothHavingContactsList);
			   log.info("Batch saved {} entities for electionId: {}", votersByBoothHavingContactsList.size(), electionId);
		   } else {
			   log.info("No valid entities to save for electionId: {}", electionId);
		   }
		   
		   log.info("Completed processing votersHavingContacts for electionId: {}", electionId);
		
	}


	public ElectionDashboardOverviewResponseDTO getElectionDashboardOverview(Long electionId) {
		log.info("inside getElectionDashboardOverview:election id:{}", electionId);
		
		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    ElectionDashboardOverviewResponseDTO dto = new ElectionDashboardOverviewResponseDTO();
		ElectionDashboardOverviewEntity electionDashboardOverviewEntity=   electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId,accountId)
				 .orElseThrow(() ->
		                 new ThedalException(ThedalError.ELECTION_NO_RECORDS, HttpStatus.NOT_FOUND)
		            );
		
//		PollingBasedOnAgeEntity pollingData = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId)
//				 .orElseThrow(() ->
//                 new ThedalException(ThedalError.ELECTION_NO_RECORDS, HttpStatus.NOT_FOUND)
//            );
		
		dto.setElectionId(electionId);
		dto.setTotalBooth(electionDashboardOverviewEntity.getTotalBooth());
		dto.setTotalVoters(electionDashboardOverviewEntity.getTotalVoters());
		dto.setNoOfMobileNumber(electionDashboardOverviewEntity.getNoOfPincode());
		dto.setNoOfMobileNumber(electionDashboardOverviewEntity.getNoOfMobileNumber());
		dto.setMale(electionDashboardOverviewEntity.getMale());
		dto.setFemale(electionDashboardOverviewEntity.getFemale());
		dto.setTransgender(electionDashboardOverviewEntity.getTransgender());
		dto.setFirstTimeVoters(electionDashboardOverviewEntity.getAgeGroup18To21());
		dto.setSeniorCitizens(electionDashboardOverviewEntity.getAgeGroup60To80());
		dto.setSuperCitizens(electionDashboardOverviewEntity.getAbove80());		
		log.info("end of getElectionDashboardOverview:election id:{}}", electionId);
		return dto;
	}


	public PollingBasedOnAgeResponseDTO getPollingBasedOnAgeRange(Long electionId) {
		log.info("inside getPollingBasedOnAgeRange:election id:{}", electionId);

		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    ElectionDashboardOverviewEntity pollingData = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId)
				 .orElseThrow(() ->
                 new ThedalException(ThedalError.ELECTION_NO_RECORDS, HttpStatus.NOT_FOUND)
            );
		PollingBasedOnAgeResponseDTO dto=new PollingBasedOnAgeResponseDTO();
		dto.setElectionId(electionId);
		dto.setAgeGroup18To30(pollingData.getAgeGroup18To30());
		dto.setAgeGroup30To40(pollingData.getAgeGroup30To40());
		dto.setAgeGroup40To50(pollingData.getAgeGroup40To50());
		dto.setAgeGroup50To60(pollingData.getAgeGroup50To60());
		dto.setAgeGroup60To70(pollingData.getAgeGroup60To70());
		dto.setAbove70(pollingData.getAbove70());
		log.info("end of getPollingBasedOnAgeRange:election id:{}", electionId);
		return dto;
	}


	public List<VotersHaveContactsResponseDTO> getVotersHaveContacts(Long electionId) {
		log.info("inside getVotersHaveContacts:election id:{}", electionId);
		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    List<VotersByBoothHavingContactsEntity> votersByBoothHavingContactsList = votersByBoothHavingContactsRepository
	            .findByElectionIdAndAccountId(electionId, accountId);
		
		log.info("end of getVotersHaveContacts:election id:{}", electionId);
		return votersByBoothHavingContactsList.stream()
				.map(votersByBoothHavingContacts -> new VotersHaveContactsResponseDTO(
						votersByBoothHavingContacts.getElectionId(), votersByBoothHavingContacts.getBoothNumber(),
						votersByBoothHavingContacts.getVoterCount()))
				.toList();
		}
	
    public List<BoothWiseTimingVotersCountResponseDTO> getBoothWiseTimingVotersCountByElectionId(Long electionId) {
    	log.info("inside getBoothWiseTimingVotersCountByElectionId:election id:{}", electionId);
    	Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
        return boothWiseTimingVotersCountRedisRepo.getListOfBoothWiseTimingVotersCountByElectionId(accountId,electionId);
    }
  
//    public BoothWiseTimingVotersCountRedis getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(Long electionId, Long boothNumber) {
//    	log.info("inside getBoothWiseTimingVotersCountByElectionIdAndBoothNumber:election id:{},booth number:{}", electionId,boothNumber);
//    	Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account id not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//        return boothWiseTimingVotersCountRedisRepo.getBoothWiseTimingVotersCount(accountId,electionId, boothNumber)
//        		.orElse(null);
//    }
//    public BoothWiseTimingVotersCountRedis getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(Long electionId, Long boothNumber) {
//        log.info("inside getBoothWiseTimingVotersCountByElectionIdAndBoothNumber: electionId={}, boothNumber={}", electionId, boothNumber);
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account id not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        Optional<BoothWiseTimingVotersCountRedis> recordOpt = boothWiseTimingVotersCountRedisRepo.getBoothWiseTimingVotersCount(accountId, electionId, boothNumber);
//        if (recordOpt.isEmpty()) {
//            log.warn("No BoothWiseTimingVotersCountRedis record found for electionId={}, boothNumber={}. Populating data.", electionId, boothNumber);
//            List<VoterEntity> voters = voterRepo.findByElectionIdAndAccountId(electionId, accountId);
//            if (voters.stream().noneMatch(v -> v.getPartNo() != null && v.getPartNo().equals(boothNumber.intValue()) && Boolean.TRUE.equals(v.getHasVoted()))) {
//                log.info("No voted voters found for electionId={}, boothNumber={}", electionId, boothNumber);
//                return null; // Early return for invalid booth
//            }
//            boothWiseTimingVotersCountRedisRepo.clearBoothWiseTimingVotersCount(accountId, electionId);
//            boothWiseTimingVotersCountRedisRepo.populateBoothWiseTimingVotersCount(accountId, electionId, voters);
//            recordOpt = boothWiseTimingVotersCountRedisRepo.getBoothWiseTimingVotersCount(accountId, electionId, boothNumber);
//            if (recordOpt.isEmpty()) {
//                log.info("No voters found for electionId={}, boothNumber={}", electionId, boothNumber);
//                return null; // Returns 404 via controller
//            }
//        }
//        return recordOpt.get();
//    }

    @Transactional
    public BoothWiseTimingVotersCount getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(Long electionId, Long boothNumber) {
        log.debug("Processing getBoothWiseTimingVotersCountByElectionIdAndBoothNumber: electionId={}, boothNumber={}", electionId, boothNumber);
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account ID not found, unauthorized access");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        // Check for existing record
        Optional<BoothWiseTimingVotersCount> recordOpt = boothWiseTimingVotersCountRepo.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId);
        if (recordOpt.isPresent()) {
            log.debug("Found existing record for electionId={}, boothNumber={}, accountId={}", electionId, boothNumber, accountId);
            return recordOpt.get();
        }

        // Fetch voters
        List<VoterEntity> voters = voterRepo.findByElectionIdAndAccountId(electionId, accountId);
        log.debug("Found {} voters for electionId={}, accountId={}", voters.size(), electionId, accountId);
        voters.forEach(v -> log.debug("Voter: id={}, partNo={}, hasVoted={}, votedTimestamp={}, epicNumber={}", 
                                     v.getId(), v.getPartNo(), v.getHasVoted(), v.getVotedTimestamp(), v.getEpicNumber()));

        // Check if any voters exist for the booth
        if (voters.stream().noneMatch(v -> v.getPartNo() != null && v.getPartNo().equals(boothNumber.intValue()) && Boolean.TRUE.equals(v.getHasVoted()))) {
            log.info("No voted voters found for electionId={}, boothNumber={}", electionId, boothNumber);
            return null;
        }

        // Clear all booth records for this election and account
        boothWiseTimingVotersCountRepo.deleteByElectionIdAndAccountId(electionId, accountId);
        log.debug("Cleared all booth records for electionId={}, accountId={}", electionId, accountId);

        // Populate all booths
        populateBoothWiseTimingVotersCount(accountId, electionId, voters);

        // Re-query for the specific booth
        recordOpt = boothWiseTimingVotersCountRepo.findByElectionIdAndBoothNumberAndAccountId(electionId, boothNumber, accountId);
        if (recordOpt.isEmpty()) {
            log.warn("No record found after population for electionId={}, boothNumber={}", electionId, boothNumber);
            return null;
        }
        log.info("Returning record for electionId={}, boothNumber={}, totalVote={}", electionId, boothNumber, recordOpt.get().getTotalVote());
        return recordOpt.get();
    }

    private void populateBoothWiseTimingVotersCount(Long accountId, Long electionId, List<VoterEntity> voters) {
        List<VoterEntity> invalidBoothVoters = voters.stream()
                .filter(v -> v.getPartNo() == null || v.getPartNo() <= 0)
                .collect(Collectors.toList());
        if (!invalidBoothVoters.isEmpty()) {
            log.warn("Found {} voters with invalid booth numbers for electionId={}, accountId={}", 
                     invalidBoothVoters.size(), electionId, accountId);
        }

        Map<Integer, List<VoterEntity>> votersByBooth = voters.stream()
                .filter(voter -> Boolean.TRUE.equals(voter.getHasVoted()) && voter.getVotedTimestamp() != null)
                .filter(voter -> voter.getPartNo() != null && voter.getPartNo() > 0)
                .collect(Collectors.groupingBy(VoterEntity::getPartNo));

        if (votersByBooth.isEmpty()) {
            log.info("No valid voters found for electionId={}, accountId={}", electionId, accountId);
            return;
        }

        for (Map.Entry<Integer, List<VoterEntity>> entry : votersByBooth.entrySet()) {
            Integer boothNumber = entry.getKey();
            List<VoterEntity> boothVoters = entry.getValue();

            BoothWiseTimingVotersCount record = new BoothWiseTimingVotersCount();
            record.setElectionId(electionId);
            record.setAccountId(accountId);
            record.setBoothNumber(boothNumber.longValue());
            record.setTime07To08(0);
            record.setTime08To09(0);
            record.setTime09To10(0);
            record.setTime10To11(0);
            record.setTime11To12(0);
            record.setTime12To13(0);
            record.setTime13To14(0);
            record.setTime14To15(0);
            record.setTime15To16(0);
            record.setTime16To17(0);
            record.setTotalVote(0);

            for (VoterEntity voter : boothVoters) {
                LocalDateTime votedTimestamp = voter.getVotedTimestamp();
                if (votedTimestamp == null) {
                    log.warn("Null votedTimestamp for voterId={}, electionId={}, boothNumber={}, epicNumber={}", 
                             voter.getId(), electionId, boothNumber, voter.getEpicNumber());
                    continue;
                }
                int hour = votedTimestamp.getHour();
                log.debug("Processing voterId={}, epicNumber={}, votedTimestamp={}, hour={}", 
                          voter.getId(), voter.getEpicNumber(), votedTimestamp, hour);
                if (hour >= 7 && hour < 17) {
                    if (hour >= 7 && hour < 8) {
                        record.setTime07To08(record.getTime07To08() + 1);
                    } else if (hour >= 8 && hour < 9) {
                        record.setTime08To09(record.getTime08To09() + 1);
                    } else if (hour >= 9 && hour < 10) {
                        record.setTime09To10(record.getTime09To10() + 1);
                    } else if (hour >= 10 && hour < 11) {
                        record.setTime10To11(record.getTime10To11() + 1);
                    } else if (hour >= 11 && hour < 12) {
                        record.setTime11To12(record.getTime11To12() + 1);
                    } else if (hour >= 12 && hour < 13) {
                        record.setTime12To13(record.getTime12To13() + 1);
                    } else if (hour >= 13 && hour < 14) {
                        record.setTime13To14(record.getTime13To14() + 1);
                    } else if (hour >= 14 && hour < 15) {
                        record.setTime14To15(record.getTime14To15() + 1);
                    } else if (hour >= 15 && hour < 16) {
                        record.setTime15To16(record.getTime15To16() + 1);
                    } else if (hour >= 16 && hour < 17) {
                        record.setTime16To17(record.getTime16To17() + 1);
                    }
                    record.setTotalVote(record.getTotalVote() + 1);
                } else {
                    log.warn("Voted timestamp {} outside 7 AM-5 PM for voterId={}, electionId={}, boothNumber={}, epicNumber={}", 
                             votedTimestamp, voter.getId(), electionId, boothNumber, voter.getEpicNumber());
                    continue;
                }
            }

            if (record.getTotalVote() > 0) {
                record.setTimestamp(LocalDateTime.now());
                boothWiseTimingVotersCountRepo.save(record);
                log.info("Populated BoothWiseTimingVotersCount for electionId={}, boothNumber={}, totalVote={}", 
                         electionId, boothNumber, record.getTotalVote());
            } else {
                log.info("No valid votes recorded for electionId={}, boothNumber={}", electionId, boothNumber);
            }
        }
    }

    public void updateVoterAgeGroupCount(Long electionId, Long accountId, List<Integer> voterAges) {
    	log.info("inside updateVoterAgeGroupCount:election id:{},voter age:{}", electionId,voterAges);
        // Retrieve record for TOTAL_VOTERS
        PollingAgeWiseRedis pollingRecord = pollingAgeWiseRedisRepo.findByElectionIdAndVoteCountType(electionId, VoteCountType.TOTAL_VOTERS);

        // Create a new record if it doesn't exist
        if (pollingRecord == null) {
        	log.info("inside updateVoterAgeGroupCount:polling record is null:election id:{},voter age:{}", electionId,voterAges);
            pollingRecord = new PollingAgeWiseRedis();
            String compositeId = electionId + "_" + accountId + "_" + VoteCountType.TOTAL_VOTERS.name(); // Combine electionId, accountId, and voteCountType
            pollingRecord.setId(compositeId);
            pollingRecord.setElectionId(electionId);
            pollingRecord.setAccountId(accountId);
            pollingRecord.setVoteCountType(VoteCountType.TOTAL_VOTERS.getValue());
            pollingRecord.setAgeGroup18To30(0);
            pollingRecord.setAgeGroup30To40(0);
            pollingRecord.setAgeGroup40To50(0);
            pollingRecord.setAgeGroup50To60(0);
            pollingRecord.setAgeGroup60To70(0);
            pollingRecord.setOverallPolledCount(0);
        }

        // Update the appropriate age group count
        for (int voterAge : voterAges) {
        if (voterAge >= 18 && voterAge < 30) {
            pollingRecord.setAgeGroup18To30(pollingRecord.getAgeGroup18To30() + 1);
        } else if (voterAge >= 30 && voterAge < 40) {
            pollingRecord.setAgeGroup30To40(pollingRecord.getAgeGroup30To40() + 1);
        } else if (voterAge >= 40 && voterAge < 50) {
            pollingRecord.setAgeGroup40To50(pollingRecord.getAgeGroup40To50() + 1);
        } else if (voterAge >= 50 && voterAge < 60) {
            pollingRecord.setAgeGroup50To60(pollingRecord.getAgeGroup50To60() + 1);
        } else if (voterAge >= 60 && voterAge < 70) {
            pollingRecord.setAgeGroup60To70(pollingRecord.getAgeGroup60To70() + 1);
        }

        // Update overall count
        pollingRecord.setOverallPolledCount(pollingRecord.getOverallPolledCount() + 1);
        }
        pollingRecord.setTimestamp(LocalDateTime.now());
        log.info("end of updateVoterAgeGroupCount:election id:{},voter age:{}", electionId,voterAges);
        // Save back to Redis
        pollingAgeWiseRedisRepo.saveOrUpdate(pollingRecord);
        
     // Update the NOT_VOTED_VOTERS count
        updateNotVotedVotersCount(electionId, accountId);
        log.info("end of updateVoterAgeGroupCount:after update in redis:election id:{},voter age:{}", electionId,voterAges);
    }
    
    
    private void updateNotVotedVotersCount(Long electionId, Long accountId) {
        log.info("Updating NOT_VOTED_VOTERS count for electionId={}", electionId);

        // Retrieve total voter counts from PollingBasedOnAgeEntity
       Optional<ElectionDashboardOverviewEntity> optionalpollingBasedOnAgeEntity = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId);
       ElectionDashboardOverviewEntity pollingBasedOnAgeEntity = optionalpollingBasedOnAgeEntity.get();
       if (pollingBasedOnAgeEntity == null) {
            log.warn("No PollingBasedOnAgeEntity found for electionId={}, accountId={}", electionId, accountId);
            return;
        }

        // Retrieve or create NOT_VOTED_VOTERS record
        PollingAgeWiseRedis notVotedRecord = pollingAgeWiseRedisRepo.findByElectionIdAndVoteCountType(electionId, VoteCountType.NOT_VOTED_VOTERS);
        if (notVotedRecord == null) {
            notVotedRecord = new PollingAgeWiseRedis();
            String compositeId = electionId + "_" + accountId + "_" + VoteCountType.NOT_VOTED_VOTERS.name(); // Combine electionId, accountId, and voteCountType
            notVotedRecord.setId(compositeId);
            notVotedRecord.setElectionId(electionId);
            notVotedRecord.setAccountId(accountId);
            notVotedRecord.setVoteCountType(VoteCountType.NOT_VOTED_VOTERS.getValue());
            notVotedRecord.setAgeGroup18To30(pollingBasedOnAgeEntity.getAgeGroup18To30());
            notVotedRecord.setAgeGroup30To40(pollingBasedOnAgeEntity.getAgeGroup30To40());
            notVotedRecord.setAgeGroup40To50(pollingBasedOnAgeEntity.getAgeGroup40To50());
            notVotedRecord.setAgeGroup50To60(pollingBasedOnAgeEntity.getAgeGroup50To60());
            notVotedRecord.setAgeGroup60To70(pollingBasedOnAgeEntity.getAgeGroup60To70());
            notVotedRecord.setOverallPolledCount(pollingBasedOnAgeEntity.getTotalVoters());
        }

        // Retrieve the TOTAL_VOTERS record from Redis
        PollingAgeWiseRedis totalVotersRecord = pollingAgeWiseRedisRepo.findByElectionIdAndVoteCountType(electionId, VoteCountType.TOTAL_VOTERS);
        if (totalVotersRecord == null) {
            log.warn("No TOTAL_VOTERS record found for electionId={}", electionId);
            return;
        }
        
     // Calculate remaining voters for each age group
        notVotedRecord.setAgeGroup18To30(
            pollingBasedOnAgeEntity.getAgeGroup18To30() - totalVotersRecord.getAgeGroup18To30()
        );
        notVotedRecord.setAgeGroup30To40(
            pollingBasedOnAgeEntity.getAgeGroup30To40() - totalVotersRecord.getAgeGroup30To40()
        );
        notVotedRecord.setAgeGroup40To50(
            pollingBasedOnAgeEntity.getAgeGroup40To50() - totalVotersRecord.getAgeGroup40To50()
        );
        notVotedRecord.setAgeGroup50To60(
            pollingBasedOnAgeEntity.getAgeGroup50To60() - totalVotersRecord.getAgeGroup50To60()
        );
        notVotedRecord.setAgeGroup60To70(
            pollingBasedOnAgeEntity.getAgeGroup60To70() - totalVotersRecord.getAgeGroup60To70()
        );

        // Calculate overall remaining voters
        notVotedRecord.setOverallPolledCount(
            pollingBasedOnAgeEntity.getTotalVoters() - totalVotersRecord.getOverallPolledCount()
        );
        notVotedRecord.setTimestamp(LocalDateTime.now());

        // Save back to Redis
        pollingAgeWiseRedisRepo.saveOrUpdate(notVotedRecord);
    }

//    public PollingAgeWiseResponse getPollingAgeWiseData(Long electionId) {
//    	Long accountId = requestDetails.getCurrentAccountId();
//	    if (accountId == null) {
//	        log.error("Account id not found, unauthorized access.");
//	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//	    }
//    	
//        log.info("Fetching PollingAgeWise data for electionId={}, accountId={}", electionId, accountId);
//
//        // Fetch all PollingAgeWiseRedis records for the electionId
//        List<PollingAgeWiseRedis> pollingRecords = pollingAgeWiseRedisRepo.findByElectionIdAndAccountId(electionId,accountId);
//        if (pollingRecords.isEmpty()) {
//            throw new RuntimeException("No PollingAgeWiseRedis records found for electionId=" + electionId);
//        }
//
//        // Fetch total voters from PollingBasedOnAgeEntity
//       Optional<ElectionDashboardOverviewEntity> optionalAgeEntity = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId);
//       ElectionDashboardOverviewEntity ageEntity = optionalAgeEntity.get();
//        if (ageEntity == null) {
//            throw new RuntimeException("PollingBasedOnAgeEntity not found for electionId=" + electionId);
//        }
//
//        // Calculate the overall polled percentage
//        PollingAgeWiseRedis totalVotersRecord = pollingRecords.stream()
//                .filter(record -> record.getVoteCountType() == VoteCountType.TOTAL_VOTERS.getValue())
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("TOTAL_VOTERS record not found in PollingAgeWiseRedis"));
//
//        long totalVoters = ageEntity.getTotalVoters();
//        long polledVoters = totalVotersRecord.getOverallPolledCount();
//        double overallPolledPercentage = ((double) polledVoters / totalVoters) * 100;
//
//        // Construct response
//        PollingAgeWiseResponse response = new PollingAgeWiseResponse();
//        response.setPollingAgeWiseRecords(pollingRecords);
//        response.setOverallPolledPercentage(overallPolledPercentage);
//
//        return response;
//    }
    
//    public PollingAgeWiseResponse getPollingAgeWiseData(Long electionId) {
//        Long accountId = requestDetails.getCurrentAccountId();
//        if (accountId == null) {
//            log.error("Account id not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        log.info("Fetching PollingAgeWise data for electionId={}, accountId={}", electionId, accountId);
//
//        List<PollingAgeWiseRedis> pollingRecords = pollingAgeWiseRedisRepo.findByElectionIdAndAccountId(electionId, accountId);
//        if (pollingRecords.isEmpty()) {
//            log.warn("No PollingAgeWiseRedis records found for electionId={}. Triggering update.", electionId);
//            pollingAgeWiseService.updatePollingAgeWiseData(electionId, accountId);
//            pollingRecords = pollingAgeWiseRedisRepo.findByElectionIdAndAccountId(electionId, accountId);
//            if (pollingRecords.isEmpty()) {
//                log.error("Still no polling data after update for electionId={}", electionId);
//                throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "No polling data available for electionId=" + electionId);
//            }
//        }
//
//        Optional<ElectionDashboardOverviewEntity> optionalAgeEntity = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId);
//        ElectionDashboardOverviewEntity ageEntity = optionalAgeEntity.orElseThrow(() ->
//            new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "ElectionDashboardOverviewEntity not found for electionId=" + electionId));
//
//        PollingAgeWiseRedis totalVotersRecord = pollingRecords.stream()
//                .filter(record -> record.getVoteCountType() == VoteCountType.TOTAL_VOTERS.getValue())
//                .findFirst()
//                .orElseThrow(() -> new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "TOTAL_VOTERS record not found"));
//
//        long totalVoters = ageEntity.getTotalVoters();
//        long polledVoters = totalVotersRecord.getOverallPolledCount();
//        double overallPolledPercentage = totalVoters > 0 ? ((double) polledVoters / totalVoters) * 100 : 0.0;
//
//        PollingAgeWiseResponse response = new PollingAgeWiseResponse();
//        response.setPollingAgeWiseRecords(pollingRecords);
//        response.setOverallPolledPercentage(overallPolledPercentage);
//
//        log.info("Successfully fetched PollingAgeWise data for electionId={}", electionId);
//        return response;
//    }
    
    public PollingAgeWiseResponse getPollingAgeWiseData(Long electionId) {
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        log.info("Fetching PollingAgeWise data for electionId={}, accountId={}", electionId, accountId);

        List<PollingAgeWiseEntity> pollingRecords = pollingAgeWiseRepository.findByElectionIdAndAccountId(electionId, accountId);
        if (pollingRecords.isEmpty()) {
            log.warn("No PollingAgeWiseEntity records found for electionId={}. Triggering update.", electionId);
            pollingAgeWiseService.updatePollingAgeWiseData(electionId, accountId);
            pollingRecords = pollingAgeWiseRepository.findByElectionIdAndAccountId(electionId, accountId);
            if (pollingRecords.isEmpty()) {
                log.error("Still no polling data after update for electionId={}", electionId);
                throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "No polling data available for electionId=" + electionId);
            }
        }

        Optional<ElectionDashboardOverviewEntity> optionalAgeEntity = electionDashboardOverviewRepository.findByElectionIdAndAccountId(electionId, accountId);
        ElectionDashboardOverviewEntity ageEntity = optionalAgeEntity.orElseThrow(() ->
            new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "ElectionDashboardOverviewEntity not found for electionId=" + electionId));

        PollingAgeWiseEntity totalVotersRecord = pollingRecords.stream()
                .filter(record -> record.getVoteCountType() == VoteCountType.TOTAL_VOTERS.getValue())
                .findFirst()
                .orElseThrow(() -> new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "TOTAL_VOTERS record not found"));

        long totalVoters = ageEntity.getTotalVoters();
        long polledVoters = totalVotersRecord.getOverallPolledCount();
        double overallPolledPercentage = totalVoters > 0 ? ((double) polledVoters / totalVoters) * 100 : 0.0;

        PollingAgeWiseResponse response = new PollingAgeWiseResponse();
        response.setPollingAgeWiseRecords(pollingRecords);
        response.setOverallPolledPercentage(overallPolledPercentage);

        log.info("Successfully fetched PollingAgeWise data for electionId={}", electionId);
        return response;
    }
           
    public void saveVoterPartyReport(Long electionId, Long accountId, List<Long> partyIds) {
    	log.info("inside saveVoterPartyReport:election id:{},party ids:{},account id:{}", electionId,partyIds,accountId);
    	if (partyIds == null || partyIds.isEmpty()) {
            return; // Do nothing if the party list is null or empty
        }
    	
    	
    	for (Long partyId : partyIds) {
            if (partyId == null) {
                continue; // Skip if a party ID is null
            }

        // Retrieve the existing record for this electionId, accountId, and partyId
        Optional<VoterPartyReportEntity> existingRecordOpt = voterPartyReportRepository.findByElectionIdAndAccountIdAndPartyId(electionId, accountId, partyId);
        VoterPartyReportEntity voterPartyReport; 

        if (existingRecordOpt.isPresent()) {
            // Update the voter count if the record exists
            voterPartyReport = existingRecordOpt.get();
            voterPartyReport.setVotersCount(voterPartyReport.getVotersCount() + 1);
        } else {
            // Create a new record if it doesn't exist
            voterPartyReport = new VoterPartyReportEntity();
            voterPartyReport.setElectionId(electionId);
            voterPartyReport.setAccountId(accountId);
            voterPartyReport.setPartyId(partyId);
            voterPartyReport.setVotersCount(1); // Initial voter count
        }

        // Save to database
        voterPartyReportRepository.save(voterPartyReport);
    }

    }


	public PartyWiseVotersResponse getPollingBasedParty(Long electionId) {
		log.info("inside getPollingBasedParty:election id:{}", electionId);
		Long accountId = requestDetails.getCurrentAccountId();
	    if (accountId == null) {
	        log.error("Account id not found, unauthorized access.");
	        throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
	    }
	    
	    // Get party-wise voter counts from voter_party_report table
		List<VoterPartyReportEntity> voterPartyList = voterPartyReportRepository.findByElectionIdAndAccountId(electionId, accountId);
		
		// Fetch all parties for this election to get names
		List<Party> parties = partyRepository.findByElectionIdAndAccountId(electionId, accountId);
		Map<Long, String> partyIdToNameMap = parties.stream()
		    .collect(Collectors.toMap(Party::getId, Party::getPartyName));
		
		// Build the party voter count list with names
		List<PartyVoterCount> partyVoterCounts = voterPartyList.stream()
		    .map(voterParty -> {
		        String partyName = partyIdToNameMap.getOrDefault(voterParty.getPartyId(), "Unknown Party");
		        return new PartyVoterCount(partyName, voterParty.getVotersCount());
		    })
		    .collect(Collectors.toList());
		
		// Calculate total voters in the election (unfiltered)
		long totalVoters = voterRepo.countByAccountIdAndElectionId(accountId, electionId);
		
		// Calculate neutral voters (voters with no party affiliation)
		int partyVotersSum = voterPartyList.stream()
		    .mapToInt(VoterPartyReportEntity::getVotersCount)
		    .sum();
		int neutralVotersCount = (int) (totalVoters - partyVotersSum);
		
		// Add neutral voters to the list if there are any
		if (neutralVotersCount > 0) {
		    partyVoterCounts.add(new PartyVoterCount("NEUTRAL VOTERS", neutralVotersCount));
		}
		
		log.info("Total voters: {}, Party voters: {}, Neutral voters: {}", totalVoters, partyVotersSum, neutralVotersCount);
		
		return new PartyWiseVotersResponse(partyVoterCounts, totalVoters);
	}
	
	public void recordVoteTime(Long accountId,Long electionId, List<VoterVoteDetailsRequest> voterVoteDetailsRequest) {
		log.info("inside recordVoteTime:election id:{}", electionId);
		
		for(VoterVoteDetailsRequest vote:voterVoteDetailsRequest) {
		boothWiseTimingVotersCountRedisRepo.saveOrUpdateBoothWiseTimingVotersCount(accountId,electionId, vote.getBoothNumber(),vote.getVoteTimestamp());
		}
    }


    public List<CadrePerformanceDto> getAllUsersPerformance(Long accountId) {
        log.info("Inside getAllUsersPerformance: accountId: {}", accountId);
        
        // Fetch all users for the account
        List<UserEntity> users = userRepo.findByAccountIdOrderById(accountId);
        log.info("Fetched {} users for accountId: {}", users.size(), accountId);
        
        // Fetch all performance records for the account
        List<CadrePerformanceDto> performanceRecords = volunteerVsVoterReportRepository
                .findByAccountIdOrderByUserId(accountId);
        log.info("Fetched {} performance records for accountId: {}", performanceRecords.size(), accountId);
        
        // Log performance records for debugging
        performanceRecords.forEach(record -> 
            log.info("Performance record: userId={}, totalVoterCreated={}", 
                     record.getUserId(), record.getTotalVoterCreated()));

        // Map users to performance data, including users with no performance records
        List<CadrePerformanceDto> result = users.stream()
                .map(user -> {
                    // Find matching performance record for the user
                    CadrePerformanceDto performance = performanceRecords.stream()
                            .filter(record -> record.getUserId() != null && record.getUserId().equals(user.getId()))
                            .findFirst()
                            .orElse(new CadrePerformanceDto(user.getId(), 0L));
                    
                    // Construct user name
                    String userName = user.getFirstName() + " " + 
                            (user.getLastName() != null ? user.getLastName() : "");
                    return new CadrePerformanceDto(user.getId(), userName, performance.getTotalVoterCreated());
                })
                .collect(Collectors.toList());

        // Add performance records for users not in UserEntity (if any)
        performanceRecords.stream()
                .filter(record -> record.getUserId() != null && 
                        users.stream().noneMatch(user -> user.getId().equals(record.getUserId())))
                .forEach(record -> {
                    String userName = "Unknown User (ID: " + record.getUserId() + ")";
                    result.add(new CadrePerformanceDto(record.getUserId(), userName, record.getTotalVoterCreated()));
                });

        log.info("End of getAllUsersPerformance: accountId: {}, records returned: {}", accountId, result.size());
        return result;
    }
    
    @Transactional
    public boolean saveOrUpdateCadreReport(Long electionId, CadreReportDTO dto) {
        log.info("Processing cadre report for electionId: {}", electionId);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

       // Long electionId = Long.valueOf(dto.getElectionId());
        // Check for existing report
        Optional<CadreReportEntity> existingReport = cadreReportRepository
                .findByElectionIdAndAccountId(electionId, accountId)
                .stream()
                .findFirst(); // Assuming one report per electionId and accountId

        CadreReportEntity entity;
        boolean isCreated = false;

        if (existingReport.isPresent()) {
            // Update existing report
            entity = existingReport.get();
            log.info("Updating existing cadre report for electionId: {}", electionId);
        } else {
            // Create new report
            entity = new CadreReportEntity();
            entity.setElectionId(electionId);
            entity.setAccountId(accountId);
            isCreated = true;
            log.info("Creating new cadre report for electionId: {}", electionId);
        }

        // Update fields
        entity.setActiveTab(dto.getActiveTab());
        entity.setSelectedBooth(dto.getSelectedBooth());

        // Map tabs
        if (dto.getTabs() != null) {
            entity.setTopPerformers(dto.getTabs().getTopPerformers());
            entity.setLowPerformers(dto.getTabs().getLowPerformers());
            entity.setActivity(dto.getTabs().getActivity());
            entity.setDemographics(dto.getTabs().getDemographics());
            entity.setUpdates(dto.getTabs().getUpdates());
        }

        cadreReportRepository.save(entity);
        log.info("Successfully processed cadre report for electionId: {}", electionId);
        return isCreated;
    }

    public List<CadreReportDTO> getCadreReports(Long electionId) {
        log.info("Fetching cadre reports for electionId: {}", electionId);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        List<CadreReportEntity> entities = cadreReportRepository.findByElectionIdAndAccountId(electionId, accountId);
        if (entities.isEmpty()) {
            log.warn("No cadre reports found for electionId: {}", electionId);
            throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
        }

        return entities.stream().map(entity -> {
            CadreReportDTO dto = new CadreReportDTO();
            dto.setElectionId(String.valueOf(entity.getElectionId()));
            dto.setActiveTab(entity.getActiveTab());
            dto.setSelectedBooth(entity.getSelectedBooth());

            CadreReportDTO.Tabs tabs = new CadreReportDTO.Tabs();
            tabs.setTopPerformers(entity.getTopPerformers());
            tabs.setLowPerformers(entity.getLowPerformers());
            tabs.setActivity(entity.getActivity());
            tabs.setDemographics(entity.getDemographics());
            tabs.setUpdates(entity.getUpdates());
            dto.setTabs(tabs);

            return dto;
        }).collect(Collectors.toList());
    }
    
    @Transactional
    public boolean saveOrUpdatePollDayReport(Long electionId, PollDayReportDTO dto) {
        log.info("Processing poll day report for electionId: {}", electionId);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        Optional<PollDayReportEntity> existingReport = pollDayReportRepository
                .findByElectionIdAndAccountId(electionId, accountId)
                .stream()
                .filter(report -> dto.getBoothNumber() == null ? report.getBoothNumber() == null :
                        dto.getBoothNumber().equals(report.getBoothNumber()))
                .findFirst();

        PollDayReportEntity entity;
        boolean isCreated = false;

        if (existingReport.isPresent()) {
            entity = existingReport.get();
            log.info("Updating existing poll day report for electionId: {}, boothNumber: {}", electionId, dto.getBoothNumber());
        } else {
            entity = new PollDayReportEntity();
            entity.setElectionId(electionId);
            entity.setAccountId(accountId);
            isCreated = true;
            log.info("Creating new poll day report for electionId: {}, boothNumber: {}", electionId, dto.getBoothNumber());
        }

        entity.setBoothNumber(dto.getBoothNumber());
        entity.setActiveTab(dto.getActiveTab());

        if (dto.getTabs() != null) {
            entity.setVote(dto.getTabs().getVote());
            entity.setPerformance(dto.getTabs().getPerformance());
            entity.setDemographics(dto.getTabs().getDemographics());
            entity.setTiming(dto.getTabs().getTiming());
        }

        pollDayReportRepository.save(entity);
        log.info("Successfully processed poll day report for electionId: {}", electionId);
        return isCreated;
    }


    public List<PollDayReportDTO> getPollDayReports(Long electionId) {
        log.info("Fetching poll day reports for electionId: {}", electionId);

        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }

        List<PollDayReportEntity> entities = pollDayReportRepository.findByElectionIdAndAccountId(electionId, accountId);
        if (entities.isEmpty()) {
            log.warn("No poll day reports found for electionId: {}", electionId);
            throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
        }

        return entities.stream().map(entity -> {
            PollDayReportDTO dto = new PollDayReportDTO();
            dto.setElectionId(String.valueOf(entity.getElectionId()));
            dto.setBoothNumber(entity.getBoothNumber());
            dto.setActiveTab(entity.getActiveTab());

            PollDayReportDTO.Tabs tabs = new PollDayReportDTO.Tabs();
            tabs.setVote(entity.getVote());
            tabs.setPerformance(entity.getPerformance());
            tabs.setDemographics(entity.getDemographics());
            tabs.setTiming(entity.getTiming());
            dto.setTabs(tabs);

            return dto;
        }).collect(Collectors.toList());
    }
    
//    @Transactional
//    public boolean saveOrUpdateElectionDashboardReport(Long electionId, ElectionDashboardDTO dto) {
//        log.info("Processing election dashboard report for electionId: {}", electionId);
//
//        Long userId = requestDetails.getCurrentAccountId();
//        if (userId == null) {
//            log.error("User ID not found, unauthorized access.");
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        Optional<ElectionDashboardEntity> existingReport = electionDashboardRepository
//                .findByElectionIdAndUserId(electionId, userId)
//                .stream()
//                .findFirst();
//
//        ElectionDashboardEntity entity;
//        boolean isCreated = false;
//
//        if (existingReport.isPresent()) {
//            entity = existingReport.get();
//            log.info("Updating existing election dashboard report for electionId: {}", electionId);
//        } else {
//            entity = new ElectionDashboardEntity();
//            entity.setElectionId(electionId);
//            entity.setUserId(userId);
//            isCreated = true;
//            log.info("Creating new election dashboard report for electionId: {}", electionId);
//        }
//
//        entity.setActiveTab(dto.getActiveTab());
//        entity.setBoothNumber(dto.getBoothNumber());
//
//        if (dto.getTabs() != null) {
//            entity.setDemographics(dto.getTabs().getDemographics());
//            entity.setIssues(dto.getTabs().getIssues());
//            entity.setReligion(dto.getTabs().getReligion());
//            entity.setCaste(dto.getTabs().getCaste());
//            entity.setSubcaste(dto.getTabs().getSubcaste());
//            entity.setLanguages(dto.getTabs().getLanguages());
//            entity.setParty(dto.getTabs().getParty());
//            entity.setScheme(dto.getTabs().getScheme());
//            entity.setHistory(dto.getTabs().getHistory());
//            entity.setAvailability(dto.getTabs().getAvailability());
//        }
//
//        electionDashboardRepository.save(entity);
//        log.info("Successfully processed election dashboard report for electionId: {}", electionId);
//        return isCreated;
//    }
//
//    public List<ElectionDashboardDTO> getElectionDashboardReports(Long electionId, Long userId) {
//        log.info("Fetching election dashboard reports for electionId: {}, userId: {}", electionId, userId);
//
//        Long currentUserId = requestDetails.getCurrentAccountId();
//        if (currentUserId == null || !currentUserId.equals(userId)) {
//            log.error("Unauthorized access for userId: {}", userId);
//            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
//        }
//
//        List<ElectionDashboardEntity> entities = electionDashboardRepository.findByElectionIdAndUserId(electionId, userId);
//        if (entities.isEmpty()) {
//            log.warn("No election dashboard reports found for electionId: {}, userId: {}", electionId, userId);
//            throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
//        }
//
//        return entities.stream().map(entity -> {
//            ElectionDashboardDTO dto = new ElectionDashboardDTO();
//            dto.setUserId(String.valueOf(entity.getUserId()));
//            dto.setElectionId(String.valueOf(entity.getElectionId()));
//            dto.setBoothNumber(entity.getBoothNumber());
//            dto.setActiveTab(entity.getActiveTab());
//
//            ElectionDashboardDTO.Tabs tabs = new ElectionDashboardDTO.Tabs();
//            tabs.setDemographics(entity.getDemographics());
//            tabs.setIssues(entity.getIssues());
//            tabs.setReligion(entity.getReligion());
//            tabs.setCaste(entity.getCaste());
//            tabs.setSubcaste(entity.getSubcaste());
//            tabs.setLanguages(entity.getLanguages());
//            tabs.setParty(entity.getParty());
//            tabs.setScheme(entity.getScheme());
//            tabs.setHistory(entity.getHistory());
//            tabs.setAvailability(entity.getAvailability());
//            dto.setTabs(tabs);
//
//            return dto;
//        }).collect(Collectors.toList());
//    }
    @Transactional
    public boolean saveOrUpdateElectionDashboardReport(Long electionId, ElectionDashboardDTO dto) {
        log.info("Processing election dashboard report for electionId: {}", electionId);

        // Normalize boothNumber (null if empty or blank)
        String normalizedBoothNumber = dto.getBoothNumber() != null && !dto.getBoothNumber().trim().isEmpty() 
            ? dto.getBoothNumber().trim().toUpperCase() 
            : null;

        log.info("Received boothNumber: {}, normalized: {}", dto.getBoothNumber(), normalizedBoothNumber);

        // Check for existing record by electionId
        Optional<ElectionDashboardEntity> existingReport = electionDashboardRepository.findByElectionId(electionId);

        // Prevent multiple records
        long count = electionDashboardRepository.countByElectionId(electionId);
        if (count > 1) {
            log.error("Multiple records found for electionId: {}", electionId);
            throw new ThedalException(ThedalError.INVALID_REQUEST, HttpStatus.BAD_REQUEST, 
                                     "Multiple records exist for electionId: " + electionId);
        }

        ElectionDashboardEntity entity;
        boolean isCreated = false;

        if (existingReport.isPresent()) {
            entity = existingReport.get();
            log.info("Updating existing election dashboard report with ID: {}, electionId: {}, boothNumber: {}", 
                    entity.getId(), electionId, entity.getBoothNumber());
        } else {
            entity = new ElectionDashboardEntity();
            entity.setElectionId(electionId);
            isCreated = true;
            log.info("Creating new election dashboard report for electionId: {}", electionId);
        }

        // Update boothNumber if provided
        entity.setBoothNumber(normalizedBoothNumber);
        entity.setActiveTab(dto.getActiveTab());

        if (dto.getTabs() != null) {
            entity.setDemographics(dto.getTabs().getDemographics());
            entity.setIssues(dto.getTabs().getIssues());
            entity.setReligion(dto.getTabs().getReligion());
            entity.setCaste(dto.getTabs().getCaste());
            entity.setSubcaste(dto.getTabs().getSubcaste());
            entity.setLanguages(dto.getTabs().getLanguages());
            entity.setParty(dto.getTabs().getParty());
            entity.setScheme(dto.getTabs().getScheme());
            entity.setHistory(dto.getTabs().getHistory());
            entity.setAvailability(dto.getTabs().getAvailability());
        }

        electionDashboardRepository.save(entity);
        log.info("Successfully processed election dashboard report for electionId: {}", electionId);
        return isCreated;
    }
    
    public List<ElectionDashboardDTO> getElectionDashboardReports(Long electionId, String boothNumber) {
        log.info("Fetching election dashboard reports for electionId: {}, boothNumber: {}", electionId, boothNumber);

        Optional<ElectionDashboardEntity> entities;
        if (boothNumber != null && !boothNumber.isEmpty()) {
            entities = Optional.empty();
        } else {
            entities = electionDashboardRepository.findByElectionId(electionId);
            if (entities.isEmpty()) {
                log.warn("No election dashboard reports found for electionId: {}", electionId);
                throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND);
            }
        }

        return entities.stream().map(entity -> {
            ElectionDashboardDTO dto = new ElectionDashboardDTO();
            dto.setElectionId(String.valueOf(entity.getElectionId()));
            dto.setBoothNumber(entity.getBoothNumber());
            dto.setActiveTab(entity.getActiveTab());

            ElectionDashboardDTO.Tabs tabs = new ElectionDashboardDTO.Tabs();
            tabs.setDemographics(entity.getDemographics());
            tabs.setIssues(entity.getIssues());
            tabs.setReligion(entity.getReligion());
            tabs.setCaste(entity.getCaste());
            tabs.setSubcaste(entity.getSubcaste());
            tabs.setLanguages(entity.getLanguages());
            tabs.setParty(entity.getParty());
            tabs.setScheme(entity.getScheme());
            tabs.setHistory(entity.getHistory());
            tabs.setAvailability(entity.getAvailability());
            dto.setTabs(tabs);

            return dto;
        }).collect(Collectors.toList());
    }
    
    /**
     * Get booth strength overview based on default party performance
     * Classifies booths as STRONG, SWING, or WEAK based on default party's rank in each booth
     */
    public BoothStrengthOverviewDTO getBoothStrengthOverview(Long electionId) {
        log.info("Fetching booth strength overview for electionId: {}", electionId);
        
        Long accountId = requestDetails.getCurrentAccountId();
        if (accountId == null) {
            log.error("Account id not found, unauthorized access.");
            throw new ThedalException(ThedalError.ACCOUNT_ID_NOT_CREATED, HttpStatus.UNAUTHORIZED);
        }
        
        // Get election and default party
        ElectionEntity election = electionRepository.findByIdAndAccountId(electionId, accountId)
            .orElseThrow(() -> new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "Election not found"));
        
        Long defaultPartyId = election.getDefaultPartyId();
        if (defaultPartyId == null) {
            log.warn("No default party set for electionId: {}", electionId);
            throw new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.BAD_REQUEST, "No default party set for this election");
        }
        
        // Get default party details
        Party defaultParty = partyRepository.findById(defaultPartyId)
            .orElseThrow(() -> new ThedalException(ThedalError.NO_DATA_FOUND, HttpStatus.NOT_FOUND, "Default party not found"));
        
        // Get booth-wise party rankings
        List<Object[]> rankings = voterRepo.getBoothPartyRankings(accountId, electionId);
        
        // Classify booths by strength
        List<Integer> strongBooths = new ArrayList<>();
        List<Integer> swingBooths = new ArrayList<>();
        List<Integer> weakBooths = new ArrayList<>();
        
        // Group rankings by booth and find default party's rank in each booth
        Map<Integer, Integer> boothDefaultPartyRank = new HashMap<>();
        
        for (Object[] row : rankings) {
            Integer boothNumber = (Integer) row[0];
            Long partyId = ((Number) row[1]).longValue();
            Integer rank = ((Number) row[3]).intValue();
            
            // If this is the default party, store its rank
            if (partyId.equals(defaultPartyId)) {
                boothDefaultPartyRank.put(boothNumber, rank);
            }
        }
        
        // Get all booths in the election (including those without party data)
        List<Integer> allBooths = voterRepo.findDistinctBoothNumbersByAccountIdAndElectionId(accountId, electionId);
        
        // Classify each booth
        for (Integer boothNumber : allBooths) {
            Integer rank = boothDefaultPartyRank.get(boothNumber);
            
            if (rank == null) {
                // Default party not present in this booth - WEAK
                weakBooths.add(boothNumber);
            } else if (rank == 1) {
                // Rank 1 - STRONG
                strongBooths.add(boothNumber);
            } else if (rank == 2 || rank == 3) {
                // Rank 2 or 3 - SWING
                swingBooths.add(boothNumber);
            } else {
                // Rank 4+ - WEAK
                weakBooths.add(boothNumber);
            }
        }
        
        // Sort booth lists
        strongBooths.sort(Integer::compareTo);
        swingBooths.sort(Integer::compareTo);
        weakBooths.sort(Integer::compareTo);
        
        log.info("Booth classification complete. Strong: {}, Swing: {}, Weak: {}", 
                 strongBooths.size(), swingBooths.size(), weakBooths.size());
        
        BoothStrengthOverviewDTO response = new BoothStrengthOverviewDTO();
        response.setElectionId(electionId);
        response.setDefaultPartyId(defaultPartyId);
        response.setDefaultPartyName(defaultParty.getPartyName());
        response.setStrongBooths(strongBooths);
        response.setSwingBooths(swingBooths);
        response.setWeakBooths(weakBooths);
        
        return response;
    }
	
}