// package com.thedal.thedal_app.awsfilestore;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;

// import com.amazonaws.services.sqs.model.CreateQueueRequest;
// import com.amazonaws.services.sqs.model.DeleteMessageRequest;
// import com.amazonaws.services.sqs.model.SendMessageRequest;
// import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
// import java.util.List;
// import com.amazonaws.services.sqs.model.Message;


// import com.amazonaws.auth.AWSCredentials;
// import com.amazonaws.auth.AWSStaticCredentialsProvider;
// import com.amazonaws.auth.BasicAWSCredentials;
// import com.amazonaws.regions.Regions;
// import com.amazonaws.services.sqs.AmazonSQS;
// import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

// @Component
// public class AwsSqs {

// @Value( "${aws.access.key}" )
// private String AWS_ACCESS_KEY;
// @Value("${aws.access.key}")
// private String AWS_SECRET_KEY;
// // @Value("${aws.sqs.queue}")
// // private String AWS_SQS_QUEUE;
// // @Value("${aws.sqs.arn}")
// // private String AWS_SQS_QUEUE_ARN;
// @Value("${aws.sqs.queue.url}")
// private String AWS_SQS_QUEUE_URL;
// @Value("${aws.s3.region}") String s3Region;


// private AWSCredentials awsCredentials() {
// AWSCredentials credentials = new BasicAWSCredentials (AWS_ACCESS_KEY, AWS_SECRET_KEY);
// return credentials;
// }

// private AmazonSQS sqsClientBuilder() {
//     return AmazonSQSClientBuilder
//             .standard()
//             .withCredentials(new AWSStaticCredentialsProvider(awsCredentials()))
//             .withRegion(s3Region)
//             .build();
// }
// public void createSQSQueue(String queueName) {
// AmazonSQS sqsClient = sqsClientBuilder();
// CreateQueueRequest createStandardQueueRequest = new CreateQueueRequest(queueName);
// String standardQueueUrl = sqsClient.createQueue (createStandardQueueRequest).getQueueUrl();
// System.out.println("AWS SQS Queue URL: " + standardQueueUrl);
// }
// public String produceMessageToSQS(String message) {
//     AmazonSQS sqsClient = sqsClientBuilder(); // Initialize the SQS client
//     SendMessageRequest request = new SendMessageRequest() // Create a request object
//             .withQueueUrl(AWS_SQS_QUEUE_URL) // Set the SQS queue URL
//             .withMessageBody(message) // Set the message body
//             .withDelaySeconds(10); // Set the delay in seconds
//     return sqsClient.sendMessage(request).getMessageId(); // Send the message and return its ID
// }
// public List<Message> consumeMessageFromSQS() {
//     AmazonSQS sqsClient = sqsClientBuilder(); // Initialize the SQS client
//     ReceiveMessageRequest request = new ReceiveMessageRequest(AWS_SQS_QUEUE_URL) // Create a request object
//             .withWaitTimeSeconds(10) // Set the wait time in seconds
//             .withMaxNumberOfMessages(10); // Set the maximum number of messages to fetch
//     List<Message> sqsMessages = sqsClient.receiveMessage(request).getMessages(); // Fetch messages from the queue
//     for (Message message : sqsMessages) {
//         // Process each message
//         System.out.println(message.getBody());
//     }
//     return sqsMessages; // Return the list of messages
// }
// // public void dequeuMessageFromSQS (Message message) {
// // AmazonSQS sqsClient = sqsClientBuilder();
// // sqsClient.deleteMessage(new DeleteMessageRequest()
// // .withQueueUrl(AWS_SQS_QUEUE_URL)
// // .withReceiptHandle (message.getReceipt Handle()));
// // }
// }