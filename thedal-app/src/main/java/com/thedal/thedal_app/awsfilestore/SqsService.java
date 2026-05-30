// package com.thedal.thedal_app.awsfilestore;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.amazonaws.services.sqs.AmazonSQSAsync;
// import com.amazonaws.services.sqs.model.SendMessageRequest;

// @Service
// public class SqsService {
//      private final AmazonSQSAsync amazonSQSAsync;

//     @Value("${cloud.aws.sqs.queue.url}")
//     private String queueUrl;

//     public SqsService(AmazonSQSAsync amazonSQSAsync) {
//         this.amazonSQSAsync = amazonSQSAsync;
//     }

//     // Send JSON payload to SQS
//     public void sendMessage(String messageBody) {
//         SendMessageRequest sendMessageRequest = new SendMessageRequest()
//                 .withQueueUrl(queueUrl)
//                 .withMessageBody(messageBody);
//         amazonSQSAsync.sendMessage(sendMessageRequest);
//     }
// }


