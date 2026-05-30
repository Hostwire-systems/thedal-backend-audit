package com.thedal.thedal_app.sqs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import java.util.List;

public class SqsLambdaHandler implements RequestHandler<List<Message>, String>  {

    private final AmazonSQS sqsClient = AmazonSQSClient.builder().build();
    private String queueUrl = "your-sqs-queue-url"; // Replace with your SQS queue URL

    @Override
    public String handleRequest(List<Message> messages, Context context) {
        for (Message message : messages) {
            System.out.println("Processing message: " + message.getBody());
            // Your processing logic here

            // Delete the message from the queue after processing
            sqsClient.deleteMessage(queueUrl, message.getReceiptHandle());
            System.out.println("Message deleted from the queue");
        }
        return "Success";
    }

}
