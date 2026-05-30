package com.thedal.thedal_app.sqs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

@Configuration
public class SqsConfig {
    @Value("${aws.s3.region}")
    private String awsRegion;
    
    @Value("${aws.access.key}")
    private String accessKey;
    
    @Value("${aws.secret.key}")
    private String secretKey;
    
    @Bean
    public AmazonSQS sqsClient() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        
        return AmazonSQSClientBuilder.standard()
                .withRegion(awsRegion)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

}
