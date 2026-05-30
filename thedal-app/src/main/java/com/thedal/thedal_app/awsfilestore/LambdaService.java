// package com.thedal.thedal_app.awsfilestore;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;

// import com.amazonaws.services.lambda.AWSLambda;
// import com.amazonaws.services.lambda.model.InvokeRequest;

// // @Service
// public class LambdaService {
//       private final AWSLambda awsLambda;

//     @Value("${cloud.aws.lambda.function-name}")
//     private String functionName;

//     public LambdaService(AWSLambda awsLambda) {
//         this.awsLambda = awsLambda;
//     }

//     // Call AWS Lambda function
//     public void invokeLambda(String payload) {
//         InvokeRequest request = new InvokeRequest()
//                 .withFunctionName(functionName)
//                 .withPayload(payload);
//         awsLambda.invoke(request);
//     }


