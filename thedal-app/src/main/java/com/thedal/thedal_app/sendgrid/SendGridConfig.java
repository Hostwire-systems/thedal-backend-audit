package com.thedal.thedal_app.sendgrid;

import org.springframework.context.annotation.Bean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sendgrid.SendGrid;

import com.sendgrid.SendGrid;

@Configuration
public class SendGridConfig {

    private static final Logger LOGGER= LogManager.getLogger(SendGridConfig.class);
	@Value("${sendgrid.key}")
	private String key;

	   @Bean
	    public SendGrid getSendGrid() {
		   LOGGER.info("inside sendGrid config: key:{}",key);
	    	return new SendGrid(key);
	    }
}



