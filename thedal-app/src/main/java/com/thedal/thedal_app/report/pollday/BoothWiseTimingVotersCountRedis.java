package com.thedal.thedal_app.report.pollday;


import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@RedisHash(value = "booth_wise_timing_voters_count_redis")
public class BoothWiseTimingVotersCountRedis {

	@Id
	private String id;
	

	private Long electionId;
	
	private Long accountId;
	

	private Integer boothNumber;
	

	private long time07To08;
	

	private long time08To09;
	

	private long time09To10;
	

	private long time10To11;
	

	private long time11To12;
	

	private long time12To13;
	

	private long time13To14;
	

	private long time14To15;
	

	private long time15To16;
	

	private long time16To17;
	

	private long totalVote;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
	private LocalDateTime timestamp;

}

