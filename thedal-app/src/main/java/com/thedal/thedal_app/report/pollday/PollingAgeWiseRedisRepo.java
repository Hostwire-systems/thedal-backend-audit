package com.thedal.thedal_app.report.pollday;



import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;


@Repository
public class PollingAgeWiseRedisRepo {

	private final HashOperations<String, String, PollingAgeWiseRedis> hashOperations;
    //private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_HASH = "polling_age_wise_redis";
    
    @Autowired
    private RedisTemplate<String, PollingAgeWiseRedis> redisTemplate;

    public PollingAgeWiseRedisRepo(RedisTemplate<String, PollingAgeWiseRedis> redisTemplate) {
      //  this.redisTemplate = redisTemplate;
        this.hashOperations = redisTemplate.opsForHash();
    }
    
    public PollingAgeWiseRedis findByElectionIdAndVoteCountType(Long electionId, VoteCountType voteCountType) {
        List<PollingAgeWiseRedis> records = hashOperations.values(REDIS_HASH);
        return records.stream()
                .filter(record -> record.getElectionId().equals(electionId) && record.getVoteCountType() == voteCountType.getValue())
                .findFirst()
                .orElse(null);
    }

    public void saveOrUpdate(PollingAgeWiseRedis entity) {
        hashOperations.put(REDIS_HASH, entity.getId(), entity);
    }
    
    public List<PollingAgeWiseRedis> findByElectionIdAndAccountId(Long electionId, Long accountId) {
        // Scan all keys matching the pattern
        String pattern = electionId + "_" + accountId + "_";
        return hashOperations.keys(REDIS_HASH).stream()
                .filter(key -> key.startsWith(pattern))
                .map(key -> hashOperations.get(REDIS_HASH, key))
                .collect(Collectors.toList());
    }
}
