package com.thedal.thedal_app.sqs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class SQSReceiver {
      @Autowired
    private SqsConsumerService sqsService;

    @PostConstruct
    public void init() {
        sqsService.receiveMessages(); // Test the SQS message processing
    }
}


