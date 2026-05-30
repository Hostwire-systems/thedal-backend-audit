package com.thedal.thedal_app.report.pollday;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisTestService {

//    @Autowired
//    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void saveToRedis(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public Object getFromRedis(String key) {
        return redisTemplate.opsForValue().get(key);
    }


//    public String testConnection() {
//        try {
//            redisTemplate.opsForValue().set("test-key", "connected");
//            return redisTemplate.opsForValue().get("test-key");
//        } catch (Exception e) {
//            return "Redis connection failed: " + e.getMessage();
//        }
//    }
}