package com.thedal.thedal_app.sqs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;


import software.amazon.awssdk.services.sqs.SqsClient;

@Service
public class SQSTest {

    private final AmazonSQS sqsClient;

    @Value("${aws.sqs.queue.url}") // You should add this URL in your application.properties or application.yml
    private String queueUrl;

    public SQSTest(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    public void sendMessage(String messageBody) {
        try {
            System.out.println("Sending message to SQS queue: " + queueUrl);
            
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(messageBody)
                    .withDelaySeconds(0); // Optional: You can set a delay if required

            SendMessageResult sendMessageResult = sqsClient.sendMessage(sendMessageRequest);

            System.out.println("Message sent successfully to SQS.");
            System.out.println("Message ID: " + sendMessageResult.getMessageId());
            
        } catch (Exception e) {
            System.err.println("Failed to send message to SQS. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
