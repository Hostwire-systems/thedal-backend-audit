// package com.thedal.thedal_app.sqs;

// import org.apache.logging.log4j.LogManager;
// import org.apache.logging.log4j.Logger;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.amazonaws.services.sqs.model.SendMessageRequest;
// import com.amazonaws.services.sqs.AmazonSQS;
// import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.thedal.thedal_app.notification.NotificationServiceImpl;
// import com.thedal.thedal_app.voter.dto.VoterDTO;

// @Service
// public class SqsService {
//     private static final Logger LOGGER = LogManager.getLogger(SqsService.class);
    
//     private final AmazonSQS sqs;
//     private final ObjectMapper objectMapper;
//     private final String queueUrl;

//     public SqsService(AmazonSQS sqs, ObjectMapper objectMapper, 
//                       @Value("${aws.sqs.queue.url}") String queueUrl) {
//         this.sqs = sqs;
//         this.objectMapper = objectMapper;
//         this.queueUrl = queueUrl;
//     }

//     public void sendVoterToSQS(VoterDTO1 voterDto) {
//         try {
//             String message = objectMapper.writeValueAsString(voterDto);
//             SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, message);
//             sqs.sendMessage(sendMessageRequest);
//             LOGGER.info("Successfully sent message to SQS for voter ID: {}", voterDto.getVoterId());
//         } catch (Exception e) {
//             LOGGER.error("Failed to send message to SQS for voter ID: {}. Error: {}", 
//                          voterDto.getVoterId(), e.getMessage());
//            // throw new SqsMessageException("Failed to send message to SQS for voter ID: " 
//                                        //   + voterDto.getVoterId(), e);
//         }
//     }
// }

