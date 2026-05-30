package com.thedal.thedal_app.sqs;


import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedal.thedal_app.voter.dto.VoterDTO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SqsConsumerService {

    private final AmazonSQS sqsClient;
    private final ObjectMapper objectMapper;
    private static final Logger LOGGER = LogManager.getLogger(SqsConsumerService.class);

    @Value("${aws.sqs.queue.url}")
    private String queueUrl;


    public SqsConsumerService(AmazonSQS sqsClient, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
    }

    public void receiveMessages() {
        try {
            System.out.println("Polling messages from SQS queue: " + queueUrl);
            
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl)
                    .withMaxNumberOfMessages(5) // Fetch up to 5 messages at a time
                    .withWaitTimeSeconds(10); // Long polling for 10 seconds

            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).getMessages();
            
            for (Message message : messages) {
                System.out.println("Message received: " + message.getBody());
                
                // Process the message as needed
                System.out.println("Message ID: " + message.getMessageId());
                
                // Delete the message from SQS after processing
                sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
                System.out.println("Message deleted from queue");
            }

        } catch (Exception e) {
            System.err.println("Failed to receive messages from SQS. Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

        public void sendVoterToSQS(VoterDTO voterDto) {
                    try {
                        String message = objectMapper.writeValueAsString(voterDto);
                        SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, message);
                        sqsClient.sendMessage(sendMessageRequest);
                        LOGGER.info("Successfully sent message to SQS for voter ID: {}", voterDto.getVoterId());
                    } catch (Exception e) {
                        LOGGER.error("Failed to send message to SQS for voter ID: {}. Error: {}", 
                                     voterDto.getVoterId(), e.getMessage());
                       // throw new SqsMessageException("Failed to send message to SQS for voter ID: " 
                                                   //   + voterDto.getVoterId(), e);
                    }
                }
    }



