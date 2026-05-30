package com.thedal.thedal_app.report.pollday;


import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@RedisHash(value = "polling_age_wise_redis")
public class PollingAgeWiseRedis {

	@Id
	private String id;
	

	private Long electionId;
	
	private Long accountId;
	
	//@Enumerated(EnumType.STRING)
	private Integer voteCountType;

	private long ageGroup18To30;
	
	private long ageGroup30To40;
	
	private long ageGroup40To50;
	
	private long ageGroup50To60;
	
	private long ageGroup60To70;
	
	private long overallPolledCount;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime timestamp;

	
}

