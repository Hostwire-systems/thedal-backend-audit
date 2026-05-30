package com.thedal.thedal_app;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.thedal.thedal_app.report.pollday.BoothWiseTimingVotersCountRedis;
import com.thedal.thedal_app.report.pollday.PollingAgeWiseRedis;

@Configuration
public class RedisConfig {	

	@Value("${spring.redis.host:localhost}")
	private String redisHost;

	@Value("${spring.redis.port:6379}")
	private int redisPort;

	@Value("${spring.redis.username:}")
	private String redisUsername;

	@Value("${spring.redis.password:}")
	private String redisPassword;

	@Value("${spring.redis.ssl:false}")
	private boolean redisSsl;

	
//@Bean
//public LettuceConnectionFactory lettuceConnectionFactory() {
//	return new LettuceConnectionFactory();
//}
	@Bean
	@Primary
	public LettuceConnectionFactory lettuceConnectionFactory() {
	    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
	    config.setHostName(redisHost);
	    config.setPort(redisPort);
	    if (redisUsername != null && !redisUsername.isBlank()) {
	        config.setUsername(redisUsername);
	    }
	    config.setPassword(RedisPassword.of(redisPassword));

	    LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = LettuceClientConfiguration.builder();
	    if (redisSsl) {
	        clientConfigBuilder.useSsl();
	    }
	    LettuceClientConfiguration clientConfig = clientConfigBuilder.build();

	    return new LettuceConnectionFactory(config, clientConfig);
	}
	
//	@Bean
//	@Primary
//	public LettuceConnectionFactory lettuceConnectionFactory() {
//	    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//	    config.setHostName("localhost");
//	    config.setPort(6380);
//	    // No username or password for local Redis
//
//	    return new LettuceConnectionFactory(config);
//	}


    // Add this bean definition
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());
        
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer.setObjectMapper(objectMapper);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer); 

        return redisTemplate;
    }
    
    @Bean
    public RedisTemplate<String, PollingAgeWiseRedis> pollingAgeWiseRedisredis() {
        RedisTemplate<String, PollingAgeWiseRedis> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());
        
        Jackson2JsonRedisSerializer<PollingAgeWiseRedis> serializer = 
            new Jackson2JsonRedisSerializer<>(PollingAgeWiseRedis.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer.setObjectMapper(objectMapper);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer); 

        return redisTemplate;
    }
    
    @Bean
    public RedisTemplate<String, BoothWiseTimingVotersCountRedis> boothWiseTimingVotersCountRedisTemplate() {
        RedisTemplate<String, BoothWiseTimingVotersCountRedis> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory());
        
        Jackson2JsonRedisSerializer<BoothWiseTimingVotersCountRedis> serializer = 
            new Jackson2JsonRedisSerializer<>(BoothWiseTimingVotersCountRedis.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        serializer.setObjectMapper(objectMapper);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(serializer); 

        return redisTemplate;
    }

	
////	@Bean
////	public LettuceConnectionFactory lettuceConnectionFactory() {
////		return new LettuceConnectionFactory();
////	}
//	@Bean
//	public LettuceConnectionFactory lettuceConnectionFactory() {
//	    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//	    config.setHostName(redisHost);
//	    config.setPort(redisPort);
//	    config.setUsername(redisUsername); // Redis Cloud requires a username
//	    config.setPassword(redisPassword);
//
//	    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//	        .useSsl() // Enable SSL for Redis Cloud
//	        .build();
//
//	    return new LettuceConnectionFactory(config, clientConfig);
//	}
//	
//	@Bean
//	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
//	    StringRedisTemplate template = new StringRedisTemplate();
//	    template.setConnectionFactory(redisConnectionFactory);
//	    return template;
//	}
//
//
//	@Bean
//	public RedisTemplate<String, Object> redisTemplate() {
//		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//		redisTemplate.setConnectionFactory(lettuceConnectionFactory());
////		 Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
////	        ObjectMapper objectMapper = new ObjectMapper();
////	        objectMapper.registerModule(new JavaTimeModule()); // Support Java 8 time
////	        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 format for dates
////	        serializer.setObjectMapper(objectMapper);
////
////	        redisTemplate.setDefaultSerializer(serializer);
////	        redisTemplate.setKeySerializer(new StringRedisSerializer());
////	        redisTemplate.setValueSerializer(serializer);
//		Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        serializer.setObjectMapper(objectMapper);
//
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(serializer);
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(serializer); 
//
//        return redisTemplate;
//	}
//	
//	@Bean
//	public RedisTemplate<String, PollingAgeWiseRedis> pollingAgeWiseRedisredis() {
//		RedisTemplate<String, PollingAgeWiseRedis> redisTemplate = new RedisTemplate<>();
//		redisTemplate.setConnectionFactory(lettuceConnectionFactory());
////		 Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
////	        ObjectMapper objectMapper = new ObjectMapper();
////	        objectMapper.registerModule(new JavaTimeModule()); // Support Java 8 time
////	        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601 format for dates
////	        serializer.setObjectMapper(objectMapper);
////
////	        redisTemplate.setDefaultSerializer(serializer);
////	        redisTemplate.setKeySerializer(new StringRedisSerializer());
////	        redisTemplate.setValueSerializer(serializer);
//		Jackson2JsonRedisSerializer<PollingAgeWiseRedis> serializer = new Jackson2JsonRedisSerializer<>(PollingAgeWiseRedis.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        serializer.setObjectMapper(objectMapper);
//
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(serializer);
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(serializer); 
//
//        return redisTemplate;
//	}
//	
//	
//	@Bean
//	public RedisTemplate<String, BoothWiseTimingVotersCountRedis> boothWiseTimingVotersCountRedisTemplate() {
//		RedisTemplate<String, BoothWiseTimingVotersCountRedis> redisTemplate = new RedisTemplate<>();
//		redisTemplate.setConnectionFactory(lettuceConnectionFactory());
//		Jackson2JsonRedisSerializer<BoothWiseTimingVotersCountRedis> serializer = new Jackson2JsonRedisSerializer<>(BoothWiseTimingVotersCountRedis.class);
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//        serializer.setObjectMapper(objectMapper);
//
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(serializer);
//        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
//        redisTemplate.setHashValueSerializer(serializer); 
//
//        return redisTemplate;
//	}
//
////	@Bean
////	public Jackson2JsonRedisSerializer<PollingAgeWiseRedis> redisSerializer() {
////	    Jackson2JsonRedisSerializer<PollingAgeWiseRedis> serializer = new Jackson2JsonRedisSerializer<>(PollingAgeWiseRedis.class);
////	    ObjectMapper objectMapper = new ObjectMapper();
////	    
////	    // Register the JavaTimeModule to handle LocalDateTime serialization
////	    objectMapper.registerModule(new JavaTimeModule());
////	    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // Use ISO-8601 format for dates
////	    
////	    serializer.setObjectMapper(objectMapper);
////	    return serializer;
////	}
////	
//	
//	@Bean
//	public RedisConnectionFactory redisConnectionFactory() {
//	    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
//	    config.setHostName(redisHost);
//	    config.setPort(redisPort);
//	    config.setUsername(redisUsername);
//	    config.setPassword(redisPassword);
//	    
//	    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//	        .useSsl()
//	        .build();
//	        
//	    LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
//	    factory.setValidateConnection(true); // Enable connection validation
//	    return factory;
//	}
	
}
