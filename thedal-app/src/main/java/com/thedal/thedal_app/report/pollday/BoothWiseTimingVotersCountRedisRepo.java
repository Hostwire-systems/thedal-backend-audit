package com.thedal.thedal_app.report.pollday;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.thedal.thedal_app.report.dto.BoothWiseTimingVotersCountResponseDTO;
import com.thedal.thedal_app.voter.VoterEntity;

import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
public class BoothWiseTimingVotersCountRedisRepo {	
	
	private static final String REDIS_HASH_KEY = "booth_wise_timing_voters_count_redis";
    private final HashOperations<String, String, BoothWiseTimingVotersCountRedis> hashOperations;

    @Autowired
    public BoothWiseTimingVotersCountRedisRepo(RedisTemplate<String, BoothWiseTimingVotersCountRedis> redisTemplate) {
        this.hashOperations = redisTemplate.opsForHash();
    }

    public void saveOrUpdateBoothWiseTimingVotersCount(Long accountId, Long electionId, Integer boothNumber, LocalDateTime votedTimestamp) {
        String key = generateKey(accountId, electionId, boothNumber.longValue());
        BoothWiseTimingVotersCountRedis record = hashOperations.get(REDIS_HASH_KEY, key);

        if (record == null) {
            record = new BoothWiseTimingVotersCountRedis();
            record.setId(key);
            record.setElectionId(electionId);
            record.setAccountId(accountId);
            record.setBoothNumber(boothNumber);
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
            record.setTimestamp(LocalDateTime.now());
        }

        int hour = votedTimestamp.getHour();
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
        } else {
            log.warn("Voted timestamp {} outside valid hours (07:00-17:00) for electionId={}, boothNumber={}", 
                     votedTimestamp, electionId, boothNumber);
            return;
        }

        record.setTotalVote(record.getTotalVote() + 1);
        record.setTimestamp(LocalDateTime.now());
        hashOperations.put(REDIS_HASH_KEY, key, record);
    }

    private String generateKey(Long accountId, Long electionId, Long boothNumber) {
        return accountId + ":" + electionId + ":" + boothNumber;
    }

//    public Optional<BoothWiseTimingVotersCountRedis> getBoothWiseTimingVotersCount(Long accountId, Long electionId, Long boothNumber) {
//        String key = generateKey(accountId, electionId, boothNumber);
//        return Optional.ofNullable(hashOperations.get(REDIS_HASH_KEY, key));
//    }
    public Optional<BoothWiseTimingVotersCountRedis> getBoothWiseTimingVotersCount(Long accountId, Long electionId, Long boothNumber) {
        String key = electionId + "_" + accountId + "_" + boothNumber;
        log.info("Fetching BoothWiseTimingVotersCount for key={}", key);
        BoothWiseTimingVotersCountRedis record = hashOperations.get("booth_wise_timing_voters_count_redis", key);
        if (record == null) {
            log.warn("No BoothWiseTimingVotersCountRedis record found for key={}", key);
        }
        return Optional.ofNullable(record);
    }

    public List<BoothWiseTimingVotersCountResponseDTO> getListOfBoothWiseTimingVotersCountByElectionId(Long accountId, Long electionId) {
        String pattern = accountId + ":" + electionId + ":";
        return hashOperations.keys(REDIS_HASH_KEY).stream()
                .filter(key -> key.startsWith(pattern))
                .map(key -> hashOperations.get(REDIS_HASH_KEY, key))
                .map(record -> new BoothWiseTimingVotersCountResponseDTO(record.getBoothNumber(), record.getTotalVote()))
                .collect(Collectors.toList());
    }

    public void populateBoothWiseTimingVotersCount(Long accountId, Long electionId, List<VoterEntity> voters) {
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

            BoothWiseTimingVotersCountRedis record = new BoothWiseTimingVotersCountRedis();
            String key = generateKey(accountId, electionId, boothNumber.longValue());
            record.setId(key);
            record.setElectionId(electionId);
            record.setAccountId(accountId);
            record.setBoothNumber(boothNumber);
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
                int hour = votedTimestamp.getHour();
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
                } else {
                    log.warn("Voted timestamp {} outside valid hours for voterId={}, electionId={}, boothNumber={}",
                             votedTimestamp, voter.getId(), electionId, boothNumber);
                    continue;
                }
                record.setTotalVote(record.getTotalVote() + 1);
            }

            record.setTimestamp(LocalDateTime.now());
            hashOperations.put(REDIS_HASH_KEY, key, record);
            log.info("Populated BoothWiseTimingVotersCountRedis for electionId={}, boothNumber={}", electionId, boothNumber);
        }
    }

    public void clearBoothWiseTimingVotersCount(Long accountId, Long electionId) {
        String pattern = accountId + ":" + electionId + ":";
        hashOperations.keys(REDIS_HASH_KEY).stream()
                .filter(key -> key.startsWith(pattern))
                .forEach(key -> hashOperations.delete(REDIS_HASH_KEY, key));
        log.info("Cleared BoothWiseTimingVotersCountRedis records for electionId={}, accountId={}", electionId, accountId);
    }
	
//    private static final String REDIS_HASH_KEY = "booth_wise_timing_voters_count_redis";
//	private final HashOperations<String, String, BoothWiseTimingVotersCountRedis> hashOperations;
//    //private RedisTemplate<String, Object> redisTemplate;
//
//	@Autowired
//    public BoothWiseTimingVotersCountRedisRepo(RedisTemplate<String, BoothWiseTimingVotersCountRedis> redisTemplate) {
//        this.hashOperations=redisTemplate.opsForHash();
//        //this.redisTemplate = redisTemplate;
//    }
//    
//    
//    /**
//     * Updates or creates the voter count for a booth and time slot.
//     */
//    public void saveOrUpdateBoothWiseTimingVotersCount(Long accountId,Long electionId, Integer boothNumber, LocalDateTime votedTimestamp) {
//        String key = generateKey(accountId,electionId, boothNumber.longValue());
//        BoothWiseTimingVotersCountRedis record = hashOperations.get(REDIS_HASH_KEY, key);
//
//        if (record == null) {
//            record = new BoothWiseTimingVotersCountRedis();
//            record.setId(key);
//            record.setElectionId(electionId);
//            record.setBoothNumber(boothNumber);
//            record.setTime07To08(0);
//            record.setTime08To09(0);
//            record.setTime09To10(0);
//            record.setTime10To11(0);
//            record.setTime11To12(0);
//            record.setTime12To13(0);
//            record.setTime13To14(0);
//            record.setTime14To15(0);
//            record.setTime15To16(0);
//            record.setTime16To17(0);
//            record.setTotalVote(0);
//            record.setTimestamp(LocalDateTime.now());
//        }
//
//        // Increment the appropriate time slot based on the voted timestamp
//        int hour = votedTimestamp.getHour();
//        if (hour >= 7 && hour < 8) {
//            record.setTime07To08(record.getTime07To08() + 1);
//        } else if (hour >= 8 && hour < 9) {
//            record.setTime08To09(record.getTime08To09() + 1);
//        } else if (hour >= 9 && hour < 10) {
//            record.setTime09To10(record.getTime09To10() + 1);
//        } else if (hour >= 10 && hour < 11) {
//            record.setTime10To11(record.getTime10To11() + 1);
//        } else if (hour >= 11 && hour < 12) {
//            record.setTime11To12(record.getTime11To12() + 1);
//        } else if (hour >= 12 && hour < 13) {
//            record.setTime12To13(record.getTime12To13() + 1);
//        } else if (hour >= 13 && hour < 14) {
//            record.setTime13To14(record.getTime13To14() + 1);
//        } else if (hour >= 14 && hour < 15) {
//            record.setTime14To15(record.getTime14To15() + 1);
//        } else if (hour >= 15 && hour < 16) {
//            record.setTime15To16(record.getTime15To16() + 1);
//        } else if (hour >= 16 && hour < 17) {
//            record.setTime16To17(record.getTime16To17() + 1);
//        }
//
//        // Increment the total vote count
//        record.setTotalVote(record.getTotalVote() + 1);
//        record.setTimestamp(LocalDateTime.now());
//
//        // Save or update in Redis
//        hashOperations.put(REDIS_HASH_KEY, key, record);
//    }
//    
//    /**
//     * Helper method to generate a unique key for Redis.
//     */
//    private String generateKey(Long accountId,Long electionId, Long boothNumber) {
//        return accountId +":"+electionId + ":" + boothNumber;
//    }
//
//    
//    
//    /**
//     * Retrieves voter count information for a specific election ID and booth number.
//     */
//    public Optional<BoothWiseTimingVotersCountRedis> getBoothWiseTimingVotersCount(Long accountId,Long electionId, Long boothNumber) {
//        String key = generateKey(accountId,electionId, boothNumber);
//        BoothWiseTimingVotersCountRedis record = hashOperations.get(REDIS_HASH_KEY, key);
//        return Optional.ofNullable(record);
//    }
//    
//    /**
//     * Retrieves list of voter count information for a specific election ID .
//     */
//    public List<BoothWiseTimingVotersCountResponseDTO> getListOfBoothWiseTimingVotersCountByElectionId(Long accountId,Long electionId) {
////        List<BoothWiseTimingVotersCountRedis> allRecords = hashOperations.values("booth_wise_timing_voters_count_redis");
////        return allRecords.stream()
////                .filter(record -> record.getElectionId().equals(electionId))
////                .map(record -> new BoothWiseTimingVotersCountResponseDTO(record.getBoothNumber(), record.getTotalVote()))
////                .collect(Collectors.toList());
//        
//        String pattern = accountId + ":" + electionId;
//        List<BoothWiseTimingVotersCountRedis> boothWiseRecords = hashOperations.keys(REDIS_HASH_KEY).stream()
//                .filter(key -> key.startsWith(pattern))
//                //.map(key -> new BoothWiseTimingVotersCountResponseDTO(key.getBoothNumber(), key.getTotalVote()))
//                .map(key -> hashOperations.get(REDIS_HASH_KEY, key))
//                .collect(Collectors.toList());
//        
//        return boothWiseRecords.stream()
//              .map(record -> new BoothWiseTimingVotersCountResponseDTO(record.getBoothNumber(), record.getTotalVote()))
//              .collect(Collectors.toList());
//    }
//    
//    
////    /**
////     * Retrieves  voter count based on time ranges for a specific booth number .
////     */
////    public BoothWiseTimingVotersCountRedis getBoothWiseTimingVotersCountByElectionIdAndBoothNumber(Long electionId, Long boothNumber) {
////        List<BoothWiseTimingVotersCountRedis> allRecords = hashOperations.values("booth_wise_timing_voters_count_redis");
////        return allRecords.stream()
////                .filter(record -> record.getElectionId().equals(electionId) && record.getBoothNumber().equals(boothNumber))
////                .findFirst()
////                .orElse(null);
////    }
    
}



