package com.thedal.thedal_app.report.pollday;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/redis")
public class RedisTestController {

	
	 @Autowired
	    private RedisTemplate<String, Object> redisTemplate;

	    @GetMapping("/set")
	    public String setValue() {
	        redisTemplate.opsForValue().set("testKey", "Hello Redis Cloud");
	        return "Value Set";
	    }

	    @GetMapping("/get")
	    public String getValue() {
	        return (String) redisTemplate.opsForValue().get("testKey");
	    }
	
//    private final RedisTestService redisTestService;
//
//    public RedisTestController(RedisTestService redisTestService) {
//        this.redisTestService = redisTestService;
//    }
//
//    @GetMapping("/test-redis")
//    public String testRedis() {
//        return redisTestService.testConnection();
//    }
	
}