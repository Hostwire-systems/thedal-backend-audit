package com.thedal.thedal_app.report;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.thedal.thedal_app.report.pollday.PollingAgeWiseRedis;
import com.thedal.thedal_app.report.pollday.PollingAgeWiseRedisRepo;
import com.thedal.thedal_app.report.pollday.VoteCountType;
import com.thedal.thedal_app.voter.VoterEntity;
import com.thedal.thedal_app.voter.VoterRepo;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PollingAgeWiseService {

    @Autowired
    private VoterRepo voterRepository;
    
    @Autowired
    private PollingAgeWiseRepository pollingAgeWiseRepository;

    public void updatePollingAgeWiseData(Long electionId, Long accountId) {
        // Fetch voters
        List<VoterEntity> voters = voterRepository.findByElectionIdAndAccountId(electionId, accountId);

        // Initialize counts
        long[] ageGroupsVoted = new long[5]; // 18-30, 30-40, 40-50, 50-60, 60-70
        long[] ageGroupsNotVoted = new long[5];
        long polledCount = 0;
        long invalidAgeCount = 0;

        for (VoterEntity voter : voters) {
            Integer age = voter.getAge();
            if (age == null || age < 18) {
                invalidAgeCount++; // Track invalid ages (e.g., 0 or null)
                continue;
            }
            int index = getAgeGroupIndex(age);
            if (index >= 0) {
                if (Boolean.TRUE.equals(voter.getHasVoted())) {
                    ageGroupsVoted[index]++;
                    polledCount++;
                } else {
                    ageGroupsNotVoted[index]++;
                }
            }
        }

        // Log invalid ages for debugging
        if (invalidAgeCount > 0) {
            log.warn("Found {} voters with invalid age (null or < 18) for electionId={}, accountId={}", 
                     invalidAgeCount, electionId, accountId);
        }

        // Save POLLED_2025 record
        PollingAgeWiseEntity votedRecord = new PollingAgeWiseEntity();
        votedRecord.setElectionId(electionId);
        votedRecord.setAccountId(accountId);
        votedRecord.setVoteCountType(VoteCountType.POLLED_2025.getValue());
        votedRecord.setAgeGroup18To30(ageGroupsVoted[0]);
        votedRecord.setAgeGroup30To40(ageGroupsVoted[1]);
        votedRecord.setAgeGroup40To50(ageGroupsVoted[2]);
        votedRecord.setAgeGroup50To60(ageGroupsVoted[3]);
        votedRecord.setAgeGroup60To70(ageGroupsVoted[4]);
        votedRecord.setOverallPolledCount(polledCount);
        votedRecord.setTimestamp(LocalDateTime.now());
        pollingAgeWiseRepository.save(votedRecord);

        // Save NOT_VOTED_VOTERS record
        PollingAgeWiseEntity notVotedRecord = new PollingAgeWiseEntity();
        notVotedRecord.setElectionId(electionId);
        notVotedRecord.setAccountId(accountId);
        notVotedRecord.setVoteCountType(VoteCountType.NOT_VOTED_VOTERS.getValue());
        notVotedRecord.setAgeGroup18To30(ageGroupsNotVoted[0]);
        notVotedRecord.setAgeGroup30To40(ageGroupsNotVoted[1]);
        notVotedRecord.setAgeGroup40To50(ageGroupsNotVoted[2]);
        notVotedRecord.setAgeGroup50To60(ageGroupsNotVoted[3]);
        notVotedRecord.setAgeGroup60To70(ageGroupsNotVoted[4]);
        notVotedRecord.setOverallPolledCount(voters.size() - polledCount);
        notVotedRecord.setTimestamp(LocalDateTime.now());
        pollingAgeWiseRepository.save(notVotedRecord);

        // Save TOTAL_VOTERS record
        PollingAgeWiseEntity totalRecord = new PollingAgeWiseEntity();
        totalRecord.setElectionId(electionId);
        totalRecord.setAccountId(accountId);
        totalRecord.setVoteCountType(VoteCountType.TOTAL_VOTERS.getValue());
        totalRecord.setAgeGroup18To30(ageGroupsVoted[0] + ageGroupsNotVoted[0]);
        totalRecord.setAgeGroup30To40(ageGroupsVoted[1] + ageGroupsNotVoted[1]);
        totalRecord.setAgeGroup40To50(ageGroupsVoted[2] + ageGroupsNotVoted[2]);
        totalRecord.setAgeGroup50To60(ageGroupsVoted[3] + ageGroupsNotVoted[3]);
        totalRecord.setAgeGroup60To70(ageGroupsVoted[4] + ageGroupsNotVoted[4]);
        totalRecord.setOverallPolledCount(polledCount);
        totalRecord.setTimestamp(LocalDateTime.now());
        pollingAgeWiseRepository.save(totalRecord);
    }

    private int getAgeGroupIndex(int age) {
        if (age >= 18 && age <= 30) return 0;
        if (age > 30 && age <= 40) return 1;
        if (age > 40 && age <= 50) return 2;
        if (age > 50 && age <= 60) return 3;
        if (age > 60 && age <= 70) return 4;
        return -1; // Ignore ages outside defined groups
    }

//    @Autowired
//    private PollingAgeWiseRedisRepo pollingAgeWiseRedisRepo;
//
//    public void updatePollingAgeWiseData(Long electionId, Long accountId) {
//        // Fetch voters
//        List<VoterEntity> voters = voterRepository.findByElectionIdAndAccountId(electionId, accountId);
//
//        // Initialize counts
//        long[] ageGroupsVoted = new long[5]; // 18-30, 30-40, 40-50, 50-60, 60-70
//        long[] ageGroupsNotVoted = new long[5];
//        long polledCount = 0;
//        long invalidAgeCount = 0;
//
//        for (VoterEntity voter : voters) {
//            Integer age = voter.getAge();
//            if (age == null || age < 18) {
//                invalidAgeCount++; // Track invalid ages (e.g., 0 or null)
//                continue;
//            }
//            int index = getAgeGroupIndex(age);
//            if (index >= 0) {
//                if (Boolean.TRUE.equals(voter.getHasVoted())) {
//                    ageGroupsVoted[index]++;
//                    polledCount++;
//                } else {
//                    ageGroupsNotVoted[index]++;
//                }
//            }
//        }
//
//        // Log invalid ages for debugging
//        if (invalidAgeCount > 0) {
//            log.warn("Found {} voters with invalid age (null or < 18) for electionId={}, accountId={}", 
//                     invalidAgeCount, electionId, accountId);
//        }
//
//        // Save POLLED_2025 record
//        PollingAgeWiseRedis votedRecord = new PollingAgeWiseRedis();
//        votedRecord.setId(electionId + "_" + accountId + "_POLLED_2025");
//        votedRecord.setElectionId(electionId);
//        votedRecord.setAccountId(accountId);
//        votedRecord.setVoteCountType(VoteCountType.POLLED_2025.getValue());
//        votedRecord.setAgeGroup18To30(ageGroupsVoted[0]);
//        votedRecord.setAgeGroup30To40(ageGroupsVoted[1]);
//        votedRecord.setAgeGroup40To50(ageGroupsVoted[2]);
//        votedRecord.setAgeGroup50To60(ageGroupsVoted[3]);
//        votedRecord.setAgeGroup60To70(ageGroupsVoted[4]);
//        votedRecord.setOverallPolledCount(polledCount);
//        votedRecord.setTimestamp(LocalDateTime.now());
//        pollingAgeWiseRedisRepo.saveOrUpdate(votedRecord);
//
//        // Save NOT_VOTED_VOTERS record
//        PollingAgeWiseRedis notVotedRecord = new PollingAgeWiseRedis();
//        notVotedRecord.setId(electionId + "_" + accountId + "_NOT_VOTED_VOTERS");
//        notVotedRecord.setElectionId(electionId);
//        notVotedRecord.setAccountId(accountId);
//        notVotedRecord.setVoteCountType(VoteCountType.NOT_VOTED_VOTERS.getValue());
//        notVotedRecord.setAgeGroup18To30(ageGroupsNotVoted[0]);
//        notVotedRecord.setAgeGroup30To40(ageGroupsNotVoted[1]);
//        notVotedRecord.setAgeGroup40To50(ageGroupsNotVoted[2]);
//        notVotedRecord.setAgeGroup50To60(ageGroupsNotVoted[3]);
//        notVotedRecord.setAgeGroup60To70(ageGroupsNotVoted[4]);
//        notVotedRecord.setOverallPolledCount(voters.size() - polledCount);
//        notVotedRecord.setTimestamp(LocalDateTime.now());
//        pollingAgeWiseRedisRepo.saveOrUpdate(notVotedRecord);
//
//        // Save TOTAL_VOTERS record
//        PollingAgeWiseRedis totalRecord = new PollingAgeWiseRedis();
//        totalRecord.setId(electionId + "_" + accountId + "_TOTAL_VOTERS");
//        totalRecord.setElectionId(electionId);
//        totalRecord.setAccountId(accountId);
//        totalRecord.setVoteCountType(VoteCountType.TOTAL_VOTERS.getValue());
//        totalRecord.setAgeGroup18To30(ageGroupsVoted[0] + ageGroupsNotVoted[0]);
//        totalRecord.setAgeGroup30To40(ageGroupsVoted[1] + ageGroupsNotVoted[1]);
//        totalRecord.setAgeGroup40To50(ageGroupsVoted[2] + ageGroupsNotVoted[2]);
//        totalRecord.setAgeGroup50To60(ageGroupsVoted[3] + ageGroupsNotVoted[3]);
//        totalRecord.setAgeGroup60To70(ageGroupsVoted[4] + ageGroupsNotVoted[4]);
//        totalRecord.setOverallPolledCount(polledCount);
//        totalRecord.setTimestamp(LocalDateTime.now());
//        pollingAgeWiseRedisRepo.saveOrUpdate(totalRecord);
//    }
//
//    private int getAgeGroupIndex(int age) {
//        if (age >= 18 && age <= 30) return 0;
//        if (age > 30 && age <= 40) return 1;
//        if (age > 40 && age <= 50) return 2;
//        if (age > 50 && age <= 60) return 3;
//        if (age > 60 && age <= 70) return 4;
//        return -1; // Ignore ages outside defined groups
//    }
    
    
}